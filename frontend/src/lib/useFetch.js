import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from './api';

// Load a GET endpoint and expose { data, loading, error, reload }.
// Pass null as the path to skip loading (e.g. when a choice isn't made yet).
// A counter guards against an older, slower response overwriting a newer one.
export function useFetch(path) {
  const [state, setState] = useState({ data: null, loading: Boolean(path), error: null });
  const latest = useRef(0);

  const load = useCallback(async () => {
    if (!path) {
      setState({ data: null, loading: false, error: null });
      return;
    }
    latest.current += 1;
    const mine = latest.current;
    setState((s) => ({ ...s, loading: true, error: null }));   // keep old rows visible while refreshing
    try {
      const data = await api.get(path);
      if (mine === latest.current) setState({ data, loading: false, error: null });
    } catch (error) {
      if (mine === latest.current) setState({ data: null, loading: false, error });
    }
  }, [path]);

  useEffect(() => { load(); }, [load]);
  return { ...state, reload: load };
}
