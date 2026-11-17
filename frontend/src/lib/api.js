// The ONE place that talks to the backend. Pages never call fetch() directly,
// so token handling, error shapes and "session expired" behave identically
// everywhere.
//
// All URLs are relative ("/api/..."), i.e. same origin: served by Spring Boot
// in production, forwarded by Vite's proxy in development (no CORS needed).

let authToken = null;
let onUnauthorized = () => {};

export const setToken = (token) => { authToken = token; };
export const setUnauthorizedHandler = (fn) => { onUnauthorized = fn; };

// What every failed call throws. `fieldErrors` mirrors the backend's
// ApiError.fieldErrors, e.g. { phone: "Phone number format is invalid" } or,
// for bulk requests, { "[1].phone": "..." }.
export class ApiError extends Error {
  constructor(status, message, fieldErrors = null) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function request(method, path, body, { auth = true } = {}) {
  const headers = {};
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (auth && authToken) headers.Authorization = `Bearer ${authToken}`;

  let response;
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the MediCare server. Check that the backend is running.');
  }

  const text = await response.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); } catch { data = null; }
  }

  if (!response.ok) {
    // 401 on a normal call = token missing/expired -> back to the login screen.
    // (Login/register pass auth:false, where a failure is just a wrong password.)
    if (response.status === 401 && auth) onUnauthorized();
    const message =
      (data && data.message) ||
      (response.status === 403 ? 'You do not have permission to do that.' : `Request failed (${response.status})`);
    throw new ApiError(response.status, message, (data && data.fieldErrors) || null);
  }
  return data;
}

export const api = {
  get: (path) => request('GET', path),
  post: (path, body = {}) => request('POST', path, body),
  put: (path, body = {}) => request('PUT', path, body),
  patch: (path, body = {}) => request('PATCH', path, body),
  del: (path) => request('DELETE', path),
  login: (credentials) => request('POST', '/api/auth/login', credentials, { auth: false }),
  register: (payload) => request('POST', '/api/auth/register', payload, { auth: false }),
};
