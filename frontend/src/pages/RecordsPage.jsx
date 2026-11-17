import { useState } from 'react';
import { Eye, FilePlus2, Pencil } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { api } from '../lib/api';
import { can } from '../lib/access';
import { fmtDate, todayStr } from '../lib/format';
import { strOrNull, trimmed } from '../lib/forms';
import { useFetch } from '../lib/useFetch';
import { DataTable, RowAction } from '../components/DataTable';
import { FormModal } from '../components/Form';
import { Modal } from '../components/Modal';
import { Button, Details, PageHeader } from '../components/ui';

export default function RecordsPage() {
  const { session } = useAuth();
  const toast = useToast();
  const canWrite = can(session.role, 'writeRecords');
  const { data, loading, error, reload } = useFetch('/api/medical-records');
  const patients = useFetch(canWrite ? '/api/patients' : null);
  const [dialog, setDialog] = useState(null);
  const close = () => setDialog(null);
  const rows = [...(data || [])].sort((a, b) => b.visitDate.localeCompare(a.visitDate) || b.id - a.id);

  const fields = (editing) => [
    {
      name: 'patientId', label: 'Patient', type: 'select', required: true, disabled: editing,
      options: (patients.data || []).filter((p) => p.active).map((p) => ({ value: String(p.id), label: `${p.fullName} (#${p.id})` })),
    },
    { name: 'visitDate', label: 'Visit date', type: 'date', required: true, max: todayStr() },
    { name: 'symptoms', label: 'Symptoms', type: 'textarea', required: true },
    { name: 'diagnosis', label: 'Diagnosis', required: true, maxLength: 255, span: 2 },
    { name: 'treatment', label: 'Treatment', type: 'textarea' },
    { name: 'medicalNotes', label: 'Notes', type: 'textarea' },
    { name: 'followUpDate', label: 'Follow-up date', type: 'date', min: todayStr() },
  ];
  const payload = (v, patientId) => ({
    patientId: Number(patientId ?? v.patientId), doctorId: session.profile?.id,
    visitDate: v.visitDate, symptoms: trimmed(v.symptoms), diagnosis: trimmed(v.diagnosis),
    treatment: strOrNull(v.treatment), medicalNotes: strOrNull(v.medicalNotes), followUpDate: v.followUpDate || null,
  });

  return (
    <>
      <PageHeader
        title="Medical records"
        subtitle={session.role === 'PATIENT' ? 'Your visit history, written by your doctors.' : 'Visits you have documented. Only you and the patient can see these.'}
        actions={canWrite && <Button icon={FilePlus2} onClick={() => setDialog({ type: 'create' })}>New record</Button>}
      />
      <DataTable
        rows={rows} loading={loading} error={error} onRetry={reload} searchPlaceholder="Search diagnosis, patient…"
        emptyTitle="No medical records" emptyText={canWrite ? 'Document your first visit.' : 'Your doctors have not added any records yet.'}
        columns={[
          { header: 'Visit', cell: (r) => <div className="cell-main"><b>{fmtDate(r.visitDate)}</b><small>Record #{r.id}</small></div> },
          { header: 'Patient', cell: (r) => r.patient?.fullName || '—' },
          { header: 'Doctor', cell: (r) => r.doctor?.name || '—' },
          { header: 'Diagnosis', cell: (r) => r.diagnosis },
          { header: 'Follow-up', cell: (r) => (r.followUpDate ? fmtDate(r.followUpDate) : '—') },
        ]}
        actions={(r) => (
          <>
            <RowAction icon={Eye} label="View" onClick={() => setDialog({ type: 'view', row: r })} />
            {canWrite && <RowAction icon={Pencil} label="Edit" onClick={() => setDialog({ type: 'edit', row: r })} />}
          </>
        )}
      />

      {dialog?.type === 'create' && (
        <FormModal
          title="New medical record" size="lg" fields={fields(false)} submitLabel="Save record" onClose={close}
          initial={{ visitDate: todayStr() }}
          onSubmit={async (v) => { await api.post('/api/medical-records', payload(v)); toast.success('Medical record saved.'); reload(); }}
        />
      )}
      {dialog?.type === 'edit' && (
        <FormModal
          title={`Edit record #${dialog.row.id}`} size="lg" fields={fields(true)} onClose={close}
          initial={{
            patientId: String(dialog.row.patient?.id ?? ''), visitDate: dialog.row.visitDate, symptoms: dialog.row.symptoms ?? '',
            diagnosis: dialog.row.diagnosis ?? '', treatment: dialog.row.treatment ?? '', medicalNotes: dialog.row.medicalNotes ?? '',
            followUpDate: dialog.row.followUpDate ?? '',
          }}
          onSubmit={async (v) => { await api.put(`/api/medical-records/${dialog.row.id}`, payload(v, dialog.row.patient?.id)); toast.success('Record updated.'); reload(); }}
        />
      )}
      {dialog?.type === 'view' && (
        <Modal title={`Visit on ${fmtDate(dialog.row.visitDate)}`} subtitle={`Record #${dialog.row.id}`} size="lg" onClose={close}
          footer={<Button variant="secondary" onClick={close}>Close</Button>}>
          <Details rows={[
            ['Patient', dialog.row.patient?.fullName], ['Doctor', `${dialog.row.doctor?.name} — ${dialog.row.doctor?.specialization}`],
            ['Symptoms', dialog.row.symptoms], ['Diagnosis', dialog.row.diagnosis], ['Treatment', dialog.row.treatment],
            ['Notes', dialog.row.medicalNotes], ['Follow-up', dialog.row.followUpDate ? fmtDate(dialog.row.followUpDate) : null],
          ]} />
        </Modal>
      )}
    </>
  );
}
