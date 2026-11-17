import { useState } from 'react';
import { HeartPulse, LogOut, Menu } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { labelFor, navFor } from '../lib/access';
import { enumLabel } from '../lib/format';

// The frame around every signed-in screen: sidebar (only the pages this role
// may use), top bar, and the page itself.
export default function Layout({ route, navigate, children }) {
  const { session, logout } = useAuth();
  const [collapsed, setCollapsed] = useState(false);

  const items = navFor(session.role);
  const groups = [...new Set(items.map((i) => i.group))];
  const current = items.find((i) => i.id === route);
  const profile = session.profile;
  const displayName = profile?.fullName || profile?.name || session.username;

  return (
    <div className="shell">
      <aside className={`sidebar ${collapsed ? 'collapsed' : ''}`}>
        <div className="brand">
          <span className="brand-mark"><HeartPulse size={22} /></span>
          <span className="brand-text">MediCare<b>+</b></span>
        </div>

        <nav aria-label="Main">
          {groups.map((group) => (
            <div className="nav-group" key={group}>
              <div className="nav-label">{group.toUpperCase()}</div>
              {items.filter((i) => i.group === group).map((item) => {
                const Icon = item.icon;
                const label = labelFor(item, session.role);
                const selected = item.id === route;
                return (
                  <button
                    key={item.id} type="button" title={label}
                    className={`nav-item ${selected ? 'selected' : ''}`}
                    aria-current={selected ? 'page' : undefined}
                    onClick={() => navigate(item.id)}
                  >
                    <Icon size={18} />
                    <span>{label}</span>
                  </button>
                );
              })}
            </div>
          ))}
        </nav>

        <div className="side-bottom">
          <div className="profile">
            <div className="avatar">{displayName.charAt(0).toUpperCase()}</div>
            <div className="profile-copy">
              <b title={displayName}>{displayName}</b>
              <small>{enumLabel(session.role)}</small>
            </div>
            <button type="button" className="icon-button" aria-label="Sign out" title="Sign out" onClick={() => logout()}>
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </aside>

      <div className="main">
        <header className="topbar">
          <div className="crumb">
            <button type="button" className="icon-button" aria-label="Toggle menu" onClick={() => setCollapsed((c) => !c)}>
              <Menu size={20} />
            </button>
            <span className="crumb-root">MediCare</span>
            <span className="crumb-sep">/</span>
            <strong>{current ? labelFor(current, session.role) : ''}</strong>
          </div>
          <div className="top-actions">
            <span className="role-chip">{enumLabel(session.role)}</span>
            <span className="user-chip">{session.username}</span>
          </div>
        </header>
        <main className="content" id="content">{children}</main>
      </div>
    </div>
  );
}
