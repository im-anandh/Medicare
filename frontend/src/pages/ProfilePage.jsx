import { useAuth } from '../context/AuthContext';
import { ageFrom, enumLabel, fmtDate } from '../lib/format';
import { useFetch } from '../lib/useFetch';
import { Card, Details, ErrorState, PageHeader, Pill, Spinner } from '../components/ui';

// A doctor's or patient's own record (what GET /api/.../me returns).
export default function ProfilePage() {
  const { session } = useAuth();
  const isDoctor = session.role === 'DOCTOR';
  const { data: p, loading, error, reload } = useFetch(isDoctor ? '/api/doctors/me' : '/api/patients/me');

  return (
    <>
      <PageHeader title="My profile" subtitle={isDoctor ? 'Your professional record.' : 'Your details on file with the hospital.'} />
      {loading && !p ? <Spinner /> : error ? (
        <ErrorState error={error} onRetry={reload} />
      ) : (
        <div className="grid-2">
          <Card title={isDoctor ? p.name : p.fullName} subtitle={`${enumLabel(session.role)} · record #${p.id}`}>
            {isDoctor ? (
              <Details rows={[
                ['Specialization', p.specialization], ['Department', p.department?.name], ['Experience', `${p.experience} years`],
                ['Availability', p.availability], ['Phone', p.phone], ['Email', p.email],
              ]} />
            ) : (
              <Details rows={[
                ['Date of birth', `${fmtDate(p.dateOfBirth)} (${ageFrom(p.dateOfBirth)} yrs)`], ['Gender', enumLabel(p.gender)],
                ['Blood group', p.bloodGroup], ['Phone', p.phone], ['Email', p.email],
                ['Emergency contact', p.emergencyContact], ['Address', p.address],
              ]} />
            )}
          </Card>
          <Card title="Sign-in" subtitle="Your account">
            <Details rows={[['Username', session.username], ['Role', enumLabel(session.role)]]} />
            <p className="status-line">Status: <Pill value={p.active ? 'ACTIVE' : 'INACTIVE'} /></p>
            <p className="muted small">To change your details, please contact the front desk.</p>
          </Card>
        </div>
      )}
    </>
  );
}
