import { useMemo, useState } from 'react';
import { Upload } from 'lucide-react';
import { api } from '../lib/api';
import { csvToObjects } from '../lib/csv';
import { Modal } from './Modal';
import { Alert, Button } from './ui';

// Import many records at once from pasted CSV / JSON or an uploaded file.
// The backend (POST .../bulk) is all-or-nothing and reports EVERY bad row at
// once as "[row].field", which is shown here as "Row 3 — phone: ...".
export default function BulkImportModal({ title, noun, endpoint, columns, sample, numeric = [], onDone, onClose }) {
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [rowErrors, setRowErrors] = useState([]);

  const parsed = useMemo(() => {
    const t = text.trim();
    if (!t) return { items: [], problem: '' };
    try {
      const items = t.startsWith('[') ? JSON.parse(t) : csvToObjects(t, { numeric });
      if (!Array.isArray(items)) return { items: [], problem: 'Expected a list of records.' };
      return { items, problem: '' };
    } catch {
      return { items: [], problem: 'That is not valid JSON. Check brackets, commas and quotes.' };
    }
  }, [text, numeric]);

  const readFile = (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => setText(String(reader.result || ''));
    reader.readAsText(file);
  };

  const submit = async () => {
    setBusy(true);
    setError('');
    setRowErrors([]);
    try {
      const created = await api.post(endpoint, parsed.items);
      onDone(Array.isArray(created) ? created.length : parsed.items.length);
      onClose();
    } catch (err) {
      setError(err.message);
      const list = Object.entries(err.fieldErrors || {}).map(([key, message]) => {
        const m = /^\[(\d+)\]\.?(.*)$/.exec(key);
        return m ? { row: Number(m[1]) + 1, field: m[2], message } : { row: null, field: key, message };
      });
      setRowErrors(list);
      setBusy(false);
    }
  };

  return (
    <Modal
      title={title} size="lg" onClose={onClose} dismissable={!busy}
      subtitle={`Add many ${noun} in one go. It is all-or-nothing: if any row has a problem, nothing is saved.`}
      footer={(
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>Cancel</Button>
          <Button icon={Upload} loading={busy} disabled={parsed.items.length === 0 || Boolean(parsed.problem)} onClick={submit}>
            Import {parsed.items.length > 0 ? `${parsed.items.length} ${parsed.items.length === 1 ? 'row' : 'rows'}` : ''}
          </Button>
        </>
      )}
    >
      <p className="bulk-help">
        Paste <b>CSV</b> (first line = column names) or a <b>JSON</b> array, or choose a file. Up to 100 rows.
        Columns: <code>{columns.join(', ')}</code>
      </p>
      <div className="bulk-tools">
        <label className="btn btn-secondary btn-sm file-btn">
          Choose file…
          <input type="file" accept=".csv,.json,.txt,text/csv,application/json" onChange={readFile} hidden />
        </label>
        <button type="button" className="link-btn" onClick={() => setText(sample)}>Fill with an example</button>
        {text && <button type="button" className="link-btn" onClick={() => setText('')}>Clear</button>}
      </div>
      <textarea
        className="bulk-text" rows={9} value={text} spellCheck={false} aria-label="Data to import"
        placeholder={columns.join(',')} onChange={(e) => { setText(e.target.value); setRowErrors([]); setError(''); }}
      />
      {parsed.problem && <Alert>{parsed.problem}</Alert>}
      {!parsed.problem && parsed.items.length > 0 && (
        <p className="bulk-count">{parsed.items.length} {parsed.items.length === 1 ? 'row' : 'rows'} ready to import.</p>
      )}
      {error && (
        <Alert>
          {error}
          {rowErrors.length > 0 && (
            <ul className="alert-list">
              {rowErrors.slice(0, 40).map((r, i) => (
                <li key={i}>{r.row ? <b>Row {r.row}</b> : null}{r.field ? <> — <code>{r.field}</code></> : null} {r.message}</li>
              ))}
              {rowErrors.length > 40 && <li>…and {rowErrors.length - 40} more.</li>}
            </ul>
          )}
        </Alert>
      )}
    </Modal>
  );
}
