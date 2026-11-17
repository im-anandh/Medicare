import { useMemo, useState } from 'react';
import { CalendarClock, KeyRound, Pencil, Plus, Upload, UserX } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { api } from '../lib/api';
import { can } from '../lib/access';
import { strOrNull, trimmed } from '../lib/forms';
import { useFetch } from '../lib/useFetch';
import BulkImportModal from '../components/BulkImportModal';
import CreateAccountModal from '../components/CreateAccountModal';
import { DataTable, RowAction } from '../components/DataTable';
import { FormModal } from '../components/Form';
import { ConfirmDialog } from '../components/Modal';
import { Button, PageHeader, Pill } from '../components/ui';

const BULK_COLUMNS = ['name', 'specialization', 'departmentId', 'phone', 'email', 'experience', 'availability'];
const BULK_SAMPLE = `name,specialization,departmentId,phone,email,experience,availability
Dr. Meera Nair,Cardiologist,1,9876500101,meera.nair@medicare.com,9,Mon-Fri 9am-4pm
Dr. Karthik Iyer,Neurologist,2,9876500102,karthik.iyer@medicare.com,14,Tue-Sat 10am-6pm`;

export default function DoctorsPage() {
  const { session } = useAuth();
  const toast = useToast();
  const isAdmin = can(session.role, 'writeDoctors');
  const ownId = session.role === 'DOCTOR' ? session.profile?.id : null;
  const { data, loading, error, reload } = useFetch('/api/doctors');
  const departments = useFetch(isAdmin ? '/api/departments' : null);
  const [dialog, setDialog] = useState(null);
  const [status, setStatus] = useState('all');
  const close = () => setDialog(null);

  const rows = useMemo(() => {
    const list = [...(data || [])].sort((a, b) => a.id - b.id);
    return status === 'all' ? list : list.filter((d) => d.active === (status === 'active'));
  }, [data, status]);

  const fields = [
    { name: 'name', label: 'Full name', required: true, maxLength: 100, placeholder: 'Dr. A. Kumar', span: 2 },
    { name: 'specialization', label: 'Specialization', required: true, placeholder: 'Cardiologist' },
    {
      name: 'departmentId', label: 'Department', type: 'select', required: true,
      options: (departments.data || []).filter((d) => d.active).map((d) => ({ value: String(d.id), label: d.name })),
    },
    { name: 'phone', label: 'Phone', type: 'tel', required: true },
    { name: 'email', label: 'Email', type: 'email', required: true },
    { name: 'experience', label: 'Experience (years)', type: 'number', min: 0, max: 70, required: true },
    { name: 'availability', label: 'Availability', placeholder: 'Mon–Fri, 9am–5pm' },
  ];
  const toPayload = (v) => ({
    name: trimmed(v.name), specialization: trimmed(v.specialization), departmentId: Number(v.departmentId),
    phone: trimmed(v.phone), email: trimmed(v.email), experience: Number(v.experience), availability: strOrNull(v.availability),
  });

  return (
    <>
      <PageHeader
        title="Doctors" subtitle="The clinical team and when they are available."
        actions={isAdmin && (
          <>
            <Button variant="secondary" icon={Upload} onClick={() => setDialog({ type: 'bulk' })}>Bulk import</Button>
            <Button icon={Plus} onClick={() => setDialog({ type: 'create' })}>Add doctor</Button>
          </>
        )}
      />

      <DataTable
        rows={rows} loading={loading} error={error} onRetry={reload} searchPlaceholder="Search by name, specialization…"
        emptyTitle="No doctors yet" emptyText={isAdmin ? 'Add the first doctor (create departments first if you have none).' : undefined}
        toolbar={(
          <select aria-label="Filter by status" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="all">All statuses</option>
            <option value="active">Active</option>
            <option value="inactive">Inactive</option>
          </select>
        )}
        columns={[
          { header: 'Doctor', cell: (d) => <div className="cell-main"><b>{d.name}</b><small>#{d.id} · {d.specialization}</small></div> },
          { header: 'Department', cell: (d) => d.department?.name || '—' },
          { header: 'Experience', cell: (d) => `${d.experience} ${d.experience === 1 ? 'yr' : 'yrs'}` },
          { header: 'Availability', cell: (d) => d.availability || '—' },
          { header: 'Contact', cell: (d) => <div className="cell-main"><span>{d.phone}</span><small>{d.email}</small></div> },
          { header: 'Status', cell: (d) => <Pill value={d.active ? 'ACTIVE' : 'INACTIVE'} /> },
        ]}
        actions={(d) => (
          <>
            {(isAdmin || ownId === d.id) && <RowAction icon={CalendarClock} label="Availability" onClick={() => setDialog({ type: 'availability', row: d })} />}
            {isAdmin && <RowAction icon={Pencil} label="Edit" onClick={() => setDialog({ type: 'edit', row: d })} />}
            {isAdmin && d.active && <RowAction icon={KeyRound} label="Create login" onClick={() => setDialog({ type: 'login', row: d })} />}
            {isAdmin && d.active && <RowAction icon={UserX} label="Deactivate" danger onClick={() => setDialog({ type: 'deactivate', row: d })} />}
          </>
        )}
      />

      {dialog?.type === 'create' && (
        <FormModal
          title="Add doctor" fields={fields} submitLabel="Add doctor" onClose={close}
          onSubmit={async (v) => { await api.post('/api/doctors', toPayload(v)); toast.success('Doctor added.'); reload(); }}
        />
      )}
      {dialog?.type === 'edit' && (
        <FormModal
          title={`Edit ${dialog.row.name}`} fields={fields} onClose={close}
          initial={{ ...dialog.row, departmentId: String(dialog.row.department?.id ?? ''), availability: dialog.row.availability ?? '' }}
          onSubmit={async (v) => { await api.put(`/api/doctors/${dialog.row.id}`, toPayload(v)); toast.success('Doctor updated.'); reload(); }}
        />
      )}
      {dialog?.type === 'availability' && (
        <FormModal
          title={`Availability — ${dialog.row.name}`} size="sm" submitLabel="Save availability" onClose={close}
          fields={[{ name: 'availability', label: 'When are they available?', type: 'textarea', maxLength: 255, placeholder: 'Mon–Fri, 9am–5pm', required: true }]}
          initial={{ availability: dialog.row.availability ?? '' }}
          onSubmit={async (v) => { await api.patch(`/api/doctors/${dialog.row.id}/availability`, { availability: trimmed(v.availability) }); toast.success('Availability updated.'); reload(); }}
        />
      )}
      {dialog?.type === 'deactivate' && (
        <ConfirmDialog
          danger title="Deactivate doctor?" confirmLabel="Deactivate" onClose={close}
          message={`${dialog.row.name} will be marked inactive and can no longer be booked. Existing records are kept.`}
          onConfirm={async () => { await api.del(`/api/doctors/${dialog.row.id}`); toast.success('Doctor deactivated.'); reload(); }}
        />
      )}
      {dialog?.type === 'login' && <CreateAccountModal fixedRole="DOCTOR" fixedProfileId={dialog.row.id} onClose={close} />}
      {dialog?.type === 'bulk' && (
        <BulkImportModal
          title="Import doctors" noun="doctors" endpoint="/api/doctors/bulk" numeric={['departmentId', 'experience']}
          columns={BULK_COLUMNS} sample={BULK_SAMPLE} onClose={close}
          onDone={(n) => { toast.success(`${n} ${n === 1 ? 'doctor' : 'doctors'} imported.`); reload(); }}
        />
      )}
    </>
  );
}
