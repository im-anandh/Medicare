import { useMemo, useState } from 'react';
import { KeyRound, UserPlus } from 'lucide-react';
import { useToast } from '../context/ToastContext';
import { api } from '../lib/api';
import { fmtDateTime } from '../lib/format';
import { useFetch } from '../lib/useFetch';
import CreateAccountModal from '../components/CreateAccountModal';
import { DataTable, RowAction } from '../components/DataTable';
import { FormModal } from '../components/Form';
import { Button, PageHeader, Pill } from '../components/ui';

export default function AccountsPage() {
  const toast = useToast();
  const { data, loading, error, reload } = useFetch('/api/users');
  const [dialog, setDialog] = useState(null);
  const [role, setRole] = useState('all');
  const close = () => setDialog(null);

  const rows = useMemo(() => (data || []).filter((u) => role === 'all' || u.role === role), [data, role]);
  // Which doctors/patients already have a login (so they aren't offered again).
  const linked = useMemo(() => {
    const sets = { PATIENT: new Set(), DOCTOR: new Set() };
    (data || []).forEach((u) => { if (u.profileId && sets[u.role]) sets[u.role].add(u.profileId); });
    return sets;
  }, [data]);

  return (
    <>
      <PageHeader
        title="Accounts" subtitle="Everyone who can sign in to MediCare, and which record each login belongs to."
        actions={<Button icon={UserPlus} onClick={() => setDialog({ type: 'create' })}>New account</Button>}
      />
      <DataTable
        rows={rows} loading={loading} error={error} onRetry={reload} searchPlaceholder="Search username, email…"
        emptyTitle="No accounts"
        toolbar={(
          <select aria-label="Filter by role" value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="all">All roles</option>
            <option value="ADMIN">Admin</option><option value="RECEPTIONIST">Receptionist</option>
            <option value="DOCTOR">Doctor</option><option value="PATIENT">Patient</option>
          </select>
        )}
        columns={[
          { header: 'Username', cell: (u) => <div className="cell-main"><b>{u.username}</b><small>{u.email}</small></div> },
          { header: 'Role', cell: (u) => <Pill value={u.role} /> },
          { header: 'Linked record', cell: (u) => (u.profileId ? <div className="cell-main"><span>{u.profileName}</span><small>#{u.profileId}</small></div> : '—') },
          { header: 'Created', cell: (u) => fmtDateTime(u.createdAt) },
        ]}
        actions={(u) => <RowAction icon={KeyRound} label="Reset password" onClick={() => setDialog({ type: 'reset', row: u })} />}
      />
      {dialog?.type === 'create' && <CreateAccountModal linked={linked} onClose={close} onCreated={reload} />}
      {dialog?.type === 'reset' && (
        <FormModal
          title={`Reset password — ${dialog.row.username}`} size="sm" submitLabel="Set new password" onClose={close}
          subtitle="They will need the new password the next time they sign in."
          fields={[{ name: 'newPassword', label: 'New password', required: true, maxLength: 72, hint: 'At least 6 characters. Shown so you can pass it on securely.' }]}
          onSubmit={async (v) => { await api.patch(`/api/users/${dialog.row.id}/password`, { newPassword: v.newPassword }); toast.success(`Password reset for ${dialog.row.username}.`); }}
        />
      )}
    </>
  );
}
