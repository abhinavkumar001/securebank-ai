import { useEffect, useState } from "react";
import Icon from "./Icon.jsx";
import Assistant from "./Assistant.jsx";
import { initials } from "../api.js";

const THEME_KEY = "securebank.theme";

export default function Shell({ nav, current, counts, session, onNavigate, onSignOut, search, onSearch, onBell, children }) {
  const isAdmin = session.role === "ADMIN";
  const [assistantOpen, setAssistantOpen] = useState(false);
  const [query, setQuery] = useState(search);
  const [theme, setTheme] = useState(() => {
    try { return localStorage.getItem(THEME_KEY); } catch { return null; }
  });

  useEffect(() => {
    if (theme) document.documentElement.dataset.theme = theme;
    else delete document.documentElement.dataset.theme;
  }, [theme]);

  const toggleTheme = () => {
    const dark = theme ? theme === "dark" : window.matchMedia("(prefers-color-scheme: dark)").matches;
    const next = dark ? "light" : "dark";
    setTheme(next);
    try { localStorage.setItem(THEME_KEY, next); } catch { /* storage unavailable */ }
  };

  const currentLabel = nav.flatMap((g) => g.items).find((i) => i.id === current)?.label ?? "";

  return (
    <div className="app">
      <aside className="side">
        <div className="brand">
          <span className="mark">S</span>
          <span><b>SecureBank</b><small>{isAdmin ? "Operations desk" : "Personal banking"}</small></span>
        </div>
        <nav aria-label="Main">
          {nav.map((group, gi) => (
            <div className={group.title ? "group" : ""} key={gi}>
              {group.title && <p>{group.title}</p>}
              {group.items.map((item) => (
                <button key={item.id} className={`nav ${item.id === current ? "on" : ""}`}
                        aria-current={item.id === current ? "page" : undefined}
                        onClick={() => onNavigate(item.id)}>
                  <Icon name={item.icon} />{item.label}
                  {item.countKey && counts[item.countKey] > 0 && <span className="count">{counts[item.countKey]}</span>}
                </button>
              ))}
            </div>
          ))}
        </nav>
        <div className="side-foot">
          <div className="who"><span className="avatar">{initials(session.name)}</span>
            <span><b>{session.name}</b><small>{isAdmin ? "Administrator" : "Customer"}</small></span></div>
          <button className="nav" onClick={onSignOut}><Icon name="logout" />Sign out</button>
        </div>
      </aside>

      <div className="main">
        <header className="top">
          <div className="crumb"><b>{currentLabel}</b></div>
          <form className="search" role="search" onSubmit={(e) => { e.preventDefault(); onSearch(query.trim()); }}>
            <Icon name="search" />
            <input value={query} onChange={(e) => setQuery(e.target.value)}
                   placeholder={isAdmin ? "Search accounts or customer ID…" : "Search transactions, notes, references…"}
                   aria-label="Search" />
          </form>
          <div className="tools">
            <button className="icon-btn" onClick={toggleTheme} aria-label="Switch light or dark mode"><Icon name="moon" /></button>
            <button className="icon-btn" onClick={onBell}
                    aria-label={isAdmin ? "Items needing attention" : "Transfers under review"}>
              <Icon name="bell" />{counts.bell > 0 && <span className="dot">{counts.bell}</span>}
            </button>
            {!isAdmin && (
              <button className="icon-btn" onClick={() => setAssistantOpen((o) => !o)} aria-label="Open SecureBank Assist">
                <Icon name="chat" />
              </button>
            )}
            <span className="avatar">{initials(session.name)}</span>
          </div>
        </header>

        <div className={`body ${assistantOpen ? "" : "no-assist"}`}>
          <div className="page">{children}</div>
          {!isAdmin && assistantOpen && <Assistant session={session} onClose={() => setAssistantOpen(false)} />}
          {!isAdmin && !assistantOpen && (
            <button className="fab" onClick={() => setAssistantOpen(true)}><Icon name="chat" />Ask SecureBank Assist</button>
          )}
        </div>
      </div>
    </div>
  );
}
