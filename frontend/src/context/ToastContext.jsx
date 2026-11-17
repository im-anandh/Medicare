import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { CheckCircle2, CircleAlert, X } from 'lucide-react';

const ToastContext = createContext(null);
export const useToast = () => useContext(ToastContext);

// Small "it worked / it failed" messages in the corner of the screen.
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const dismiss = useCallback((id) => setToasts((list) => list.filter((t) => t.id !== id)), []);
  const push = useCallback((kind, message) => {
    const id = Date.now() + Math.random();
    setToasts((list) => [...list, { id, kind, message }]);
    setTimeout(() => dismiss(id), kind === 'error' ? 7000 : 4000);
  }, [dismiss]);

  const api = useMemo(() => ({
    success: (message) => push('success', message),
    error: (message) => push('error', message),
  }), [push]);

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className={`toast toast-${t.kind}`} role="status">
            {t.kind === 'success' ? <CheckCircle2 size={18} /> : <CircleAlert size={18} />}
            <span>{t.message}</span>
            <button type="button" className="icon-button" aria-label="Dismiss" onClick={() => dismiss(t.id)}>
              <X size={15} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
