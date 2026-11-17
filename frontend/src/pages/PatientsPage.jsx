import { useMemo, useState } from 'react';
import { Eye, KeyRound, Pencil, Plus, Upload, UserX, Users } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { api } from '../lib/api';
import { can } from '../lib/access';
import { ageFrom, enumLabel, fmtDate, todayStr } from '../lib/format';
import { strOrNull, trimmed } from '../lib/forms';
import { useFetch } from '../lib/useFetch';
import BulkImportModal from '../components/BulkImportModal';
import CreateAccountModal from '../components/CreateAccountModal';
import { DataTable, RowAction } from '../components/DataTable';
import { FormModal } from '../components/Form';
import { ConfirmDialog, Modal } from '../components/Modal';
import { Button, Details, PageHeader, Pill } from '../components/ui';

const GENDERS = [{ value: 'MALE', label: 'Male' }, { value: 'FEMALE', label: 'Female' }, { value: 'OTHER', label: 'Other' }];
const BLOOD = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map((v) => ({ value: v, label: v }));

const FIELDS = [
  { name: 'fullName', label: 'Full name', required: true, maxLength: 100, span: 2 },
  { name: 'dateOfBirth', label: 'Date of birth', type: 'date', required: true, max: todayStr() },
  { name: 'gender', label: 'Gender', type: 'select', required: true, options: GENDERS },
  { name: 'phone', label: 'Phone', type: 'tel', required: true, placeholder: '9876543210' },
  { name: 'bloodGroup', label: 'Blood group', type: 'select', options: BLOOD },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'emergencyContact', label: 'Emergency contact', type: 'tel' },
  { name: 'address', label: 'Address', type: 'textarea' },
];

const toPayload = (v) => ({
  fullName: trimmed(v.fullName), dateOfBirth: v.dateOfBirth, gender: v.gender, phone: trimmed(v.phone),
  bloodGroup: strOrNull(v.bloodGroup), email: strOrNull(v.email),
  emergencyContact: strOrNull(v.emergencyContact), address: strOrNull(v.address),
});

const BULK_COLUMNS = ['fullName', 'dateOfBirth', 'gender', 'phone', 'bloodGroup', 'email', 'address', 'emergencyContact'];
const BULK_SAMPLE = `fullName,dateOfBirth,gender,phone,bloodGroup,email,address,emergencyContact
Asha Raj,1995-03-14,FEMALE,9876500001,B+,asha@example.com,"12 Anna Nagar, Chennai",9876500011
Bala Kumar,1988-11-02,MALE,9876500002,O+,,Salem,`;

