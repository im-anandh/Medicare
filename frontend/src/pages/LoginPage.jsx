import { useState } from 'react';
import { ArrowRight, HeartPulse, ShieldCheck } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Field } from '../components/Form';
import { Alert, Button } from '../components/ui';
import { todayStr } from '../lib/format';

const LOGIN_FIELDS = [
  { name: 'username', label: 'Username', placeholder: 'Enter your username', autoComplete: 'username', span: 2 },
  { name: 'password', label: 'Password', type: 'password', placeholder: 'Enter your password', autoComplete: 'current-password', span: 2 },
];

const SIGNUP_FIELDS = [
  { name: 'profileId', label: 'Patient ID', type: 'number', min: 1, placeholder: 'e.g. 12', hint: 'Given to you by the front desk.' },
  { name: 'dateOfBirth', label: 'Date of birth', type: 'date', max: todayStr() },
  { name: 'phone', label: 'Phone number on file', type: 'tel', placeholder: '98765 43210', span: 2, hint: 'The number you gave the hospital. Used to confirm it is really you.' },
  { name: 'username', label: 'Choose a username', placeholder: 'At least 3 characters', autoComplete: 'username' },
  { name: 'email', label: 'Email', type: 'email', autoComplete: 'email' },
  { name: 'password', label: 'Choose a password', type: 'password', placeholder: 'At least 6 characters', autoComplete: 'new-password', span: 2 },
];

export default function LoginPage() {
  const { login, register, notice } = useAuth();
  const [mode, setMode] = useState('login');
  const [values, setValues] = useState({});
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);

  const fields = mode === 'login' ? LOGIN_FIELDS : SIGNUP_FIELDS;
  const set = (name, value) => { setValues((v) => ({ ...v, [name]: value })); setFieldErrors((e) => ({ ...e, [name]: undefined })); };
  const switchMode = (next) => { setMode(next); setError(''); setFieldErrors({}); setValues({}); };

  const submit = async (e) => {
    e.preventDefault();
    const missing = {};
    fields.forEach((f) => { if (!String(values[f.name] ?? '').trim()) missing[f.name] = 'Required'; });
    if (Object.keys(missing).length) { setFieldErrors(missing); setError('Please fill in every field.'); return; }

    setBusy(true);
    setError('');
    try {
      if (mode === 'login') {
        await login(values.username.trim(), values.password);
      } else {
        await register({
          role: 'PATIENT', profileId: Number(values.profileId), dateOfBirth: values.dateOfBirth,
          phone: values.phone.trim(), username: values.username.trim(), email: values.email.trim(), password: values.password,
        });
      }
    } catch (err) {
      setError(err.message);
      setFieldErrors(err.fieldErrors || {});
      setBusy(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-brand">
        <span className="brand-mark"><HeartPulse size={22} /></span>
        <span className="login-brand-name">MediCare</span>
        <span className="login-brand-tag">CARE OPERATIONS</span>
      </div>

      <div className="login-card">
        <div className="login-icon"><ShieldCheck size={24} /></div>
        <p className="eyebrow">{mode === 'login' ? 'WELCOME BACK' : 'PATIENT PORTAL'}</p>
        <h1>{mode === 'login' ? 'Care starts here.' : 'Create your account'}</h1>
        <p className="login-sub">
          {mode === 'login' ? 'Sign in to your secure workspace.' : 'Patients can sign up to see their own appointments, prescriptions and bills.'}
        </p>

        <div className="tabs" role="tablist">
          <button type="button" role="tab" aria-selected={mode === 'login'} className={mode === 'login' ? 'on' : ''} onClick={() => switchMode('login')}>Sign in</button>
          <button type="button" role="tab" aria-selected={mode === 'signup'} className={mode === 'signup' ? 'on' : ''} onClick={() => switchMode('signup')}>Patient sign-up</button>
        </div>

        {notice && mode === 'login' && !error && <Alert kind="info">{notice}</Alert>}
        {error && <Alert>{error}</Alert>}

        <form onSubmit={submit} noValidate>
          <div className="form-grid">
            {fields.map((def) => (
              <Field key={`${mode}-${def.name}`} def={def} values={values} value={values[def.name]} onChange={set} error={fieldErrors[def.name]} />
            ))}
          </div>
          <Button type="submit" className="btn-block" size="lg" loading={busy} icon={mode === 'login' ? ArrowRight : undefined}>
            {mode === 'login' ? 'Sign in securely' : 'Create account'}
          </Button>
        </form>

        <div className="login-foot"><ShieldCheck size={15} /> Protected access · JWT authentication</div>
        {mode === 'login' && <p className="login-help">Staff accounts are created by an administrator.</p>}
      </div>
      <p className="login-copy">© {new Date().getFullYear()} MediCare Health Systems</p>
    </div>
  );
}
