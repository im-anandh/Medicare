import { useState } from 'react';
import { Building2, Pencil, Plus, UserX } from 'lucide-react';
import { useToast } from '../context/ToastContext';
import { api } from '../lib/api';
import { strOrNull, trimmed } from '../lib/forms';
import { useFetch } from '../lib/useFetch';
import { DataTable, RowAction } from '../components/DataTable';
import { FormModal } from '../components/Form';
import { ConfirmDialog } from '../components/Modal';
import { Button, PageHeader, Pill } from '../components/ui';

const FIELDS = [
  { name: 'name', label: 'Department name', required: true, maxLength: 100, placeholder: 'Cardiology' },
  { name: 'description', label: 'Description', type: 'textarea', maxLength: 255 },
];

export default function DepartmentsPage() {
  const toast = useToast();
  const { data, loading, error, reload } = useFetch('/api/departments');
  const [dialog, setDialog] = useState(null);
  const close = () => setDialog(null);
  const rows = [...(data || [])].sort((a, b) => a.id - b.id);
  const payload = (v) => ({ name: trimmed(v.name), description: strOrNull(v.description) });

  return (
    <>
      <PageHeader
        title="Departments" subtitle="Doctors are grouped into departments."
        actions={<Button icon={Plus} onClick={() => setDialog({ type: 'create' })}>Add department</Button>}
      />
      <DataTable
        rows={rows} loading={loading} error={error} onRetry={reload} searchPlaceholder="Search departments…"
        emptyTitle="No departments yet" emptyText="Create departments before adding doctors."
        emptyAction={<Button icon={Plus} onClick={() => setDialog({ type: 'create' })}>Add department</Button>}
        columns={[
          { header: 'Department', cell: (d) => <div className="cell-main"><b>{d.name}</b><small>#{d.id}</small></div> },
          { header: 'Description', cell: (d) => d.description || '—' },
          { header: 'Status', cell: (d) => <Pill value={d.active ? 'ACTIVE' : 'INACTIVE'} /> },
        ]}
        actions={(d) => (
          <>
            <RowAction icon={Pencil} label="Edit" onClick={() => setDialog({ type: 'edit', row: d })} />
            {d.active && <RowAction icon={UserX} label="Deactivate" danger onClick={() => setDialog({ type: 'deactivate', row: d })} />}
          </>
        )}
      />
      {dialog?.type === 'create' && (
        <FormModal title="Add department" fields={FIELDS} submitLabel="Add department" size="sm" onClose={close}
          onSubmit={async (v) => { await api.post('/api/departments', payload(v)); toast.success('Department added.'); reload(); }} />
      )}
      {dialog?.type === 'edit' && (
        <FormModal title={`Edit ${dialog.row.name}`} fields={FIELDS} size="sm" onClose={close}
          initial={{ name: dialog.row.name, description: dialog.row.description ?? '' }}
          onSubmit={async (v) => { await api.put(`/api/departments/${dialog.row.id}`, payload(v)); toast.success('Department updated.'); reload(); }} />
      )}
      {dialog?.type === 'deactivate' && (
        <ConfirmDialog danger title="Deactivate department?" confirmLabel="Deactivate" onClose={close}
          message={`${dialog.row.name} will be marked inactive and hidden from new doctor forms.`}
          onConfirm={async () => { await api.del(`/api/departments/${dialog.row.id}`); toast.success('Department deactivated.'); reload(); }} />
      )}
    </>
  );
}
