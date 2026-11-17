// Display helpers. Rule: format dates, times, money and enum labels - but NEVER
// rewrite free text (names, emails, diagnoses, SMS bodies) shown to the user.

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const pad = (n) => String(n).padStart(2, '0');

// Dates arrive as "2026-10-22" (LocalDate). Parsed by hand - new Date("2026-10-22")
// is read as UTC midnight and can show the previous day in some time zones.
export function fmtDate(value) {
  if (!value) return '—';
  const [y, m, d] = String(value).slice(0, 10).split('-').map(Number);
  if (!y || !m || !d) return String(value);
  return `${d} ${MONTHS[m - 1]} ${y}`;
}

// "14:30:00" (LocalTime) -> "2:30 PM"
export function fmtTime(value) {
  if (!value) return '—';
  const [h, m] = String(value).split(':').map(Number);
  if (Number.isNaN(h) || Number.isNaN(m)) return String(value);
  return `${h % 12 === 0 ? 12 : h % 12}:${pad(m)} ${h < 12 ? 'AM' : 'PM'}`;
}

// "2026-10-22T10:00:00" (LocalDateTime) -> "22 Oct 2026, 10:00 AM"
export function fmtDateTime(value) {
  if (!value) return '—';
  const [date, time] = String(value).split('T');
  return `${fmtDate(date)}${time ? `, ${fmtTime(time)}` : ''}`;
}

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' });
export const money = (n) => (n === null || n === undefined || n === '' ? '—' : inr.format(Number(n)));

// "SCHEDULED" -> "Scheduled" (for status/role enums only)
export const enumLabel = (v) => (v ? String(v).charAt(0) + String(v).slice(1).toLowerCase().replace(/_/g, ' ') : '—');

// Today as "YYYY-MM-DD" in the USER'S time zone (toISOString would give UTC).
export function todayStr() {
  const d = new Date();
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function ageFrom(dob) {
  if (!dob) return null;
  const [y, m, d] = String(dob).split('-').map(Number);
  const now = new Date();
  let age = now.getFullYear() - y;
  if (now.getMonth() + 1 < m || (now.getMonth() + 1 === m && now.getDate() < d)) age -= 1;
  return age;
}

export const greeting = () => {
  const h = new Date().getHours();
  return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening';
};

export const longDate = () =>
  new Date().toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
