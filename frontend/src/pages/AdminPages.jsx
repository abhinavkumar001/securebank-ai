import { useState } from "react";
import Icon from "../components/Icon.jsx";
import { REASON_LABELS, api, dateTime, money, moneyShort } from "../api.js";
import { CHART_COLORS, Donut, Empty, Legend, Meter, Notice, PageHeader, Pager, StatusBadge, useLoad } from "../ui.jsx";

const ACTION_LABELS = {
  REGISTER: "Customer registered", LOGIN: "Signed in", OPEN_ACCOUNT: "Opened account",
  CHANGE_ACCOUNT_STATUS: "Changed account status", DEPOSIT: "Deposit", WITHDRAWAL: "Withdrawal",
  TRANSFER: "Transfer", TRANSFER_HELD: "Transfer held", TRANSFER_APPROVED: "Transfer approved",
  TRANSFER_REJECTED: "Transfer rejected", CARD_FROZEN: "Card frozen", CARD_UNFROZEN: "Card unfrozen",
  ADD_PAYEE: "Payee added", DELETE_PAYEE: "Payee removed", TICKET_RAISED: "Ticket raised", TICKET_RESOLVED: "Ticket resolved",
};
const ACTION_ICON = {
  TRANSFER_HELD: ["shield", "held"], TRANSFER_REJECTED: ["x", "debit"], TRANSFER_APPROVED: ["check", "credit"],
  CHANGE_ACCOUNT_STATUS: ["lock", "debit"], CARD_FROZEN: ["lock", "debit"], TICKET_RAISED: ["msg", "blue"],
  TICKET_RESOLVED: ["check", "credit"], REGISTER: ["users", "blue"], LOGIN: ["users", "blue"],
};
const riskColor = (r) => (r >= 0.85 ? "var(--debit)" : "var(--held)");

/* ---------- shared: one review item with approve / reject ---------- */

function ReviewItem({ item, token, onDone, compact }) {
  const [note, setNote] = useState("");
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");

  const decide = async (action) => {
    setBusy(action); setError("");
    try {
      await api(`/api/admin/reviews/${item.id}/${action}`, { method: "POST", token, body: { note: note || null } });
      onDone();
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy("");
    }
  };

  return (
    <li>
      <div className="row">
        <span><b className="num">{money(item.amount)}</b>&nbsp; <small className="mono">{item.fromAccountNumber} → {item.toAccountNumber}</small></span>
        <span className="risk">Risk {Math.round(item.riskScore * 100)}%<Meter value={item.riskScore} color={riskColor(item.riskScore)} /></span>
      </div>
      <div className="reasons">{item.reasons.map((r) => <span key={r}>{r}</span>)}</div>
      {!compact && (
        <input className="compact note" value={note} onChange={(e) => setNote(e.target.value)} maxLength={300}
               placeholder="Note for the audit trail (optional)" aria-label="Review note" />
      )}
      <div className="row">
        <small>Customer #{item.userId} · {dateTime(item.createdAt)}</small>
        <span className="act">
          <button className="btn btn-ok" disabled={!!busy} onClick={() => decide("approve")}>{busy === "approve" ? "Approving…" : "Approve"}</button>
          <button className="btn btn-no" disabled={!!busy} onClick={() => decide("reject")}>{busy === "reject" ? "Rejecting…" : "Reject"}</button>
        </span>
      </div>
      <Notice>{error}</Notice>
    </li>
  );
}

/* =============================== OPERATIONS =============================== */

