import { useMemo, useState } from 'react';
import { BadgeCheck, Eye, Pencil, Plus, XCircle } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { api } from '../lib/api';
import { can } from '../lib/access';
import { fmtDate, fmtTime, money } from '../lib/format';
import { numOrNull, trimmed } from '../lib/forms';
import { useFetch } from '../lib/useFetch';
import { DataTable, RowAction } from '../components/DataTable';
import { FormModal, LineItems } from '../components/Form';
import { ConfirmDialog, Modal } from '../components/Modal';
import { Button, Details, PageHeader, Pill } from '../components/ui';

const CHARGES = [
  { name: 'consultationCharge', label: 'Consultation (₹)' },
  { name: 'labCharge', label: 'Lab tests (₹)' },
  { name: 'medicineCharge', label: 'Medicines (₹)' },
  { name: 'procedureCharge', label: 'Procedures (₹)' },
  { name: 'otherCharge', label: 'Other (₹)' },
].map((c) => ({ ...c, type: 'number', min: 0, step: '0.01', placeholder: '0.00' }));

const ITEM_COLUMNS = [
  { name: 'description', label: 'Description', placeholder: 'X-ray chest', width: '2fr' },
  { name: 'amount', label: 'Amount (₹)', type: 'number', step: '0.01', min: '0', placeholder: '0.00' },
];
const round2 = (n) => Math.round(n * 100) / 100;
const blankItem = (i) => !String(i.description ?? '').trim() && String(i.amount ?? '').trim() === '';

// A row is either completely empty (ignored) or needs both description and amount.
function validateItems(values) {
  const problems = {};
  values.items.forEach((i, idx) => {
    if (blankItem(i)) return;
    if (!String(i.description ?? '').trim()) problems[`items[${idx}].description`] = 'Required';
    if (!(Number(i.amount) > 0)) problems[`items[${idx}].amount`] = 'Must be above 0';
  });
  return problems;
}

function BillForm({ bill, patients, onClose, onSaved }) {
  const editing = Boolean(bill);
  const toast = useToast();
  const [patientId, setPatientId] = useState('');
  // Appointments of the chosen patient (so a bill can be linked to one).
  const appointments = useFetch(!editing && patientId ? `/api/appointments?patientId=${patientId}` : null);

  const fields = [
    ...(editing ? [] : [
      {
        name: 'patientId', label: 'Patient', type: 'select', required: true, span: 2,
        options: (patients || []).filter((p) => p.active).map((p) => ({ value: String(p.id), label: `${p.fullName} (#${p.id})` })),
      },
      {
        name: 'appointmentId', label: 'Appointment (optional)', type: 'select', span: 2, disabled: !patientId,
        placeholder: patientId ? 'Not linked to an appointment' : 'Choose a patient first',
        options: (appointments.data || []).filter((a) => a.status !== 'CANCELLED')
          .map((a) => ({ value: String(a.id), label: `#${a.id} · ${fmtDate(a.appointmentDate)} ${fmtTime(a.appointmentTime)} · ${a.doctor?.name}` })),
      },
    ]),
    ...CHARGES,
  ];

  const initial = editing
    ? {
      ...Object.fromEntries(CHARGES.map((c) => [c.name, bill[c.name] ? String(bill[c.name]) : ''])),
      items: bill.items.map((i) => ({ description: i.description, amount: String(i.amount) })),
    }
    : { items: [] };

  const payload = (v) => ({
    ...Object.fromEntries(CHARGES.map((c) => [c.name, numOrNull(v[c.name])])),
    items: v.items.filter((i) => !blankItem(i)).map((i) => ({ description: trimmed(i.description), amount: Number(i.amount) })),
  });

  return (
    <FormModal
      title={editing ? `Edit bill #${bill.id}` : 'New bill'} size="lg" fields={fields} initial={initial}
      subtitle={editing ? `${bill.patient?.fullName} · only pending bills can be edited` : 'Enter the charges; the total is calculated for you.'}
      submitLabel={editing ? 'Save changes' : 'Create bill'} onClose={onClose} validate={validateItems}
      onFieldChange={(name, value) => {
        if (name !== 'patientId') return null;
        setPatientId(value);
        return { appointmentId: '' };   // the old choice belongs to the previous patient
      }}
      onSubmit={async (v) => {
        if (editing) await api.put(`/api/bills/${bill.id}`, payload(v));
        else await api.post('/api/bills', { patientId: Number(v.patientId), appointmentId: numOrNull(v.appointmentId), ...payload(v) });
        toast.success(editing ? 'Bill updated.' : 'Bill created.');
        onSaved();
      }}
    >
      {({ values, setValue, fieldErrors }) => {
        const total = round2(
          CHARGES.reduce((s, c) => s + (Number(values[c.name]) || 0), 0)
          + values.items.reduce((s, i) => s + (Number(i.amount) || 0), 0),
        );
        return (
          <>
            <LineItems
              label="Extra items" addLabel="Add item" min={0} items={values.items} errors={fieldErrors}
              columns={ITEM_COLUMNS} onChange={(items) => setValue('items', items)}
            />
            <div className="total-bar"><span>Total</span><strong>{money(total)}</strong></div>
          </>
        );
      }}
    </FormModal>
  );
}

