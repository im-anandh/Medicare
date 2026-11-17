import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { api } from '../lib/api';
import { useFetch } from '../lib/useFetch';
import { FormModal } from './Form';
import { Modal } from './Modal';
import { Button, Spinner } from './ui';

const ROLE_OPTIONS = [
  { value: 'ADMIN', label: 'Admin' },
  { value: 'RECEPTIONIST', label: 'Receptionist' },
  { value: 'DOCTOR', label: 'Doctor' },
  { value: 'PATIENT', label: 'Patient' },
];

/**
 * Create a login. Used three ways:
 *  - Accounts screen (admin):            any role, pick the profile
 *  - "Create login" on a patient row:    role + profile fixed (PATIENT, that patient)
 *  - "Create login" on a doctor row:     role + profile fixed (DOCTOR, that doctor)
 * A RECEPTIONIST can only ever create PATIENT logins (the server enforces it too).
 * linked: { PATIENT:Set, DOCTOR:Set } of profile ids that already have a login.
 */
export default function CreateAccountModal({ fixedRole, fixedProfileId, linked, onCreated, onClose }) {
  const { session } = useAuth();
  const toast = useToast();
  const receptionist = session.role === 'RECEPTIONIST';
  const roles = ROLE_OPTIONS.filter((r) => (receptionist ? r.value === 'PATIENT' : true));
  const startRole = fixedRole || (receptionist ? 'PATIENT' : '');

  const patients = useFetch(fixedRole === 'DOCTOR' ? null : '/api/patients');
  const doctors = useFetch(fixedRole === 'PATIENT' ? null : '/api/doctors');
  const waiting = patients.loading || doctors.loading;
  const usedBy = (role) => (linked && linked[role]) || new Set();

  const profileOptions = (values) => {
    if (values.role === 'PATIENT') {
      return (patients.data || [])
        .filter((p) => p.active && (!usedBy('PATIENT').has(p.id) || p.id === fixedProfileId))
        .map((p) => ({ value: String(p.id), label: `${p.fullName} (#${p.id})` }));
    }
    if (values.role === 'DOCTOR') {
      return (doctors.data || [])
        .filter((d) => d.active && (!usedBy('DOCTOR').has(d.id) || d.id === fixedProfileId))
        .map((d) => ({ value: String(d.id), label: `${d.name} — ${d.specialization}` }));
    }
    return [];
  };

  const fields = [
    { name: 'role', label: 'Role', type: 'select', required: true, options: roles, disabled: Boolean(fixedRole) || receptionist },
    {
      name: 'profileId', label: 'Linked record', type: 'select', required: true,
      hint: 'The patient/doctor record this login belongs to.',
      options: profileOptions, disabled: Boolean(fixedProfileId),
      hidden: (v) => v.role !== 'PATIENT' && v.role !== 'DOCTOR',
    },
    { name: 'username', label: 'Username', required: true, maxLength: 50, hint: '3–50 characters', autoComplete: 'off' },
    { name: 'email', label: 'Email', type: 'email', required: true },
    { name: 'password', label: 'Temporary password', type: 'text', required: true, maxLength: 72, hint: 'At least 6 characters. Share it securely; it can be reset later.', span: 2 },
  ];

  if (waiting) {
    return (
      <Modal title="New login" size="sm" onClose={onClose}>
        <Spinner />
        <div className="modal-inline-actions"><Button variant="secondary" onClick={onClose}>Cancel</Button></div>
      </Modal>
    );
  }

  return (
    <FormModal
      title="Create a login" subtitle="Gives a person access to MediCare with their own role."
      fields={fields} submitLabel="Create login" onClose={onClose}
      initial={{ role: startRole, profileId: fixedProfileId ? String(fixedProfileId) : '' }}
      onSubmit={async (v) => {
        const needsProfile = v.role === 'PATIENT' || v.role === 'DOCTOR';
        await api.post('/api/users', {
          username: v.username.trim(), email: v.email.trim(), password: v.password, role: v.role,
          profileId: needsProfile ? Number(v.profileId) : null,
        });
        toast.success(`Login "${v.username.trim()}" created.`);
        onCreated?.();
      }}
    />
  );
}