export function Operations({ session, go, signOut, refreshCounts }) {
  const token = session.token;
  const stats = useLoad(() => api("/api/admin/stats", { token }), [token], signOut);
  const reviews = useLoad(() => api("/api/admin/reviews", { token }), [token], signOut);
  const tickets = useLoad(() => api("/api/admin/tickets", { token }), [token], signOut);
  const audit = useLoad(() => api("/api/admin/audit-logs?size=8", { token }), [token], signOut);

  const s = stats.data;
  const reasons = Object.entries(s?.flaggedReasons30d ?? {}).map(([code, n], i) => ({
    label: REASON_LABELS[code] ?? code, value: n, color: CHART_COLORS[(i + 3) % CHART_COLORS.length] }));
  const totalFlagged = reasons.reduce((a, r) => a + r.value, 0);

  const afterReview = () => { reviews.reload(); stats.reload(); audit.reload(); refreshCounts(); };

  return (
    <>
      <PageHeader title="Operations" subtitle="Risk, customers and compliance across the bank.">
        <span className="chip"><Icon name="clock" />{new Date().toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" })}</span>
      </PageHeader>
      <Notice>{stats.error}</Notice>

      <div className="kpis">
        <div className="kpi"><span>Deposits</span><strong>{moneyShort(s?.totalDeposits)}</strong><small>{s?.accounts ?? 0} accounts · {s?.frozenAccounts ?? 0} frozen</small>
          <i className="ic t-primary"><Icon name="wallet" /></i></div>
        <div className="kpi"><span>Transactions today</span><strong>{s?.transactionsToday ?? 0}</strong><small>All channels</small>
          <i className="ic t-blue"><Icon name="chart" /></i></div>
        <div className={`kpi ${s?.pendingReviews ? "attn" : ""}`}><span>Held for review</span><strong>{s?.pendingReviews ?? 0}</strong>
          <small>{moneyShort(s?.pendingAmount)} waiting</small><i className="ic t-held"><Icon name="shield" /></i></div>
        <div className="kpi"><span>Open complaints</span><strong>{s?.openTickets ?? 0}</strong>
          <small className={s?.urgentTickets ? "down" : ""}>{s?.urgentTickets ?? 0} urgent</small><i className="ic t-debit"><Icon name="msg" /></i></div>
      </div>

      <div className="grid g2">
        <div className="card">
          <div className="card-h"><h2>Review queue</h2><button className="link" onClick={() => go("reviews")}>Open queue</button></div>
          {reviews.data?.length === 0 && <Empty>Nothing waiting. Transfers flagged by the fraud check appear here.</Empty>}
          <ul className="queue">
            {reviews.data?.slice(0, 3).map((r) => <ReviewItem key={r.id} item={r} token={token} onDone={afterReview} compact />)}
          </ul>
        </div>
        <div className="card">
          <div className="card-h"><h2>Why transfers were flagged</h2><small>Last 30 days</small></div>
          <div className="card-b">
            {reasons.length === 0 ? <Empty>No flagged transfers in the last 30 days.</Empty> : (
              <div className="donut-wrap">
                <Donut parts={reasons} size={116} label="Reasons for flagged transfers" />
                <div style={{ flex: 1 }}>
                  <p className="big">{totalFlagged} <small>reasons recorded</small></p>
                  <p><span className="up">{s.approved30d} approved</span> · <span className="down">{s.rejected30d} rejected</span></p>
                  <Legend parts={reasons} />
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="grid g2">
        <div className="card table-scroll">
          <div className="card-h"><h2>Complaints by priority</h2><button className="link" onClick={() => go("complaints")}>All complaints</button></div>
          <table>
            <thead><tr><th>Customer</th><th>Message</th><th>Routed to</th><th>Priority</th></tr></thead>
            <tbody>
              {tickets.data?.slice(0, 5).map((t) => (
                <tr key={t.id}><td>#{t.userId}</td><td className="clip-sm" title={t.message}>{t.message}</td>
                  <td><span className="badge b-info">{t.categoryLabel}</span></td><td><StatusBadge status={t.urgency} /></td></tr>
              ))}
            </tbody>
          </table>
          {tickets.data?.length === 0 && <Empty>No open complaints.</Empty>}
        </div>
        <div className="card">
          <div className="card-h"><h2>Audit trail</h2><button className="link" onClick={() => go("audit")}>View all</button></div>
          <AuditFeed items={audit.data?.content ?? []} />
        </div>
      </div>
    </>
  );
}

function AuditFeed({ items }) {
  return (
    <ul className="feed">
      {items.map((l) => {
        const [icon, tone] = ACTION_ICON[l.action] ?? ["list", "primary"];
        return (
          <li key={l.id}>
            <span className={`fi t-${tone}`}><Icon name={icon} /></span>
            <span><b>{ACTION_LABELS[l.action] ?? l.action}</b><small className="clip">{l.userId ? `#${l.userId} · ` : ""}{l.details}</small></span>
            <small>{new Date(l.createdAt).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" })}</small>
          </li>
        );
      })}
    </ul>
  );
}

/* =============================== REVIEW QUEUE =============================== */

export function Reviews({ session, signOut, refreshCounts }) {
  const token = session.token;
  const [tab, setTab] = useState("pending");
  const pending = useLoad(() => api("/api/admin/reviews", { token }), [token], signOut);
  const history = useLoad(() => api("/api/admin/reviews/history", { token }), [token], signOut);

  return (
    <>
      <PageHeader title="Review queue" subtitle="Held transfers, riskiest first. Approving re-checks the balance and moves the money; rejecting keeps it in the customer’s account." />
      <div className="segmented" role="tablist">
        <button role="tab" aria-selected={tab === "pending"} className={tab === "pending" ? "on" : ""} onClick={() => setTab("pending")}>
          Waiting{pending.data ? ` (${pending.data.length})` : ""}</button>
        <button role="tab" aria-selected={tab === "history"} className={tab === "history" ? "on" : ""} onClick={() => setTab("history")}>Decided</button>
      </div>
      <Notice>{pending.error || history.error}</Notice>

      {tab === "pending" ? (
        <div className="card">
          {pending.data?.length === 0 && <Empty>The queue is clear.</Empty>}
          <ul className="queue">
            {pending.data?.map((r) => (
              <ReviewItem key={r.id} item={r} token={token} onDone={() => { pending.reload(); history.reload(); refreshCounts(); }} />
            ))}
          </ul>
        </div>
      ) : (
        <div className="card table-scroll">
          <table>
            <thead><tr><th>Decided</th><th>Transfer</th><th>Risk</th><th>Reasons</th><th>Outcome</th><th>Note</th></tr></thead>
            <tbody>
              {history.data?.map((r) => (
                <tr key={r.id}>
                  <td>{dateTime(r.reviewedAt)}</td>
                  <td><b className="num">{money(r.amount)}</b><small className="cell-note mono">{r.fromAccountNumber} → {r.toAccountNumber}</small></td>
                  <td className="num">{Math.round(r.riskScore * 100)}%</td>
                  <td className="clip">{r.reasonCodes.map((c) => REASON_LABELS[c] ?? c).join(", ")}</td>
                  <td><StatusBadge status={r.status} /></td>
                  <td className="clip">{r.reviewNote || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {history.data?.length === 0 && <Empty>No decisions yet.</Empty>}
        </div>
      )}
    </>
  );
}

/* =============================== COMPLAINTS =============================== */

export function Complaints({ session, signOut, refreshCounts }) {
  const token = session.token;
  const [tab, setTab] = useState("OPEN");
  const tickets = useLoad(() => api(`/api/admin/tickets?status=${tab}`, { token }), [token, tab], signOut);
  const [replying, setReplying] = useState(null);
  const [reply, setReply] = useState("");
  const [error, setError] = useState("");

  const resolve = async (e) => {
    e.preventDefault();
    try {
      await api(`/api/admin/tickets/${replying}/resolve`, { method: "POST", token, body: { resolution: reply } });
      setReplying(null); setReply(""); tickets.reload(); refreshCounts();
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <>
      <PageHeader title="Complaints" subtitle="Each message is routed by a Naive Bayes classifier and ranked by an urgency and tone score." />
      <div className="segmented" role="tablist">
        {["OPEN", "RESOLVED"].map((t) => (
          <button key={t} role="tab" aria-selected={tab === t} className={tab === t ? "on" : ""} onClick={() => setTab(t)}>
            {t === "OPEN" ? "Open" : "Resolved"}</button>
        ))}
      </div>
      <Notice>{tickets.error || error}</Notice>

      <div className="card table-scroll">
        <table>
          <thead><tr><th>Raised</th><th>Customer</th><th>Message</th><th>Routed to</th><th>Tone</th><th>Priority</th><th /></tr></thead>
          <tbody>
            {tickets.data?.map((t) => (
              <tr key={t.id} className={t.urgency === "URGENT" && t.status === "OPEN" ? "urgent-row" : ""}>
                <td>{dateTime(t.createdAt)}</td>
                <td>#{t.userId}</td>
                <td className="msg-cell">{t.message}
                  {t.resolution && <small className="cell-note">Reply: {t.resolution}</small>}
                  {replying === t.id && (
                    <form className="reply" onSubmit={resolve}>
                      <input className="compact" autoFocus value={reply} onChange={(e) => setReply(e.target.value)} required maxLength={500}
                             placeholder="Reply to the customer" aria-label="Reply" />
                      <button className="btn btn-primary">Send and resolve</button>
                      <button type="button" className="btn btn-quiet" onClick={() => setReplying(null)}>Cancel</button>
                    </form>
                  )}
                </td>
                <td><span className="badge b-info">{t.categoryLabel}</span><small className="cell-note">{Math.round(t.categoryConfidence * 100)}% confident</small></td>
                <td><StatusBadge status={t.tone} /></td>
                <td><StatusBadge status={t.urgency} /></td>
                <td className="r">{t.status === "OPEN" && replying !== t.id && (
                  <button className="link" onClick={() => { setReplying(t.id); setReply(""); setError(""); }}>Reply</button>)}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {tickets.data?.length === 0 && <Empty>{tab === "OPEN" ? "No open complaints." : "Nothing resolved yet."}</Empty>}
      </div>
    </>
  );
}

/* =============================== FRAUD MODEL =============================== */

export function FraudInsights({ session, signOut }) {
  const token = session.token;
  const { data: m, error } = useLoad(() => api("/api/admin/fraud/model", { token }), [token], signOut);
  const weights = Object.entries(m?.featureWeights ?? {});
  const maxW = Math.max(...weights.map(([, w]) => Math.abs(w)), 1);

  return (
    <>
      <PageHeader title="Fraud model" subtitle="Logistic regression written in Java, trained at startup and evaluated on held-out data." />
      <Notice>{error}</Notice>
      <div className="kpis">
        {[["Accuracy", m?.accuracy], ["Precision", m?.precision], ["Recall", m?.recall], ["F1 score", m?.f1]].map(([k, v]) => (
          <div className="kpi" key={k}><span>{k}</span><strong>{v == null ? "—" : `${(v * 100).toFixed(1)}%`}</strong>
            <small>{m ? `${m.testSize} test transfers` : ""}</small></div>
        ))}
      </div>
      <div className="grid g2 align-start">
        <div className="card">
          <div className="card-h"><h2>What the model has learned</h2><small>Weight per feature</small></div>
          <div className="card-b weights">
            {weights.map(([name, w]) => (
              <div key={name}><p><span>{name}</span><b className="num">{w > 0 ? "+" : ""}{w.toFixed(2)}</b></p>
                <Meter value={Math.abs(w) / maxW} color={w > 0 ? "var(--debit)" : "var(--credit)"} /></div>
            ))}
          </div>
        </div>
        <div className="card card-b explain">
          <h2>How a transfer is scored</h2>
          <ol>
            <li>Five features are built from the account’s history: amount versus its usual transfer, new recipient, time of day, transfers in the last 10 minutes and share of balance.</li>
            <li>The model gives a probability of fraud. At {80}% or above the transfer is held. Any transfer of ₹2,00,000 or more is held by rule.</li>
            <li>Each feature’s contribution (weight × value) becomes a reason code, so reviewers see <i>why</i>, not just a score.</li>
            <li>Recall is prioritised over precision: missing fraud costs more than a short review.</li>
          </ol>
          <p className="footnote">Trained on {m?.trainSize ?? "—"} simulated transfers. A production bank would retrain on its own labelled history.</p>
        </div>
      </div>
    </>
  );
}

/* =============================== ACCOUNTS =============================== */

export function AdminAccounts({ session, signOut, search }) {
  const token = session.token;
  const { data, error, reload } = useLoad(() => api("/api/admin/accounts", { token }), [token], signOut);
  const [query, setQuery] = useState(search || "");
  const [editing, setEditing] = useState(null);
  const [msg, setMsg] = useState({ kind: "", text: "" });

  const rows = (data ?? []).filter((a) => !query
    || a.accountNumber.toLowerCase().includes(query.toLowerCase())
    || String(a.userId) === query.trim().replace("#", ""));

  const save = async (e) => {
    e.preventDefault();
    try {
      await api(`/api/admin/accounts/${editing.accountNumber}/status`, {
        method: "PATCH", token, body: { status: editing.status, reason: editing.reason } });
      setMsg({ kind: "success", text: `Account ${editing.accountNumber} is now ${editing.status.toLowerCase()}.` });
      setEditing(null); reload();
    } catch (err) {
      setMsg({ kind: "error", text: err.message });
    }
  };

  return (
    <>
      <PageHeader title="Customer accounts">
        <input className="compact search-box" type="search" value={query} onChange={(e) => setQuery(e.target.value)}
               placeholder="Account number or customer ID" aria-label="Search accounts" />
      </PageHeader>
      <Notice>{error}</Notice>
      {msg.text && <Notice kind={msg.kind}>{msg.text}</Notice>}

      {editing && (
        <form className="card card-b form-grid" onSubmit={save} style={{ marginBottom: 14 }}>
          <h2 className="full">Change status of {editing.accountNumber}</h2>
          <label>New status
            <select value={editing.status} onChange={(e) => setEditing({ ...editing, status: e.target.value })}>
              <option value="ACTIVE">Active</option><option value="FROZEN">Frozen</option><option value="CLOSED">Closed</option>
            </select>
          </label>
          <label>Reason (saved to the audit trail)
            <input value={editing.reason} required maxLength={300} onChange={(e) => setEditing({ ...editing, reason: e.target.value })} />
          </label>
          <div className="form-actions full">
            <button className="btn btn-primary">Save status</button>
            <button type="button" className="btn btn-quiet" onClick={() => setEditing(null)}>Cancel</button>
          </div>
        </form>
      )}

      <div className="card table-scroll">
        <table>
          <thead><tr><th>Account</th><th>Customer</th><th>Type</th><th>Status</th><th>Opened</th><th className="r">Balance</th><th /></tr></thead>
          <tbody>
            {rows.map((a) => (
              <tr key={a.accountNumber}>
                <td className="mono">{a.accountNumber}</td><td>#{a.userId}</td>
                <td>{a.type === "SAVINGS" ? "Savings" : "Current"}</td><td><StatusBadge status={a.status} /></td>
                <td>{dateTime(a.createdAt)}</td><td className="r num"><b>{money(a.balance)}</b></td>
                <td className="r"><button className="link" onClick={() => setEditing({ accountNumber: a.accountNumber, status: a.status, reason: "" })}>Change status</button></td>
              </tr>
            ))}
          </tbody>
        </table>
        {data && rows.length === 0 && <Empty>No accounts match “{query}”.</Empty>}
      </div>
    </>
  );
}

/* =============================== AUDIT =============================== */

export function AuditTrail({ session, signOut }) {
  const token = session.token;
  const [page, setPage] = useState(0);
  const [userId, setUserId] = useState("");
  const [applied, setApplied] = useState("");
  const { data, error } = useLoad(
    () => api(`/api/admin/audit-logs?page=${page}&size=25${applied ? `&userId=${applied}` : ""}`, { token }),
    [page, applied, token], signOut);

  return (
    <>
      <PageHeader title="Audit trail" subtitle="Every sign-in, account change, money movement and review decision, newest first.">
        <form className="filter" onSubmit={(e) => { e.preventDefault(); setPage(0); setApplied(userId.trim()); }}>
          <input className="compact" inputMode="numeric" placeholder="Customer ID" value={userId}
                 onChange={(e) => setUserId(e.target.value)} aria-label="Filter by customer ID" />
          <button className="btn btn-quiet">Filter</button>
        </form>
      </PageHeader>
      <Notice>{error}</Notice>
      <div className="card table-scroll">
        <table>
          <thead><tr><th>When</th><th>User</th><th>Action</th><th>Record</th><th>Details</th></tr></thead>
          <tbody>
            {data?.content?.map((l) => (
              <tr key={l.id} className={l.action === "TRANSFER_HELD" ? "held-row" : ""}>
                <td>{dateTime(l.createdAt)}</td><td>{l.userId ? `#${l.userId}` : "System"}</td>
                <td>{ACTION_LABELS[l.action] ?? l.action}</td>
                <td className="mono ref">{l.entityType} {String(l.entityId ?? "").slice(0, 12)}</td>
                <td className="details">{l.details}</td>
              </tr>
            ))}
          </tbody>
        </table>
        {data?.content?.length === 0 && <Empty>No audit entries{applied ? ` for customer #${applied}` : ""}.</Empty>}
      </div>
      <Pager page={page} totalPages={data?.totalPages} onChange={setPage} />
    </>
  );
}
