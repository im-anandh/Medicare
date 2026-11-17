import { useMemo, useState } from 'react';
import { Plus, Trash2 } from 'lucide-react';
import { Modal } from './Modal';
import { Alert, Button } from './ui';

const isEmpty = (v) => v === '' || v === null || v === undefined || (typeof v === 'string' && v.trim() === '');

export function Field({ def, value, onChange, error, values }) {
  const id = `f-${def.name}`;
  const options = typeof def.options === 'function' ? def.options(values) : def.options;
  const disabled = typeof def.disabled === 'function' ? def.disabled(values) : def.disabled;
  const common = {
    id, name: def.name, value: value ?? '', disabled, required: false,
    'aria-invalid': error ? 'true' : undefined,
    onChange: (e) => onChange(def.name, e.target.value),
  };

  let control;
  if (def.type === 'select') {
    control = (
      <select {...common}>
        <option value="">{def.placeholder || 'Select…'}</option>
        {(options || []).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
    );
  } else if (def.type === 'textarea') {
    control = <textarea {...common} rows={def.rows || 3} maxLength={def.maxLength} placeholder={def.placeholder} />;
  } else {
    control = (
      <input
        {...common} type={def.type || 'text'} placeholder={def.placeholder} maxLength={def.maxLength}
        min={def.min} max={def.max} step={def.step} autoComplete={def.autoComplete || 'off'}
      />
    );
  }

  return (
    <div className={`field ${def.span === 2 || def.type === 'textarea' ? 'span-2' : ''} ${error ? 'has-error' : ''}`}>
      <label htmlFor={id}>{def.label}{def.required && <span className="req"> *</span>}</label>
      {control}
      {error ? <small className="field-error">{error}</small> : def.hint && <small className="field-hint">{def.hint}</small>}
    </div>
  );
}

/**
 * A dialog containing a form, driven by a list of field definitions:
 *   { name, label, type, required, options, placeholder, hint, disabled, hidden, span, min, max, step, maxLength }
 *
 * - required fields are checked before anything is sent;
 * - if the SERVER rejects the request, its per-field messages (fieldErrors) are
 *   shown under the matching inputs, and any it reports for fields this form
 *   doesn't have are listed in the red box at the top;
 * - `children` is an optional render function for extra content such as the
 *   line-item editor:  ({ values, setValue, fieldErrors }) => node
 * - onSubmit(values) must return a promise; the dialog closes when it resolves.
 */
export function FormModal({ title, subtitle, fields, initial = {}, submitLabel = 'Save', onSubmit, onClose, size = 'md', validate, onFieldChange, inlinePrefixes = ['items['], children }) {
  const [values, setValues] = useState(() => {
    const base = {};
    fields.forEach((f) => { base[f.name] = ''; });
    return { ...base, ...initial };
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const setValue = (name, value) => {
    // onFieldChange lets a screen react to an edit (e.g. load the chosen
    // patient's appointments) and optionally reset other fields by returning
    // them, e.g. { appointmentId: '' }.
    const extra = onFieldChange ? onFieldChange(name, value) : null;
    setValues((v) => ({ ...v, [name]: value, ...(extra || {}) }));
    setError('');   // an old error banner no longer applies once the user changes something
    if (fieldErrors[name]) setFieldErrors((e) => { const { [name]: _gone, ...rest } = e; return rest; });
  };

  const visible = fields.filter((f) => !(typeof f.hidden === 'function' ? f.hidden(values) : f.hidden));
  const known = useMemo(() => new Set(fields.map((f) => f.name)), [fields]);
  // Errors for the line-item rows ("items[0].medicine") are shown on their own cells, not repeated here.
  const unmatched = Object.entries(fieldErrors).filter(([k]) => !known.has(k) && !inlinePrefixes.some((p) => k.startsWith(p)));

  const submit = async (e) => {
    e.preventDefault();
    const problems = {};
    visible.forEach((f) => { if (f.required && isEmpty(values[f.name])) problems[f.name] = 'This field is required'; });
    Object.assign(problems, validate ? validate(values) : {});
    if (Object.keys(problems).length) {
      setFieldErrors(problems);
      setError('Please fix the highlighted fields.');
      return;
    }
    setBusy(true);
    setError('');
    setFieldErrors({});
    try {
      await onSubmit(values);
      onClose();
    } catch (err) {
      setError(err.message || 'Could not save');
      setFieldErrors(err.fieldErrors || {});
      setBusy(false);
    }
  };

  return (
    <Modal
      title={title} subtitle={subtitle} size={size} onClose={onClose} dismissable={!busy}
      footer={(
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>Cancel</Button>
          <Button type="submit" form="modal-form" loading={busy}>{submitLabel}</Button>
        </>
      )}
    >
      <form id="modal-form" onSubmit={submit} noValidate>
        {error && (
          <Alert>
            {error}
            {unmatched.length > 0 && (
              <ul className="alert-list">
                {unmatched.map(([k, v]) => <li key={k}><code>{k}</code> {v}</li>)}
              </ul>
            )}
          </Alert>
        )}
        <div className="form-grid">
          {visible.map((def) => (
            <Field key={def.name} def={def} values={values} value={values[def.name]} onChange={setValue} error={fieldErrors[def.name]} />
          ))}
        </div>
        {children && children({ values, setValue, fieldErrors })}
      </form>
    </Modal>
  );
}

/**
 * Editable list of rows (medicines on a prescription, extra lines on a bill).
 * columns: [{ name, label, placeholder, type, width, step, min }]
 * Server errors arrive keyed like "items[0].medicine" and are shown on that cell.
 */
export function LineItems({ label, items, onChange, columns, addLabel = 'Add line', errors = {}, prefix = 'items', min = 1 }) {
  const update = (i, name, v) => onChange(items.map((it, idx) => (idx === i ? { ...it, [name]: v } : it)));
  const add = () => onChange([...items, Object.fromEntries(columns.map((c) => [c.name, '']))]);
  const remove = (i) => onChange(items.filter((_, idx) => idx !== i));
  const template = `${columns.map((c) => c.width || '1fr').join(' ')} 36px`;

  return (
    <div className="lineitems">
      <div className="lineitems-head">
        <strong>{label}</strong>
        <Button variant="secondary" size="sm" icon={Plus} onClick={add}>{addLabel}</Button>
      </div>
      {items.length === 0 && <p className="lineitems-empty">None added.</p>}
      {items.length > 0 && (
        <div className="lineitems-row lineitems-labels" style={{ gridTemplateColumns: template }}>
          {columns.map((c) => <span key={c.name}>{c.label}</span>)}
          <span />
        </div>
      )}
      {items.map((item, i) => (
        <div key={i} className="lineitems-row" style={{ gridTemplateColumns: template }}>
          {columns.map((c) => {
            const err = errors[`${prefix}[${i}].${c.name}`];
            return (
              <div key={c.name} className={err ? 'has-error' : ''}>
                <input
                  aria-label={`${c.label} ${i + 1}`} type={c.type || 'text'} value={item[c.name] ?? ''}
                  placeholder={c.placeholder} step={c.step} min={c.min}
                  onChange={(e) => update(i, c.name, e.target.value)}
                />
                {err && <small className="field-error">{err}</small>}
              </div>
            );
          })}
          <button type="button" className="icon-button danger" aria-label={`Remove line ${i + 1}`} disabled={items.length <= min} onClick={() => remove(i)}>
            <Trash2 size={16} />
          </button>
        </div>
      ))}
    </div>
  );
}
