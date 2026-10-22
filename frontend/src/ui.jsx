import { useCallback, useEffect, useState } from "react";

/** Loads data on mount and when deps change; signs out on an expired token. */
export function useLoad(loader, deps, signOut) {
  const [state, setState] = useState({ data: null, error: null, loading: true });

  const reload = useCallback(async () => {
    setState((s) => ({ ...s, loading: true, error: null }));
    try {
      setState({ data: await loader(), error: null, loading: false });
    } catch (e) {
      if (e.status === 401) signOut();
      setState({ data: null, error: e.message, loading: false });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    reload();
  }, [reload]);

  return { ...state, reload };
}

export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="head">
      <div>
        <h1>{title}</h1>
        {subtitle && <p>{subtitle}</p>}
      </div>
      {children && <div className="chips">{children}</div>}
    </div>
  );
}

const BADGE_KIND = {
  SUCCESS: "ok", ACTIVE: "ok", APPROVED: "ok", RESOLVED: "ok", CALM: "ok", NORMAL: "info",
  HELD: "held", PENDING: "held", HIGH: "held", CONCERNED: "held", OPEN: "info",
  REJECTED: "bad", FROZEN: "bad", CLOSED: "bad", URGENT: "bad", UPSET: "bad",
};

export function StatusBadge({ status, label }) {
  const text = label ?? status.charAt(0) + status.slice(1).toLowerCase();
  return <span className={`badge b-${BADGE_KIND[status] ?? "info"}`}>{text}</span>;
}

export function Notice({ kind = "error", children }) {
  if (!children) return null;
  return <div className={`notice notice-${kind}`} role={kind === "error" ? "alert" : "status"}>{children}</div>;
}

export function Empty({ children }) {
  return <div className="empty">{children}</div>;
}

export function Pager({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <div className="pager">
      <button className="btn btn-quiet" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</button>
      <span>Page {page + 1} of {totalPages}</span>
      <button className="btn btn-quiet" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</button>
    </div>
  );
}

export const CHART_COLORS = ["var(--primary)", "var(--blue)", "var(--held)", "var(--debit)", "var(--credit)", "var(--chart-3)"];

/** SVG donut: parts = [{ label, value, color }] */
export function Donut({ parts, size = 112, label }) {
  const total = parts.reduce((s, p) => s + p.value, 0);
  let offset = 25;
  return (
    <svg width={size} height={size} viewBox="0 0 42 42" role="img" aria-label={label}>
      <circle cx="21" cy="21" r="15.9" fill="none" stroke="var(--sunken)" strokeWidth="6" />
      {total > 0 && parts.map((p) => {
        const pct = (p.value / total) * 100;
        const gap = parts.length > 1 ? 0.8 : 0;
        const el = (
          <circle key={p.label} cx="21" cy="21" r="15.9" fill="none" stroke={p.color} strokeWidth="6"
                  strokeDasharray={`${Math.max(pct - gap, 0)} ${100 - pct + gap}`} strokeDashoffset={offset}>
            <title>{p.label}</title>
          </circle>
        );
        offset -= pct;
        return el;
      })}
    </svg>
  );
}

export function Legend({ parts, format = (v) => v }) {
  return (
    <ul className="legend">
      {parts.map((p) => (
        <li key={p.label}><i className="sw" style={{ background: p.color }} /><span>{p.label}</span><b className="num">{format(p.value)}</b></li>
      ))}
    </ul>
  );
}

export function Meter({ value, color }) {
  return <span className="meter"><i style={{ width: `${Math.round(value * 100)}%`, background: color }} /></span>;
}
