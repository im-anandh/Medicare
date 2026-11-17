import { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import { Alert, Button } from './ui';

export function Modal({ title, subtitle, onClose, children, footer, size = 'md', dismissable = true }) {
  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape' && dismissable) onClose(); };
    document.addEventListener('keydown', onKey);
    document.body.classList.add('modal-open');
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.classList.remove('modal-open');
    };
  }, [onClose, dismissable]);

  return createPortal(
    <div
      className="overlay"
      // mousedown (not click): dragging to select text inside a field and
      // releasing outside the dialog must not close it.
      onMouseDown={(e) => { if (e.target === e.currentTarget && dismissable) onClose(); }}
    >
      <div className={`modal modal-${size}`} role="dialog" aria-modal="true" aria-label={title}>
        <header className="modal-head">
          <div>
            <h2>{title}</h2>
            {subtitle && <p>{subtitle}</p>}
          </div>
          <button type="button" className="icon-button" aria-label="Close" onClick={onClose}><X size={18} /></button>
        </header>
        <div className="modal-body">{children}</div>
        {footer && <footer className="modal-foot">{footer}</footer>}
      </div>
    </div>,
    document.body,
  );
}

// "Are you sure?" for actions that are hard to undo. onConfirm may be async;
// if it throws, the message is shown here and the dialog stays open.
export function ConfirmDialog({ title, message, confirmLabel = 'Confirm', danger = false, onConfirm, onClose }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const run = async () => {
    setBusy(true);
    setError('');
    try {
      await onConfirm();
      onClose();
    } catch (e) {
      setError(e.message || 'Something went wrong');
      setBusy(false);
    }
  };

  return (
    <Modal
      title={title}
      size="sm"
      onClose={onClose}
      dismissable={!busy}
      footer={(
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>Cancel</Button>
          <Button variant={danger ? 'danger' : 'primary'} onClick={run} loading={busy}>{confirmLabel}</Button>
        </>
      )}
    >
      <p className="confirm-text">{message}</p>
      {error && <Alert>{error}</Alert>}
    </Modal>
  );
}
