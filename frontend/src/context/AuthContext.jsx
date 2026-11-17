import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, setToken, setUnauthorizedHandler } from '../lib/api';

// The signed-in user. Kept in sessionStorage (per browser tab; cleared when the
// tab closes) rather than localStorage, so a token does not linger on a shared
// hospital computer.
const STORE = 'medicare.session';
const EXPIRED = 'Your session has expired. Please sign in again.';

const AuthContext = createContext(null);
export const useAuth = () => useContext(AuthContext);

// A JWT is three base64 parts; the middle one holds "exp" (expiry, in seconds).
function tokenExpiry(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    return payload.exp ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

function restore() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(STORE));
    if (!saved || !saved.token) return null;
    const exp = tokenExpiry(saved.token);
    if (exp && exp <= Date.now()) {
      sessionStorage.removeItem(STORE);
      return null;
    }
    setToken(saved.token);
    return saved;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(restore);
  const [notice, setNotice] = useState('');

  const logout = useCallback((reason = '') => {
    sessionStorage.removeItem(STORE);
    setToken(null);
    setSession(null);
    setNotice(reason);
  }, []);

  // Any API call answered with 401 (expired/invalid token) ends the session.
  useEffect(() => {
    setUnauthorizedHandler(() => logout(EXPIRED));
  }, [logout]);

  // Log out by itself the moment the token expires, instead of waiting for the
  // first request to fail.
  useEffect(() => {
    if (!session) return undefined;
    const exp = tokenExpiry(session.token);
    if (!exp) return undefined;
    const remaining = exp - Date.now();
    if (remaining <= 0) {
      logout(EXPIRED);
      return undefined;
    }
    const timer = setTimeout(() => logout(EXPIRED), Math.min(remaining, 2147483000));
    return () => clearTimeout(timer);
  }, [session, logout]);

  // After login/registration: remember the token and (for doctors and patients)
  // load their own profile, which several screens need (e.g. "my doctor id").
  const establish = useCallback(async ({ token, username, role }) => {
    setToken(token);
    let profile = null;
    try {
      if (role === 'DOCTOR') profile = await api.get('/api/doctors/me');
      else if (role === 'PATIENT') profile = await api.get('/api/patients/me');
    } catch {
      profile = null;
    }
    const next = { token, username, role, profile };
    sessionStorage.setItem(STORE, JSON.stringify(next));
    setNotice('');
    setSession(next);
  }, []);

  const login = useCallback(async (username, password) => {
    await establish(await api.login({ username, password }));
  }, [establish]);

  const register = useCallback(async (payload) => {
    await establish(await api.register(payload));
  }, [establish]);

  const value = useMemo(
    () => ({ session, notice, login, register, logout }),
    [session, notice, login, register, logout],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
