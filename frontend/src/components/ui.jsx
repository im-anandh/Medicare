import { Inbox, Loader2, TriangleAlert } from 'lucide-react';
import { enumLabel } from '../lib/format';

export function Button({ variant = 'primary', size = 'md', icon: Icon, loading = false, disabled, children, className = '', type = 'button', ...rest }) {
  return (
    <button type={type} className={`btn btn-${variant} btn-${size} ${className}`} disabled={loading || disabled} {...rest}>
      {loading ? <Loader2 size={15} className="spin" /> : Icon ? <Icon size={15} /> : null}
      {children}
    </button>
  );
}

// Status/role badges. The colour comes from the value, so every screen shows
// "PAID", "SCHEDULED", "FAILED"... in the same colours.
const TONES = {
  green: ['ACTIVE', 'PAID', 'COMPLETED', 'SENT'],
  amber: ['PENDING', 'SCHEDULED'],
  red: ['CANCELLED', 'FAILED', 'MISSED', 'INACTIVE'],
  blue: ['RESCHEDULED', 'ADMIN', 'DOCTOR', 'RECEPTIONIST', 'PATIENT'],
};
export function Pill({ value, tone, children }) {
  const found = tone || Object.keys(TONES).find((t) => TONES[t].includes(String(value))) || 'gray';
  return <span className={`pill pill-${found}`}>{children ?? enumLabel(value)}</span>;
}

export function Spinner({ label = 'Loading…' }) {
  return (
    <div className="state-box" role="status">
      <Loader2 size={22} className="spin" />
      <span>{label}</span>
    </div>
  );
}

export function EmptyState({ icon: Icon = Inbox, title, text, action }) {
  return (
    <div className="state-box">
      <div className="state-icon"><Icon size={22} /></div>
      <strong>{title}</strong>
      {text && <p>{text}</p>}
      {action}
    </div>
  );
}

export function ErrorState({ error, onRetry }) {
  return (
    <div className="state-box state-error" role="alert">
      <div className="state-icon"><TriangleAlert size={22} /></div>
      <strong>{error?.status === 403 ? 'Access denied' : 'Something went wrong'}</strong>
      <p>{error?.message || 'Please try again.'}</p>
      {onRetry && <Button variant="secondary" size="sm" onClick={onRetry}>Try again</Button>}
    </div>
  );
}

export function Alert({ kind = 'error', children }) {
  return <div className={`alert alert-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>{children}</div>;
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}

export function Card({ title, subtitle, actions, children, className = '' }) {
  return (
    <section className={`card ${className}`}>
      {(title || actions) && (
        <header className="card-head">
          <div>
            {title && <h2>{title}</h2>}
            {subtitle && <p>{subtitle}</p>}
          </div>
          {actions}
        </header>
      )}
      {children}
    </section>
  );
}

export function StatCard({ icon: Icon, label, value, hint, tone = 'blue', loading, onClick }) {
  const Tag = onClick ? 'button' : 'div';
  return (
    <Tag className={`stat ${onClick ? 'stat-link' : ''}`} {...(onClick ? { type: 'button', onClick } : {})}>
      <div className={`stat-icon tone-${tone}`}><Icon size={20} /></div>
      <span className="stat-label">{label}</span>
      <strong className="stat-value">{loading ? '…' : value}</strong>
      {hint && <small>{hint}</small>}
    </Tag>
  );
}

// Label/value pairs for "view details" dialogs. Empty values show a dash.
export function Details({ rows }) {
  return (
    <dl className="details">
      {rows.map(([label, value]) => (
        <div key={label}>
          <dt>{label}</dt>
          <dd>{value === null || value === undefined || value === '' ? '—' : value}</dd>
        </div>
      ))}
    </dl>
  );
}