export default function BillingPage() {
  const { session } = useAuth();
  const toast = useToast();
  const canWrite = can(session.role, 'writeBills');
  const { data, loading, error, reload } = useFetch('/api/bills');
  const patients = useFetch(canWrite ? '/api/patients' : null);
  const [dialog, setDialog] = useState(null);
  const [status, setStatus] = useState('all');
  const close = () => setDialog(null);

  const rows = useMemo(() => {
    const list = [...(data || [])].sort((a, b) => b.createdDate.localeCompare(a.createdDate) || b.id - a.id);
    return status === 'all' ? list : list.filter((b) => b.paymentStatus === status);
  }, [data, status]);

  const pendingTotal = (data || []).filter((b) => b.paymentStatus === 'PENDING').reduce((s, b) => s + Number(b.totalAmount), 0);
  const setStatusOf = (bill, next) => async () => {
    await api.patch(`/api/bills/${bill.id}/status`, { status: next });
    toast.success(next === 'PAID' ? `Bill #${bill.id} marked as paid.` : `Bill #${bill.id} cancelled.`);
    reload();
  };

  return (
    <>
      <PageHeader
        title="Billing"
        subtitle={session.role === 'PATIENT'
          ? `Your bills. Outstanding balance: ${money(pendingTotal)}`
          : `Invoices and payments. Outstanding across all pending bills: ${money(pendingTotal)}`}
        actions={canWrite && <Button icon={Plus} onClick={() => setDialog({ type: 'create' })}>New bill</Button>}
      />
      <DataTable
        rows={rows} loading={loading} error={error} onRetry={reload} searchPlaceholder="Search patient, bill number…"
        emptyTitle="No bills" emptyText={canWrite ? 'Create the first bill.' : 'You have no bills.'}
        toolbar={(
          <select aria-label="Filter by status" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="all">All statuses</option>
            <option value="PENDING">Pending</option>
            <option value="PAID">Paid</option>
            <option value="CANCELLED">Cancelled</option>
          </select>
        )}
        columns={[
          { header: 'Bill', cell: (b) => <div className="cell-main"><b>#{b.id}</b><small>{fmtDate(b.createdDate)}</small></div> },
          { header: 'Patient', cell: (b) => b.patient?.fullName || '—' },
          { header: 'Appointment', cell: (b) => (b.appointmentId ? `#${b.appointmentId}` : '—') },
          { header: 'Total', className: 'num', cell: (b) => <b>{money(b.totalAmount)}</b> },
          { header: 'Status', cell: (b) => <Pill value={b.paymentStatus} /> },
        ]}
        actions={(b) => (
          <>
            <RowAction icon={Eye} label="View" onClick={() => setDialog({ type: 'view', row: b })} />
            {canWrite && b.paymentStatus === 'PENDING' && (
              <>
                <RowAction icon={Pencil} label="Edit" onClick={() => setDialog({ type: 'edit', row: b })} />
                <RowAction icon={BadgeCheck} label="Mark paid" onClick={() => setDialog({ type: 'pay', row: b })} />
                <RowAction icon={XCircle} label="Cancel" danger onClick={() => setDialog({ type: 'cancel', row: b })} />
              </>
            )}
          </>
        )}
      />

      {dialog?.type === 'create' && <BillForm patients={patients.data} onClose={close} onSaved={reload} />}
      {dialog?.type === 'edit' && <BillForm bill={dialog.row} onClose={close} onSaved={reload} />}
      {dialog?.type === 'pay' && (
        <ConfirmDialog
          title="Mark this bill as paid?" confirmLabel="Mark as paid" onClose={close}
          message={`Bill #${dialog.row.id} for ${dialog.row.patient?.fullName} — ${money(dialog.row.totalAmount)}. A paid bill can no longer be edited or changed.`}
          onConfirm={setStatusOf(dialog.row, 'PAID')}
        />
      )}
      {dialog?.type === 'cancel' && (
        <ConfirmDialog
          danger title="Cancel this bill?" confirmLabel="Cancel bill" onClose={close}
          message={`Bill #${dialog.row.id} for ${dialog.row.patient?.fullName} — ${money(dialog.row.totalAmount)}. A cancelled bill cannot be reopened.`}
          onConfirm={setStatusOf(dialog.row, 'CANCELLED')}
        />
      )}
      {dialog?.type === 'view' && (
        <Modal title={`Bill #${dialog.row.id}`} subtitle={`Created ${fmtDate(dialog.row.createdDate)}`} onClose={close}
          footer={<Button variant="secondary" onClick={close}>Close</Button>}>
          <Details rows={[['Patient', dialog.row.patient?.fullName], ['Appointment', dialog.row.appointmentId ? `#${dialog.row.appointmentId}` : null]]} />
          <div className="table-scroll inner-table">
            <table>
              <tbody>
                {CHARGES.filter((c) => Number(dialog.row[c.name]) > 0).map((c) => (
                  <tr key={c.name}><td>{c.label.replace(' (₹)', '')}</td><td className="num">{money(dialog.row[c.name])}</td></tr>
                ))}
                {dialog.row.items.map((i) => <tr key={i.id}><td>{i.description}</td><td className="num">{money(i.amount)}</td></tr>)}
                <tr className="total-row"><td>Total</td><td className="num">{money(dialog.row.totalAmount)}</td></tr>
              </tbody>
            </table>
          </div>
          <p className="status-line">Status: <Pill value={dialog.row.paymentStatus} /></p>
        </Modal>
      )}
    </>
  );
}
