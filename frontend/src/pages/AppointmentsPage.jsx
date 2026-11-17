import { useMemo, useState } from 'react';
import { CalendarClock, CalendarPlus, Check, X } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { api } from '../lib/api';
import { can } from '../lib/access';
import { fmtDate, fmtDateTime, fmtTime, todayStr } from '../lib/format';
import { strOrNull } from '../lib/forms';
import { useFetch } from '../lib/useFetch';
import { DataTable, RowAction } from '../components/DataTable';
import { FormModal } from '../components/Form';
import { ConfirmDialog } from '../components/Modal';
import { Button, PageHeader, Pill } from '../components/ui';

const STATUSES = ['SCHEDULED', 'COMPLETED', 'CANCELLED', 'MISSED'];

export default function AppointmentsPage() {
  const { session } = useAuth();
  const toast = useToast();
  const canWrite = can(session.role, 'writeAppointments');
  const isDoctor = session.role === 'DOCTOR';
  const { data, loading, error, reload } = useFetch('/api/appointments');
  const patients = useFetch(canWrite ? '/api/patients' : null);
  const doctors = useFetch(canWrite ? '/api/doctors' : null);
  const [dialog, setDialog] = useState(null);
  const [status, setStatus] = useState('all');
  const [day, setDay] = useState('');
  const close = () => setDialog(null);

  // Upcoming first (soonest at the top), then past ones (most recent first).
  const rows = useMemo(() => {
    const today = todayStr();
    const key = (a) => `${a.appointmentDate} ${a.appointmentTime}`;
    const list = (data || []).filter((a) => (status === 'all' || a.status === status) && (!day || a.appointmentDate === day));
    const upcoming = list.filter((a) => a.appointmentDate >= today).sort((a, b) => key(a).localeCompare(key(b)));
    const past = list.filter((a) => a.appointmentDate < today).sort((a, b) => key(b).localeCompare(key(a)));
    return [...upcoming, ...past];
  }, [data, status, day]);

  const bookFields = [
    {
      name: 'patientId', label: 'Patient', type: 'select', required: true,
      options: (patients.data || []).filter((p) => p.active).map((p) => ({ value: String(p.id), label: `${p.fullName} (#${p.id})` })),
      hint: isDoctor && (patients.data || []).length === 0 ? 'You can only book patients you have already seen. New patients are booked by the front desk.' : undefined,
    },
    {
      name: 'doctorId', label: 'Doctor', type: 'select', required: true, disabled: isDoctor,
      options: (doctors.data || []).filter((d) => d.active).map((d) => ({ value: String(d.id), label: `${d.name} — ${d.specialization}` })),
    },
    { name: 'appointmentDate', label: 'Date', type: 'date', required: true, min: todayStr() },
    { name: 'appointmentTime', label: 'Time', type: 'time', required: true },
    { name: 'reason', label: 'Reason for visit', type: 'textarea', maxLength: 255 },
  ];
  const whenFields = [
    { name: 'appointmentDate', label: 'New date', type: 'date', required: true, min: todayStr() },
    { name: 'appointmentTime', label: 'New time', type: 'time', required: true },
  ];

  return (
    <>
      <PageHeader
        title="Appointments"
        subtitle={session.role === 'PATIENT' ? 'Your upcoming and past visits. To change one, contact the front desk.' : 'Book, reschedule and cancel visits. Patients get an SMS reminder before each one.'}
        actions={canWrite && <Button icon={CalendarPlus} onClick={() => setDialog({ type: 'book' })}>Book appointment</Button>}
      />

      <DataTable
        rows={rows} loading={loading} error={error} onRetry={reload} searchPlaceholder="Search patient, doctor, reason…"
        emptyTitle="No appointments" emptyText={canWrite ? 'Book the first appointment.' : 'Nothing is scheduled yet.'}
        toolbar={(
          <>
            <select aria-label="Filter by status" value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="all">All statuses</option>
              {STATUSES.map((s) => <option key={s} value={s}>{s.charAt(0) + s.slice(1).toLowerCase()}</option>)}
            </select>
            <input type="date" aria-label="Filter by date" value={day} onChange={(e) => setDay(e.target.value)} />
            {day && <button type="button" className="link-btn" onClick={() => setDay('')}>Clear date</button>}
          </>
        )}
        columns={[
          { header: 'When', cell: (a) => <div className="cell-main"><b>{fmtDate(a.appointmentDate)}</b><small>{fmtTime(a.appointmentTime)}</small></div> },
          { header: 'Patient', cell: (a) => a.patient?.fullName || '—' },
          { header: 'Doctor', cell: (a) => <div className="cell-main"><span>{a.doctor?.name || '—'}</span><small>{a.doctor?.specialization}</small></div> },
          { header: 'Reason', cell: (a) => a.reason || '—' },
          { header: 'Status', cell: (a) => <Pill value={a.status} /> },
          {
            header: 'Reminder',
            cell: (a) => (a.reminderSent ? <span className="ok-text"><Check size={14} /> Sent</span> : a.status === 'SCHEDULED' ? <span className="muted">Pending</span> : '—'),
          },
        ]}
        actions={canWrite ? (a) => (a.status === 'SCHEDULED' ? (
          <>
            <RowAction icon={CalendarClock} label="Reschedule" onClick={() => setDialog({ type: 'reschedule', row: a })} />
            <RowAction icon={X} label="Cancel" danger onClick={() => setDialog({ type: 'cancel', row: a })} />
          </>
        ) : null) : undefined}
      />

      {dialog?.type === 'book' && (
        <FormModal
          title="Book appointment" fields={bookFields} submitLabel="Book appointment" onClose={close}
          initial={{ doctorId: isDoctor && session.profile ? String(session.profile.id) : '' }}
          onSubmit={async (v) => {
            await api.post('/api/appointments', {
              patientId: Number(v.patientId), doctorId: Number(v.doctorId),
              appointmentDate: v.appointmentDate, appointmentTime: v.appointmentTime, reason: strOrNull(v.reason),
            });
            toast.success('Appointment booked.');
            reload();
          }}
        />
      )}
      {dialog?.type === 'reschedule' && (
        <FormModal
          title="Reschedule appointment" size="sm" fields={whenFields} submitLabel="Reschedule" onClose={close}
          subtitle={`${dialog.row.patient?.fullName} with ${dialog.row.doctor?.name} — currently ${fmtDateTime(`${dialog.row.appointmentDate}T${dialog.row.appointmentTime}`)}`}
          initial={{ appointmentDate: dialog.row.appointmentDate, appointmentTime: String(dialog.row.appointmentTime).slice(0, 5) }}
          onSubmit={async (v) => {
            await api.patch(`/api/appointments/${dialog.row.id}/reschedule`, { appointmentDate: v.appointmentDate, appointmentTime: v.appointmentTime });
            toast.success('Appointment rescheduled. A fresh reminder will be sent.');
            reload();
          }}
        />
      )}
      {dialog?.type === 'cancel' && (
        <ConfirmDialog
          danger title="Cancel this appointment?" confirmLabel="Cancel appointment" onClose={close}
          message={`${dialog.row.patient?.fullName} with ${dialog.row.doctor?.name} on ${fmtDate(dialog.row.appointmentDate)} at ${fmtTime(dialog.row.appointmentTime)}.`}
          onConfirm={async () => { await api.patch(`/api/appointments/${dialog.row.id}/cancel`); toast.success('Appointment cancelled.'); reload(); }}
        />
      )}
    </>
  );
}
