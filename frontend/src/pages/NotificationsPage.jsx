import { useMemo, useState } from 'react';
import { BellRing } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { api } from '../lib/api';
import { can } from '../lib/access';
import { fmtDate, fmtDateTime, fmtTime } from '../lib/format';
import { useFetch } from '../lib/useFetch';
import { DataTable } from '../components/DataTable';
import { Modal } from '../components/Modal';
import { Alert, Button, Details, PageHeader, Pill } from '../components/ui';

export default function NotificationsPage() {
  const { session } = useAuth();
  const toast = useToast();
  const canRun = can(session.role, 'runReminders');
  const { data, loading, error, reload } = useFetch('/api/notifications');
  // Is the server really sending SMS, or only simulating (mock provider)?
  const provider = useFetch('/api/notifications/provider');
  const simulated = provider.data && provider.data.delivers === false;
  const [status, setStatus] = useState('all');
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState(null);

  const rows = useMemo(() => {
    const list = data || [];
    return status === 'all' ? list : list.filter((n) => n.status === status);
  }, [data, status]);

  const run = async () => {
    setRunning(true);
    try {
      setResult(await api.post('/api/notifications/run-reminders'));
      reload();
    } catch (e) {
      toast.error(e.message);
    } finally {
      setRunning(false);
    }
  };

  return (
    <>
      <PageHeader
        title="SMS log"
        subtitle="Every reminder attempt, newest first. Reminders go out automatically for appointments in the next 24 hours."
        actions={canRun && <Button icon={BellRing} loading={running} onClick={run}>Run reminders now</Button>}
      />
      {simulated && (
        <Alert kind="warning">
          <b>Test mode — no real SMS are being sent.</b> The server is using the mock SMS provider, which only writes
          each message to its own log. “Sent” below means <i>simulated</i>, not delivered to a phone. To send real
          messages, start the server with <code>SMS_PROVIDER=twilio</code> and your Twilio settings.
        </Alert>
      )}
      <DataTable
        rows={rows} loading={loading} error={error} onRetry={reload} searchPlaceholder="Search patient, doctor, message…"
        emptyTitle="No reminders sent yet" emptyText="They appear here once an appointment is within 24 hours."
        toolbar={(
          <select aria-label="Filter by status" value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="all">All results</option>
            <option value="SENT">Sent</option>
            <option value="FAILED">Failed</option>
          </select>
        )}
        columns={[
          { header: 'Attempted', cell: (n) => fmtDateTime(n.attemptedAt) },
          { header: 'Patient', cell: (n) => <div className="cell-main"><span>{n.patientName}</span><small>Appt #{n.appointmentId} · {fmtDate(n.appointmentDate)} {fmtTime(n.appointmentTime)}</small></div> },
          { header: 'Doctor', cell: (n) => n.doctorName },
          { header: 'Message', cell: (n) => <span className="clamp wide" title={n.message}>{n.message}</span> },
          { header: 'Result', cell: (n) => <div className="cell-main"><Pill value={n.status} />{n.failureReason && <small className="err-text" title={n.failureReason}>{n.failureReason}</small>}</div> },
        ]}
      />
      {result && (
        <Modal title="Reminder run finished" size="sm" onClose={() => setResult(null)} footer={<Button onClick={() => setResult(null)}>Done</Button>}>
          {result.note ? <p>{result.note}</p> : (
            <>
              {simulated && <Alert kind="warning">Test mode: nothing was delivered to any phone.</Alert>}
              <Details rows={[['Appointments checked', result.checked], ['Reminders sent', result.sent], ['Failed', result.failed], ['Skipped', result.skipped]]} />
              <p className="muted small">
                Looked for appointments between {fmtDateTime(result.windowStart)} and {fmtDateTime(result.windowEnd)}.
                {result.checked === 0 && ' Nothing was due — either everything in that window was already reminded, or nothing is booked yet.'}
              </p>
            </>
          )}
        </Modal>
      )}
    </>
  );
}