export default function PatientsPage() {
  const { session } = useAuth();
  const toast = useToast();
  const canWrite = can(session.role, 'writePatients');
  const canLogin = can(session.role, 'createLogins');
  const { data, loading, error, reload } = useFetch('/api/patients');
  const [dialog, setDialog] = useState(null);
  const [status, setStatus] = useState('all');
  const close = () => setDialog(null);

  const rows = useMemo(() => {
    const list = [...(data || [])].sort((a, b) => a.id - b.id);
    return status === 'all' ? list : list.filter((p) => p.active === (status === 'active'));
  }, [data, status]);

  const title = session.role === 'DOCTOR' ? 'My patients' : 'Patients';
  const subtitle = session.role === 'DOCTOR'
    ? 'Patients you have had an appointment with.'
    : 'Registered patients and their contact details.';

  return (
    <>
      <PageHeader
        title={title} subtitle={subtitle}
        actions={canWrite && (
          <>
            <Button variant="secondary" icon={Upload} onClick={() => setDialog({ type: 'bulk' })}>Bulk import</Button>
            <Button icon={Plus} onClick={() => setDialog({ type: 'create' })}>Add patient</Button>
          </>
        )}
      />

      <DataTable
        rows={rows} loading={loading} error={error} onRetry={reload} searchPlaceholder="Search by name, phone, email…"
        emptyTitle="No patients yet" emptyText={canWrite ? 'Add the first patient or import a list.' : 'Patients will appear after your first appointment with them.'}
        emptyAction={canWrite ? <Button icon={Plus} onClick={() => setDialog({ type: 'create' })}>Add patient</Button> : null}
        toolbar={(
          <select aria-label="Filter by status" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="all">All statuses</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
        )}
        columns={[
          { header: 'Patient', cell: (p) => <div className="cell-main"><b>{p.fullName}</b><small>#{p.id} · {ageFrom(p.dateOfBirth)} yrs · {enumLabel(p.gender)}</small></div> },
          { header: 'Contact', cell: (p) => <div className="cell-main"><span>{p.phone}</span><small>{p.email || '—'}</small></div> },
          { header: 'Blood', cell: (p) => p.bloodGroup || '—' },
          { header: 'Status', cell: (p) => <Pill value={p.active ? 'ACTIVE' : 'INACTIVE'} /> },
        ]}
        actions={(p) => (
          <>
            <RowAction icon={Eye} label="View" onClick={() => setDialog({ type: 'view', row: p })} />
            {canWrite && <RowAction icon={Pencil} label="Edit" onClick={() => setDialog({ type: 'edit', row: p })} />}
            {canLogin && p.active && <RowAction icon={KeyRound} label="Create login" onClick={() => setDialog({ type: 'login', row: p })} />}
            {canWrite && p.active && <RowAction icon={UserX} label="Deactivate" danger onClick={() => setDialog({ type: 'deactivate', row: p })} />}
          </>
        )}
      />

      {dialog?.type === 'create' && (
        <FormModal
          title="Add patient" fields={FIELDS} submitLabel="Add patient" onClose={close}
          onSubmit={async (v) => { await api.post('/api/patients', toPayload(v)); toast.success('Patient added.'); reload(); }}
        />
      )}
      {dialog?.type === 'edit' && (
        <FormModal
          title={`Edit ${dialog.row.fullName}`} fields={FIELDS} onClose={close}
          initial={Object.fromEntries(FIELDS.map((f) => [f.name, dialog.row[f.name] ?? '']))}
          onSubmit={async (v) => { await api.put(`/api/patients/${dialog.row.id}`, toPayload(v)); toast.success('Patient updated.'); reload(); }}
        />
      )}
      {dialog?.type === 'view' && (
        <Modal title={dialog.row.fullName} subtitle={`Patient #${dialog.row.id}`} onClose={close} footer={<Button variant="secondary" onClick={close}>Close</Button>}>
          <Details rows={[
            ['Date of birth', `${fmtDate(dialog.row.dateOfBirth)} (${ageFrom(dialog.row.dateOfBirth)} yrs)`], ['Gender', enumLabel(dialog.row.gender)],
            ['Phone', dialog.row.phone], ['Email', dialog.row.email], ['Blood group', dialog.row.bloodGroup],
            ['Emergency contact', dialog.row.emergencyContact], ['Address', dialog.row.address],
            ['Status', dialog.row.active ? 'Active' : 'Inactive'],
          ]} />
        </Modal>
      )}
      {dialog?.type === 'deactivate' && (
        <ConfirmDialog
          danger title="Deactivate patient?" confirmLabel="Deactivate" onClose={close}
          message={`${dialog.row.fullName} will be marked inactive. Their records and history are kept.`}
          onConfirm={async () => { await api.del(`/api/patients/${dialog.row.id}`); toast.success('Patient deactivated.'); reload(); }}
        />
      )}
      {dialog?.type === 'login' && (
        <CreateAccountModal fixedRole="PATIENT" fixedProfileId={dialog.row.id} onClose={close} />
      )}
      {dialog?.type === 'bulk' && (
        <BulkImportModal
          title="Import patients" noun="patients" endpoint="/api/patients/bulk"
          columns={BULK_COLUMNS} sample={BULK_SAMPLE} onClose={close}
          onDone={(n) => { toast.success(`${n} ${n === 1 ? 'patient' : 'patients'} imported.`); reload(); }}
        />
      )}
    </>
  );
}
