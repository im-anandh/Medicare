import { useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight, Search } from 'lucide-react';
import { EmptyState, ErrorState, Spinner } from './ui';

/**
 * Searchable, paged table used by every list screen.
 *   columns:  [{ header, cell: (row) => node, className }]
 *   actions:  (row) => node        buttons shown in the last column
 *   toolbar:  node                  extra controls beside the search box (filters)
 *   rowText:  (row) => string       what the search box matches against
 *             (default: every value in the row)
 */
export function DataTable({
  columns, rows, loading, error, onRetry, rowKey = (r) => r.id, actions, toolbar,
  rowText, emptyTitle = 'Nothing here yet', emptyText, emptyAction, pageSize = 12, searchPlaceholder = 'Search…', searchable = true,
}) {
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q || !rows) return rows || [];
    return rows.filter((r) => (rowText ? rowText(r) : JSON.stringify(r)).toLowerCase().includes(q));
  }, [rows, query, rowText]);

  const pages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const current = Math.min(page, pages - 1);
  const visible = filtered.slice(current * pageSize, current * pageSize + pageSize);
  const from = filtered.length === 0 ? 0 : current * pageSize + 1;

  return (
    <div className="card table-card">
      {(searchable || toolbar) && <div className="table-toolbar">
        {searchable && <label className="search">
          <Search size={16} />
          <input
            type="search" value={query} placeholder={searchPlaceholder} aria-label="Search"
            onChange={(e) => { setQuery(e.target.value); setPage(0); }}
          />
        </label>}
        {toolbar && <div className="table-filters">{toolbar}</div>}
      </div>}

      {loading && !rows ? <Spinner />
        : error ? <ErrorState error={error} onRetry={onRetry} />
        : filtered.length === 0 ? (
          <EmptyState
            title={query ? 'No matches' : emptyTitle}
            text={query ? `Nothing matches “${query}”.` : emptyText}
            action={query ? null : emptyAction}
          />
        ) : (
          <>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    {columns.map((c) => <th key={c.header}>{c.header}</th>)}
                    {actions && <th className="col-actions">Actions</th>}
                  </tr>
                </thead>
                <tbody>
                  {visible.map((row) => (
                    <tr key={rowKey(row)}>
                      {columns.map((c) => <td key={c.header} className={c.className}>{c.cell(row)}</td>)}
                      {actions && <td className="col-actions"><div className="row-actions">{actions(row)}</div></td>}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="table-foot">
              <span>Showing {from}–{from + visible.length - 1 < from ? 0 : from + visible.length - 1} of {filtered.length}</span>
              {pages > 1 && (
                <div className="pager">
                  <button type="button" className="icon-button" aria-label="Previous page" disabled={current === 0} onClick={() => setPage(current - 1)}><ChevronLeft size={16} /></button>
                  <span>Page {current + 1} of {pages}</span>
                  <button type="button" className="icon-button" aria-label="Next page" disabled={current >= pages - 1} onClick={() => setPage(current + 1)}><ChevronRight size={16} /></button>
                </div>
              )}
            </div>
          </>
        )}
    </div>
  );
}

// A compact text button for the Actions column.
export function RowAction({ icon: Icon, label, onClick, danger = false }) {
  return (
    <button type="button" className={`row-action ${danger ? 'danger' : ''}`} onClick={onClick}>
      {Icon && <Icon size={14} />}{label}
    </button>
  );
}
