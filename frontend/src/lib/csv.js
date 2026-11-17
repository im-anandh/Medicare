// Minimal CSV reader for the bulk-import dialogs. Handles quoted fields
// ("Chennai, TN"), doubled quotes ("" inside quotes) and CRLF line endings.
export function parseCsv(text) {
  const rows = [];
  let row = [];
  let field = '';
  let inQuotes = false;
  for (let i = 0; i < text.length; i += 1) {
    const c = text[i];
    if (inQuotes) {
      if (c === '"' && text[i + 1] === '"') { field += '"'; i += 1; }
      else if (c === '"') inQuotes = false;
      else field += c;
    } else if (c === '"') inQuotes = true;
    else if (c === ',') { row.push(field); field = ''; }
    else if (c === '\n' || c === '\r') {
      if (c === '\r' && text[i + 1] === '\n') i += 1;
      row.push(field); field = '';
      if (row.some((cell) => cell.trim() !== '')) rows.push(row);
      row = [];
    } else field += c;
  }
  row.push(field);
  if (row.some((cell) => cell.trim() !== '')) rows.push(row);
  return rows;
}

// First row = column names. Empty cells are dropped so optional fields stay
// optional. Numeric columns (listed by the caller) become numbers.
export function csvToObjects(text, { numeric = [] } = {}) {
  const rows = parseCsv(text);
  if (rows.length < 2) return [];
  const header = rows[0].map((h) => h.trim());
  return rows.slice(1).map((cells) => {
    const obj = {};
    header.forEach((key, i) => {
      const value = (cells[i] ?? '').trim();
      if (!key || value === '') return;
      obj[key] = numeric.includes(key) ? Number(value) : value;
    });
    return obj;
  });
}
