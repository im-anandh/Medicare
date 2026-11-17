import { useEffect } from 'react';
import { useAuth } from './context/AuthContext';
import { routeAllowed } from './lib/access';
import { useHashRoute } from './lib/useHashRoute';
import Layout from './components/Layout';
import AccountsPage from './pages/AccountsPage';
import AppointmentsPage from './pages/AppointmentsPage';
import BillingPage from './pages/BillingPage';
import DashboardPage from './pages/DashboardPage';
import DepartmentsPage from './pages/DepartmentsPage';
import DoctorsPage from './pages/DoctorsPage';
import LoginPage from './pages/LoginPage';
import NotificationsPage from './pages/NotificationsPage';
import PatientsPage from './pages/PatientsPage';
import PrescriptionsPage from './pages/PrescriptionsPage';
import ProfilePage from './pages/ProfilePage';
import RecordsPage from './pages/RecordsPage';

const PAGES = {
  dashboard: DashboardPage, appointments: AppointmentsPage, patients: PatientsPage, profile: ProfilePage,
  doctors: DoctorsPage, records: RecordsPage, prescriptions: PrescriptionsPage, billing: BillingPage,
  sms: NotificationsPage, departments: DepartmentsPage, accounts: AccountsPage,
};

export default function App() {
  const { session } = useAuth();
  const [route, navigate] = useHashRoute();
  const allowed = session ? routeAllowed(session.role, route) : false;
  const current = allowed ? route : 'dashboard';

  // Typed a URL for a page this role can't use? Send them to the dashboard.
  useEffect(() => { if (session && !allowed) navigate('dashboard'); }, [session, allowed, navigate]);
  useEffect(() => { window.scrollTo(0, 0); }, [current]);

  if (!session) return <LoginPage />;
  const Page = PAGES[current];
  return (
    <Layout route={current} navigate={navigate}>
      <Page navigate={navigate} />
    </Layout>
  );
}
