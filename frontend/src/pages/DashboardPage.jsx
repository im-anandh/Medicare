import { ArrowRight, CalendarDays, ClipboardList, FileHeart, Receipt, Stethoscope, Users } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { enumLabel, fmtDate, fmtTime, greeting, longDate, money, todayStr } from '../lib/format';
import { useFetch } from '../lib/useFetch';
import { DataTable } from '../components/DataTable';
import { Button, Card, Pill, StatCard } from '../components/ui';

// A number for a stat card: "—" if that request failed (e.g. no permission),
// a count while it loads, never a crash.
const countOf = (f, pick = (d) => d.length) => (f.error ? '—' : f.data ? pick(f.data) : 0);
const upcoming = (appointments) => {
  const today = todayStr();
  return (appointments || [])
    .filter((a) => a.status === 'SCHEDULED' && a.appointmentDate >= today)
    .sort((a, b) => `${a.appointmentDate} ${a.appointmentTime}`.localeCompare(`${b.appointmentDate} ${b.appointmentTime}`));
};

function Welcome({ name, subtitle, action }) {
  const { session } = useAuth();
  return (
    <section className="welcome">
      <div>
        <p className="eyebrow light">{longDate().toUpperCase()} · {enumLabel(session.role).toUpperCase()}</p>
        <h1>{greeting()}, {name}.</h1>
        <p>{subtitle}</p>
        {action}
      </div>
    </section>
  );
}

function UpcomingCard({ appointments, navigate, showPatient = true }) {
  const rows = upcoming(appointments.data).slice(0, 6);
  return (
    <Card
      title="Upcoming appointments" subtitle="The next scheduled visits"
      actions={<Button variant="ghost" size="sm" onClick={() => navigate('appointments')}>View all <ArrowRight size={14} /></Button>}
    >
      <DataTable
        rows={rows} loading={appointments.loading} error={appointments.error} onRetry={appointments.reload}
        searchable={false} pageSize={6} emptyTitle="Nothing scheduled" emptyText="Upcoming appointments will show here."
        columns={[
          { header: 'When', cell: (a) => <div className="cell-main"><b>{fmtDate(a.appointmentDate)}</b><small>{fmtTime(a.appointmentTime)}</small></div> },
          ...(showPatient ? [{ header: 'Patient', cell: (a) => a.patient?.fullName }] : []),
          { header: 'Doctor', cell: (a) => a.doctor?.name },
          { header: 'Status', cell: (a) => <Pill value={a.status} /> },
        ]}
      />
    </Card>
  );
}

function StaffDashboard({ navigate }) {
  const { session } = useAuth();
  const patients = useFetch('/api/patients');
  const doctors = useFetch('/api/doctors');
  const appointments = useFetch('/api/appointments');
  const bills = useFetch('/api/bills');
  const today = todayStr();
  const pending = (bills.data || []).filter((b) => b.paymentStatus === 'PENDING');

  return (
    <>
      <Welcome
        name={session.username} subtitle="Here is what is happening across the hospital today."
        action={<Button variant="light" onClick={() => navigate('appointments')}>View appointments <ArrowRight size={15} /></Button>}
      />
      <div className="stats-grid">
        <StatCard icon={Users} label="Active patients" tone="blue" loading={patients.loading} value={countOf(patients, (d) => d.filter((p) => p.active).length)} onClick={() => navigate('patients')} />
        <StatCard icon={Stethoscope} label="Active doctors" tone="violet" loading={doctors.loading} value={countOf(doctors, (d) => d.filter((x) => x.active).length)} onClick={() => navigate('doctors')} />
        <StatCard icon={CalendarDays} label="Appointments today" tone="green" loading={appointments.loading} value={countOf(appointments, (d) => d.filter((a) => a.status === 'SCHEDULED' && a.appointmentDate === today).length)} onClick={() => navigate('appointments')} />
        <StatCard icon={Receipt} label="Pending bills" tone="amber" loading={bills.loading} value={countOf(bills, () => pending.length)} hint={bills.data ? `${money(pending.reduce((s, b) => s + Number(b.totalAmount), 0))} outstanding` : undefined} onClick={() => navigate('billing')} />
      </div>
      <UpcomingCard appointments={appointments} navigate={navigate} />
    </>
  );
}

function DoctorDashboard({ navigate }) {
  const { session } = useAuth();
  const appointments = useFetch('/api/appointments');
  const patients = useFetch('/api/patients');
  const records = useFetch('/api/medical-records');
  const prescriptions = useFetch('/api/prescriptions');
  const today = todayStr();
  const name = session.profile?.name || session.username;

  return (
    <>
      <Welcome
        name={name} subtitle="Your schedule and patients at a glance."
        action={<Button variant="light" onClick={() => navigate('records')}>Document a visit <ArrowRight size={15} /></Button>}
      />
      <div className="stats-grid">
        <StatCard icon={CalendarDays} label="Appointments today" tone="green" loading={appointments.loading} value={countOf(appointments, (d) => d.filter((a) => a.status === 'SCHEDULED' && a.appointmentDate === today).length)} onClick={() => navigate('appointments')} />
        <StatCard icon={Users} label="My patients" tone="blue" loading={patients.loading} value={countOf(patients)} onClick={() => navigate('patients')} />
        <StatCard icon={FileHeart} label="Medical records" tone="violet" loading={records.loading} value={countOf(records)} onClick={() => navigate('records')} />
        <StatCard icon={ClipboardList} label="Prescriptions issued" tone="amber" loading={prescriptions.loading} value={countOf(prescriptions)} onClick={() => navigate('prescriptions')} />
      </div>
      <UpcomingCard appointments={appointments} navigate={navigate} />
    </>
  );
}

function PatientDashboard({ navigate }) {
  const { session } = useAuth();
  const appointments = useFetch('/api/appointments');
  const bills = useFetch('/api/bills');
  const prescriptions = useFetch('/api/prescriptions');
  const records = useFetch('/api/medical-records');
  const next = upcoming(appointments.data)[0];
  const due = (bills.data || []).filter((b) => b.paymentStatus === 'PENDING').reduce((s, b) => s + Number(b.totalAmount), 0);

  return (
    <>
      <Welcome name={session.profile?.fullName?.split(' ')[0] || session.username} subtitle="Your health information in one place." />
      <div className="stats-grid">
        <StatCard icon={CalendarDays} label="Next appointment" tone="green" loading={appointments.loading}
          value={appointments.error ? '—' : next ? fmtDate(next.appointmentDate) : 'None'} hint={next ? `${fmtTime(next.appointmentTime)} · ${next.doctor?.name}` : undefined} onClick={() => navigate('appointments')} />
        <StatCard icon={Receipt} label="Balance due" tone="amber" loading={bills.loading} value={bills.error ? '—' : money(due)} onClick={() => navigate('billing')} />
        <StatCard icon={ClipboardList} label="Prescriptions" tone="violet" loading={prescriptions.loading} value={countOf(prescriptions)} onClick={() => navigate('prescriptions')} />
        <StatCard icon={FileHeart} label="Medical records" tone="blue" loading={records.loading} value={countOf(records)} onClick={() => navigate('records')} />
      </div>
      <UpcomingCard appointments={appointments} navigate={navigate} showPatient={false} />
    </>
  );
}

export default function DashboardPage({ navigate }) {
  const { session } = useAuth();
  if (session.role === 'DOCTOR') return <DoctorDashboard navigate={navigate} />;
  if (session.role === 'PATIENT') return <PatientDashboard navigate={navigate} />;
  return <StaffDashboard navigate={navigate} />;
}
