import { useState } from 'react';
import { Eye, Pencil, Pill as PillIcon } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { api } from '../lib/api';
import { can } from '../lib/access';
import { fmtDate } from '../lib/format';
import { strOrNull, trimmed } from '../lib/forms';
import { useFetch } from '../lib/useFetch';
import { DataTable, RowAction } from '../components/DataTable';
import { FormModal, LineItems } from '../components/Form';
import { Modal } from '../components/Modal';
import { Button, Details, PageHeader } from '../components/ui';

const BLANK = { medicine: '', dosage: '', frequency: '', duration: '', instructions: '' };
const COLUMNS = [
  { name: 'medicine', label: 'Medicine', placeholder: 'Paracetamol', width: '1.4fr' },
  { name: 'dosage', label: 'Dosage', placeholder: '500mg' },
  { name: 'frequency', label: 'Frequency', placeholder: '3 times a day' },
  { name: 'duration', label: 'Duration', placeholder: '5 days' },
  { name: 'instructions', label: 'Instructions', placeholder: 'After food', width: '1.3fr' },
];

// Each medicine needs these four filled in; errors are keyed like the server's ("items[0].dosage").
function validateItems(values) {
  const problems = {};
  values.items.forEach((item, i) => {
    ['medicine', 'dosage', 'frequency', 'duration'].forEach((k) => {
      if (!String(item[k] ?? '').trim()) problems[`items[${i}].${k}`] = 'Required';
    });
  });
  return problems;
}
const itemsPayload = (items) => items.map((i) => ({
  medicine: trimmed(i.medicine), dosage: trimmed(i.dosage), frequency: trimmed(i.frequency),
  duration: trimmed(i.duration), instructions: strOrNull(i.instructions),
}));

export default function PrescriptionsPage() {
  const { session } = useAuth();
  const toast = useToast();
  const canWrite = can(session.role, 'writePrescriptions');
  const { data, loading, error, reload } = useFetch('/api/prescriptions');
  const records = useFetch(canWrite ? '/api/medical-records' : null);
  const [dialog, setDialog] = useState(null);
  const close = () => setDialog(null);
  const rows = [...(data || [])].sort((a, b) => b.createdDate.localeCompare(a.createdDate) || b.id - a.id);

  const recordField = {
    name: 'medicalRecordId', label: 'Medical record (visit)', type: 'select', required: true, span: 2,
    hint: 'A prescription is always issued against one of your own visit records.',
    options: (records.data || []).map((r) => ({ value: String(r.id), label: `#${r.id} · ${r.patient?.fullName} · ${fmtDate(r.visitDate)} · ${r.diagnosis}` })),
  };
  const itemsEditor = ({ values, setValue, fieldErrors }) => (
    <LineItems label="Medicines" items={values.items} errors={fieldErrors} addLabel="Add medicine" columns={COLUMNS}
      onChange={(items) => setValue('items', items)} />
  );

  return (
    <>
      <PageHeader
        title="Prescriptions"
        subtitle={session.role === 'PATIENT' ? 'Medicines your doctors have prescribed to you.' : 'Prescriptions you have issued.'}
        actions={canWrite && <Button icon={PillIcon} onClick={() => setDialog({ type: 'create' })}>New prescription</Button>}
      />
      <DataTable
        rows={rows} loading={loading} error={error} onRetry={reload} searchPlaceholder="Search medicine, patient…"
        emptyTitle="No prescriptions" emptyText={canWrite ? 'Issue one from a medical record.' : 'No prescriptions have been issued to you yet.'}
        columns={[
          { header: 'Prescription', cell: (p) => <div className="cell-main"><b>#{p.id}</b><small>{fmtDate(p.createdDate)}</small></div> },
          { header: 'Patient', cell: (p) => p.patient?.fullName || '—' },
          { header: 'Doctor', cell: (p) => p.doctor?.name || '—' },
          { header: 'Medicines', cell: (p) => <span className="clamp" title={p.items.map((i) => i.medicine).join(', ')}>{p.items.map((i) => i.medicine).join(', ')}</span> },
          { header: 'Visit', cell: (p) => (p.medicalRecordId ? `Record #${p.medicalRecordId}` : '—') },
        ]}
        actions={(p) => (
          <>
            <RowAction icon={Eye} label="View" onClick={() => setDialog({ type: 'view', row: p })} />
            {canWrite && <RowAction icon={Pencil} label="Edit" onClick={() => setDialog({ type: 'edit', row: p })} />}
          </>
        )}
      />

      {dialog?.type === 'create' && (
        <FormModal
          title="New prescription" size="xl" fields={[recordField]} submitLabel="Issue prescription" onClose={close}
          initial={{ items: [{ ...BLANK }] }} validate={validateItems}
          onSubmit={async (v) => {
            await api.post('/api/prescriptions', { medicalRecordId: Number(v.medicalRecordId), items: itemsPayload(v.items) });
            toast.success('Prescription issued.');
            reload();
          }}
        >
          {itemsEditor}
        </FormModal>
      )}
      {dialog?.type === 'edit' && (
        <FormModal
          title={`Edit prescription #${dialog.row.id}`} size="xl" fields={[]} onClose={close}
          subtitle={`${dialog.row.patient?.fullName} · the medicine list below replaces the old one`}
          initial={{ items: dialog.row.items.map((i) => ({ ...BLANK, ...Object.fromEntries(Object.entries(i).map(([k, v]) => [k, v ?? ''])) })) }}
          validate={validateItems}
          onSubmit={async (v) => { await api.put(`/api/prescriptions/${dialog.row.id}`, { items: itemsPayload(v.items) }); toast.success('Prescription updated.'); reload(); }}
        >
          {itemsEditor}
        </FormModal>
      )}
      {dialog?.type === 'view' && (
        <Modal title={`Prescription #${dialog.row.id}`} subtitle={`Issued ${fmtDate(dialog.row.createdDate)}`} size="lg" onClose={close}
          footer={<Button variant="secondary" onClick={close}>Close</Button>}>
          <Details rows={[['Patient', dialog.row.patient?.fullName], ['Doctor', `${dialog.row.doctor?.name} — ${dialog.row.doctor?.specialization}`]]} />
          <div className="table-scroll inner-table">
            <table>
              <thead><tr><th>Medicine</th><th>Dosage</th><th>Frequency</th><th>Duration</th><th>Instructions</th></tr></thead>
              <tbody>
                {dialog.row.items.map((i) => (
                  <tr key={i.id}><td><b>{i.medicine}</b></td><td>{i.dosage}</td><td>{i.frequency}</td><td>{i.duration}</td><td>{i.instructions || '—'}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        </Modal>
      )}
    </>
  );
}
