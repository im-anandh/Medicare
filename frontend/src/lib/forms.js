// Small helpers for turning form values (always strings) into API payloads.
export const numOrNull = (v) => (v === '' || v === null || v === undefined ? null : Number(v));
export const strOrNull = (v) => {
  const t = typeof v === 'string' ? v.trim() : v;
  return t === '' || t === undefined ? null : t;
};
export const trimmed = (v) => (typeof v === 'string' ? v.trim() : v);
