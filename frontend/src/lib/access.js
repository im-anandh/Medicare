import {
  Building2, CalendarDays, ClipboardList, FileHeart, KeyRound, LayoutDashboard,
  MessageSquareText, Receipt, Stethoscope, UserRound, Users,
} from 'lucide-react';

// What each role may do in the UI. This MIRRORS SecurityConfig on the backend -
// the UI hides what the server would refuse, but the server is the real
// gatekeeper (a hidden button is a convenience, never the protection).
const ALL = ['ADMIN', 'RECEPTIONIST', 'DOCTOR', 'PATIENT'];

export const PERMISSIONS = {
  writePatients: ['ADMIN', 'RECEPTIONIST'],
  writeDoctors: ['ADMIN'],
  writeDepartments: ['ADMIN'],
  writeAppointments: ['ADMIN', 'RECEPTIONIST', 'DOCTOR'],
  writeRecords: ['DOCTOR'],
  writePrescriptions: ['DOCTOR'],
  writeBills: ['ADMIN', 'RECEPTIONIST'],
  runReminders: ['ADMIN'],
  createLogins: ['ADMIN', 'RECEPTIONIST'], // a receptionist may only create PATIENT logins
};
export const can = (role, permission) => (PERMISSIONS[permission] || []).includes(role);

// Sidebar. `labels` overrides the label for particular roles.
export const NAV = [
  { id: 'dashboard', label: 'Dashboard', icon: LayoutDashboard, roles: ALL, group: 'Overview' },
  { id: 'appointments', label: 'Appointments', icon: CalendarDays, roles: ALL, group: 'Overview' },
  { id: 'patients', label: 'Patients', labels: { DOCTOR: 'My patients' }, icon: Users, roles: ['ADMIN', 'RECEPTIONIST', 'DOCTOR'], group: 'Overview' },
  { id: 'profile', label: 'My profile', icon: UserRound, roles: ['DOCTOR', 'PATIENT'], group: 'Overview' },
  { id: 'doctors', label: 'Doctors', icon: Stethoscope, roles: ALL, group: 'Overview' },
  { id: 'records', label: 'Medical records', icon: FileHeart, roles: ['DOCTOR', 'PATIENT'], group: 'Clinical' },
  { id: 'prescriptions', label: 'Prescriptions', icon: ClipboardList, roles: ['DOCTOR', 'PATIENT'], group: 'Clinical' },
  { id: 'billing', label: 'Billing', icon: Receipt, roles: ['ADMIN', 'RECEPTIONIST', 'PATIENT'], group: 'Operations' },
  { id: 'sms', label: 'SMS log', icon: MessageSquareText, roles: ['ADMIN', 'RECEPTIONIST'], group: 'Operations' },
  { id: 'departments', label: 'Departments', icon: Building2, roles: ['ADMIN'], group: 'Administration' },
  { id: 'accounts', label: 'Accounts', icon: KeyRound, roles: ['ADMIN'], group: 'Administration' },
];

export const navFor = (role) => NAV.filter((item) => item.roles.includes(role));
export const routeAllowed = (role, id) => navFor(role).some((item) => item.id === id);
export const labelFor = (item, role) => (item.labels && item.labels[role]) || item.label;
