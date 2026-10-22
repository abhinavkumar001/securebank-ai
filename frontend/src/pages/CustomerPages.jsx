import { useEffect, useMemo, useState } from "react";
import Icon from "../components/Icon.jsx";
import { api, dateTime, dayLabel, initials, masked, money, newKey, timeOnly } from "../api.js";
import { CHART_COLORS, Donut, Empty, Legend, Notice, PageHeader, Pager, StatusBadge, useLoad } from "../ui.jsx";

const IFSC = "SBNK0001024";
const typeLabel = (t) => (t === "SAVINGS" ? "Savings" : "Current");
const AVATAR_TINTS = [
  ["var(--primary-soft)", "var(--primary)"], ["var(--blue-soft)", "var(--blue)"],
  ["var(--credit-soft)", "var(--credit)"], ["var(--held-soft)", "var(--held)"],
];
const tint = (text) => AVATAR_TINTS[[...text].reduce((s, c) => s + c.charCodeAt(0), 0) % AVATAR_TINTS.length];

function describe(t, accountNumber) {
  const outgoing = t.fromAccountNumber === accountNumber;
  const fallback = t.type === "TRANSFER"
    ? (outgoing ? `Transfer to ${t.toAccountNumber}` : `Transfer from ${t.fromAccountNumber}`)
    : t.type === "DEPOSIT" ? "Money added" : "Cash withdrawal";
  return { outgoing, title: t.description || fallback, detail: t.description ? fallback : t.category };
}

function greeting() {
  const h = new Date().getHours();
  return h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
}

/* =============================== HOME =============================== */

export function Home({ session, go, signOut, refreshCounts }) {
  const token = session.token;
  const accounts = useLoad(() => api("/api/accounts", { token }), [token], signOut);
  const cards = useLoad(() => api("/api/cards", { token }), [token], signOut);
  const insights = useLoad(() => api("/api/insights/overview", { token }), [token], signOut);
  const payees = useLoad(() => api("/api/payees", { token }), [token], signOut);

  const [selected, setSelected] = useState(0);
  const [hidden, setHidden] = useState(false);
  const [cardError, setCardError] = useState("");
  const [opening, setOpening] = useState(false);

  const list = accounts.data ?? [];
  const account = list[Math.min(selected, list.length - 1)];
  const card = cards.data?.find((c) => c.accountNumber === account?.accountNumber);

  const recent = useLoad(
    () => (account ? api(`/api/transactions?accountNumber=${account.accountNumber}&size=8`, { token }) : Promise.resolve(null)),
    [account?.accountNumber, token], signOut);

  const toggleFreeze = async () => {
    setCardError("");
    try {
      await api(`/api/cards/${card.id}/freeze`, { method: "PATCH", token, body: { frozen: !card.frozen } });
      cards.reload();
    } catch (e) {
      setCardError(e.message);
    }
  };

  const openFirst = async () => {
    setOpening(true);
    try {
      await api("/api/accounts", { method: "POST", token, body: { type: "SAVINGS" } });
      accounts.reload(); cards.reload(); refreshCounts();
    } finally {
      setOpening(false);
    }
  };

  const groups = useMemo(() => {
    const out = [];
    (recent.data?.content ?? []).forEach((t) => {
      const label = dayLabel(t.createdAt);
      const last = out[out.length - 1];
      if (last?.label === label) last.items.push(t);
      else out.push({ label, items: [t] });
    });
    return out;
  }, [recent.data]);

  const spending = (insights.data?.spending ?? []).map((c, i) => ({
    label: c.category, value: Number(c.amount), color: CHART_COLORS[i % CHART_COLORS.length] }));
  const firstName = session.name.split(" ")[0];

  if (accounts.data && list.length === 0) {
    return (
      <>
        <PageHeader title={`Welcome to SecureBank, ${firstName}`} subtitle="Let’s get your first account open. It takes one click." />
        <div className="acct-card onboarding">
          <h2>Open a savings account</h2>
          <p>3.50% interest a year, a free debit card and instant transfers.</p>
          <div className="acct-actions">
            <button className="btn" onClick={openFirst} disabled={opening}>{opening ? "Opening…" : "Open savings account"}</button>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <div className="head">
        <div className="welcome">
          <h1>{greeting()}, {firstName}</h1>
          {session.previousLoginAt && (
            <small><Icon name="lock" />Last login {dateTime(session.previousLoginAt)}</small>
          )}
        </div>
        <div className="chips">
          <button className="btn btn-quiet" onClick={() => go("statement", { accountNumber: account?.accountNumber })}>
            <Icon name="list" />Statement
          </button>
        </div>
      </div>
      <Notice>{accounts.error}</Notice>

      <div className="hero-row">
        <div className="acct-card">
          <div className="acct-tabs" role="tablist" aria-label="Choose account">
            {list.map((a, i) => (
              <button key={a.accountNumber} role="tab" aria-selected={a === account}
                      className={a === account ? "on" : ""} onClick={() => setSelected(i)}>
                {typeLabel(a.type)} ··{a.accountNumber.slice(-4)}
              </button>
            ))}
          </div>
          <div className="lbl">Available balance
            <button className="eye" onClick={() => setHidden((h) => !h)} aria-label={hidden ? "Show balance" : "Hide balance"}>
              <Icon name={hidden ? "eyeoff" : "eye"} />
            </button>
          </div>
          <div className="acct-bal">{!account ? "—" : hidden ? "₹ • • • • • •" : money(account.balance)}</div>
          <div className="acct-meta">
            <span>Account number<b>{account ? masked(account.accountNumber) : "—"}</b></span>
            <span>IFSC<b>{IFSC}</b></span>
            <span>Interest<b>{account?.type === "SAVINGS" ? "3.50% p.a." : "—"}</b></span>
            {insights.data?.heldCount > 0 && (
              <span>Under review<b className="held-amt">{money(insights.data.heldAmount)}</b></span>
            )}
          </div>
          {account?.status !== "ACTIVE" && account && (
            <p className="acct-warning">This account is {account.status.toLowerCase()}. Raise a support ticket to find out why.</p>
          )}
          <div className="acct-actions">
            <button className="btn" onClick={() => go("move", { mode: "transfer", from: account?.accountNumber })}><Icon name="send" />Send money</button>
            <button className="btn ghost" onClick={() => go("move", { mode: "deposit", from: account?.accountNumber })}><Icon name="plus" />Add money</button>
            <button className="btn ghost" onClick={() => go("accounts")}>Account details</button>
          </div>
        </div>

        <div className="card debit">
          <div className="card-h"><h2>Debit card</h2><button className="link" onClick={() => go("accounts")}>All cards</button></div>
          {card ? (
            <>
              <div className="plastic-wrap">
                <div className={`plastic ${card.frozen ? "frozen" : ""}`}>
                  <div className="top-row"><span><b>SecureBank</b><small>Platinum debit</small></span><span className="chip-ic" /></div>
                  <div className="pan">{card.maskedNumber}</div>
                  <div className="bot-row">
                    <span><small>Card holder</small><span className="holder">{card.holderName}</span></span>
                    <span><small>Valid thru</small><span className="holder">{card.expiry}</span></span>
                    <span className="rupay">RuPay</span>
                  </div>
                  <span className="frozen-tag">CARD FROZEN</span>
                </div>
              </div>
              <div className="card-ctrls">
                <span><b>{card.frozen ? "Card frozen" : "Freeze card"}</b><small>Blocks all card payments instantly</small></span>
                <button className="switch" role="switch" aria-checked={card.frozen} aria-label="Freeze card" onClick={toggleFreeze} />
              </div>
              <Notice>{cardError}</Notice>
            </>
          ) : <Empty>No card for this account.</Empty>}
        </div>
      </div>

      <nav className="quick" aria-label="Quick actions">
        {[
          ["Transfer", "send", () => go("move", { mode: "transfer", from: account?.accountNumber })],
          ["Add money", "in", () => go("move", { mode: "deposit", from: account?.accountNumber })],
          ["Withdraw", "out", () => go("move", { mode: "withdraw", from: account?.accountNumber })],
          ["Payees", "users", () => go("payees")],
          ["Statement", "list", () => go("statement", { accountNumber: account?.accountNumber })],
          ["New account", "plus", () => go("accounts")],
          ["Cards", "card", () => go("accounts")],
          ["Get help", "msg", () => go("help")],
        ].map(([label, icon, onClick]) => (
          <button key={label} onClick={onClick}><span className="qi"><Icon name={icon} size={20} /></span>{label}</button>
        ))}
      </nav>

      <div className="grid g2">
        <div className="card">
          <div className="card-h"><h2>Recent transactions</h2>
            <button className="link" onClick={() => go("statement", { accountNumber: account?.accountNumber })}>View all</button></div>
          {recent.data?.content?.length === 0 && <Empty>No transactions yet. Add money to get started.</Empty>}
          {groups.map((g) => (
            <div key={g.label}>
              <div className="txgroup">{g.label}</div>
              {g.items.map((t) => {
                const d = describe(t, account.accountNumber);
                const [bg, fg] = t.status === "HELD" ? ["var(--held-soft)", "var(--held)"] : tint(d.title);
                return (
                  <div key={t.reference} className={`tx ${t.status === "HELD" ? "held" : ""}`}>
                    <span className="m" style={{ background: bg, color: fg }}>{initials(d.title)}</span>
                    <span><b>{d.title}</b><small>{d.detail} · {timeOnly(t.createdAt)}</small></span>
                    <span className="r">
                      <b className={`num ${d.outgoing ? "down" : "up"}`}>{d.outgoing ? "−" : "+"}{money(t.amount)}</b>
                      <small>{t.status === "SUCCESS" ? t.category : <StatusBadge status={t.status} />}</small>
                    </span>
                  </div>
                );
              })}
            </div>
          ))}
        </div>

        <div className="stack">
          <div className="card">
            <div className="card-h"><h2>Pay again</h2><button className="link" onClick={() => go("payees")}>All payees</button></div>
            <div className="card-b"><div className="payees">
              {(payees.data ?? []).slice(0, 5).map((p) => {
                const [bg, fg] = tint(p.name);
                return (
                  <button key={p.id} className="payee" onClick={() => go("move", { mode: "transfer", to: p.accountNumber, from: account?.accountNumber })}>
                    <span className="pa" style={{ background: bg, color: fg }}>{initials(p.name)}</span>{p.name.split(" ")[0]}
                  </button>
                );
              })}
              <button className="payee add" onClick={() => go("payees")}><span className="pa"><Icon name="plus" /></span>Add payee</button>
            </div></div>
          </div>

          <div className="card">
            <div className="card-h"><h2>{insights.data?.month ?? "This month"}</h2><button className="link" onClick={() => go("statement")}>Details</button></div>
            <div className="card-b">
              <div className="inout">
                <div><small>Money in</small><b className="num up">{money(insights.data?.moneyIn)}</b></div>
                <div><small>Money out</small><b className="num down">{money(insights.data?.moneyOut)}</b></div>
              </div>
              {spending.length > 0 ? (
                <div className="donut-wrap">
                  <Donut parts={spending} size={104} label="Spending by category" />
                  <Legend parts={spending} format={(v) => money(v)} />
                </div>
              ) : <Empty>Spending by category appears after your first payment.</Empty>}
              {insights.data?.tip && <div className="insight"><Icon name="sparkle" /><span>{insights.data.tip}</span></div>}
            </div>
          </div>
        </div>
      </div>

      <p className="safety"><Icon name="shield" />SecureBank will never ask for your OTP, PIN or password by call, SMS or email.</p>
    </>
  );
}

/* =============================== MOVE MONEY =============================== */

const MODES = [
  { id: "transfer", label: "Transfer", verb: "Send" },
  { id: "deposit", label: "Add money", verb: "Add" },
  { id: "withdraw", label: "Withdraw", verb: "Withdraw" },
];

export function MoveMoney({ session, signOut, params, refreshCounts }) {
  const token = session.token;
  const accounts = useLoad(() => api("/api/accounts", { token }), [token], signOut);
  const payees = useLoad(() => api("/api/payees", { token }), [token], signOut);

  const [mode, setMode] = useState(params.mode || "transfer");
  const [from, setFrom] = useState(params.from || "");
  const [to, setTo] = useState(params.to || "");
  const [manual, setManual] = useState(false);
  const [saveAs, setSaveAs] = useState("");
  const [amount, setAmount] = useState("");
  const [note, setNote] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const list = accounts.data ?? [];
  const source = list.find((a) => a.accountNumber === from) ?? list[0];
  const payeeList = payees.data ?? [];
  const ownOthers = list.filter((a) => a.accountNumber !== source?.accountNumber);
  const toIsPayee = payeeList.some((p) => p.accountNumber === to);
  const toIsOwn = ownOthers.some((a) => a.accountNumber === to);
  const showManual = manual || (to && !toIsPayee && !toIsOwn) || (payeeList.length === 0 && ownOthers.length === 0);

  useEffect(() => { setResult(null); setError(""); }, [mode]);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError(""); setResult(null);
    const body = mode === "transfer"
      ? { fromAccountNumber: source.accountNumber, toAccountNumber: to.trim().toUpperCase(), amount: Number(amount), description: note || null }
      : { accountNumber: source.accountNumber, amount: Number(amount), description: note || null };
    try {
      const tx = await api(`/api/transactions/${mode}`, { method: "POST", token, body, idempotencyKey: newKey() });
      if (mode === "transfer" && saveAs.trim() && !toIsPayee && !toIsOwn) {
        await api("/api/payees", { method: "POST", token, body: { name: saveAs.trim(), accountNumber: body.toAccountNumber } })
          .then(payees.reload).catch(() => {});
      }
      setResult(tx);
      setAmount(""); setNote(""); setSaveAs("");
      accounts.reload(); refreshCounts();
    } catch (err) {
      if (err.status === 401) signOut();
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  const verb = MODES.find((m) => m.id === mode).verb;

  return (
    <>
      <PageHeader title="Move money" subtitle="Transfers of ₹2,00,000 or more, or ones our fraud check finds unusual, are held for a quick security review." />
      <div className="segmented" role="tablist">
        {MODES.map((m) => (
          <button key={m.id} role="tab" aria-selected={mode === m.id} className={mode === m.id ? "on" : ""} onClick={() => setMode(m.id)}>{m.label}</button>
        ))}
      </div>

      {accounts.data && list.length === 0 ? <Empty>Open an account first, then come back to move money.</Empty> : (
        <div className="grid g2 align-start">
          <form className="card card-b form-grid" onSubmit={submit}>
            <label className="full">
              {mode === "deposit" ? "Into account" : "From account"}
              <select value={source?.accountNumber ?? ""} onChange={(e) => setFrom(e.target.value)}>
                {list.map((a) => <option key={a.accountNumber} value={a.accountNumber}>{typeLabel(a.type)} · {a.accountNumber} · {money(a.balance)}</option>)}
              </select>
            </label>

            {mode === "transfer" && (
              <>
                <label className="full">
                  Pay to
                  <select value={showManual ? "__manual" : to}
                          onChange={(e) => { if (e.target.value === "__manual") { setManual(true); setTo(""); } else { setManual(false); setTo(e.target.value); } }}>
                    {!showManual && !to && <option value="">Choose a payee</option>}
                    {payeeList.length > 0 && <optgroup label="Saved payees">
                      {payeeList.map((p) => <option key={p.id} value={p.accountNumber}>{p.name} · {p.accountNumber}</option>)}
                    </optgroup>}
                    {ownOthers.length > 0 && <optgroup label="Your other accounts">
                      {ownOthers.map((a) => <option key={a.accountNumber} value={a.accountNumber}>{typeLabel(a.type)} · {a.accountNumber}</option>)}
                    </optgroup>}
                    <option value="__manual">Enter an account number…</option>
                  </select>
                </label>
                {showManual && (
                  <>
                    <label>Account number
                      <input value={to} onChange={(e) => setTo(e.target.value)} required placeholder="SB0000000000" />
                    </label>
                    <label>Save as payee (optional)
                      <input value={saveAs} onChange={(e) => setSaveAs(e.target.value)} maxLength={60} placeholder="e.g. Rahul" />
                    </label>
                  </>
                )}
              </>
            )}

            <label>
              Amount (₹)
              <input type="number" min="0.01" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required />
              {mode !== "deposit" && source && <small className="hint">Available: {money(source.balance)}</small>}
            </label>
            <label>
              Note
              <input value={note} onChange={(e) => setNote(e.target.value)} maxLength={200}
                     placeholder={mode === "deposit" ? "Salary, refund…" : "Rent, groceries, dinner…"} />
              <small className="hint">Used to categorise your spending.</small>
            </label>

            <div className="form-actions full">
              <button className="btn btn-primary" disabled={busy || !source || (mode === "transfer" && !to)}>
                {busy ? "Processing…" : `${verb} ${amount ? money(amount) : "money"}`}
              </button>
            </div>
            <Notice>{error}</Notice>
          </form>

          <div>
            {result ? <Receipt tx={result} /> : (
              <div className="card card-b side-help">
                <h2>How we keep transfers safe</h2>
                <ul>
                  <li><Icon name="lock" />Both accounts are locked while the money moves, so balances can’t go wrong.</li>
                  <li><Icon name="shield" />A fraud model checks each transfer against your usual activity.</li>
                  <li><Icon name="check" />If your connection drops and you retry, money never moves twice.</li>
                </ul>
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}

function Receipt({ tx }) {
  if (tx.status === "HELD") {
    return (
      <div className="receipt receipt-held" role="status">
        <h3>Transfer held for review</h3>
        <p>{money(tx.amount)} hasn’t left your account. A reviewer will check it shortly, usually within 2 working hours.</p>
        <ul>{tx.holdReasons.map((r) => <li key={r}>{r}</li>)}</ul>
        <small>Reference {tx.reference}</small>
      </div>
    );
  }
  const verb = { DEPOSIT: "added", WITHDRAWAL: "withdrawn", TRANSFER: "sent" }[tx.type];
  return (
    <div className="receipt" role="status">
      <h3>{money(tx.amount)} {verb}</h3>
      <p>Category: {tx.category}{tx.type === "TRANSFER" && ` · fraud risk ${Math.round(tx.riskScore * 100)}%`}</p>
      <small>Reference {tx.reference}</small>
    </div>
  );
}

/* =============================== STATEMENT =============================== */

export function Statement({ session, signOut, params, search }) {
  const token = session.token;
  const accounts = useLoad(() => api("/api/accounts", { token }), [token], signOut);
  const [chosen, setChosen] = useState(params.accountNumber || "");
  const [page, setPage] = useState(0);
  const [filter, setFilter] = useState(search || "");

  useEffect(() => setFilter(search || ""), [search]);

  const accountNumber = chosen || accounts.data?.[0]?.accountNumber || "";
  const { data, error } = useLoad(
    () => (accountNumber ? api(`/api/transactions?accountNumber=${accountNumber}&page=${page}&size=20`, { token }) : Promise.resolve(null)),
    [accountNumber, page, token], signOut);

  const rows = (data?.content ?? []).filter((t) => {
    if (!filter) return true;
    const q = filter.toLowerCase();
    return [t.description, t.reference, t.category, t.fromAccountNumber, t.toAccountNumber, t.status]
      .some((v) => v && v.toLowerCase().includes(q));
  });

  return (
    <>
      <PageHeader title="Statement">
        <select className="compact" value={accountNumber} aria-label="Account"
                onChange={(e) => { setChosen(e.target.value); setPage(0); }}>
          {accounts.data?.map((a) => <option key={a.accountNumber} value={a.accountNumber}>{typeLabel(a.type)} · {a.accountNumber}</option>)}
        </select>
        <input className="compact" type="search" value={filter} onChange={(e) => setFilter(e.target.value)}
               placeholder="Filter this page" aria-label="Filter transactions" />
      </PageHeader>
      <Notice>{error}</Notice>

      <div className="card table-scroll">
        <table>
          <thead><tr><th>Date</th><th>Details</th><th>Category</th><th>Reference</th><th>Status</th><th className="r">Amount</th></tr></thead>
          <tbody>
            {rows.map((t) => {
              const d = describe(t, accountNumber);
              return (
                <tr key={t.reference} className={t.status === "HELD" ? "held-row" : ""}>
                  <td>{dateTime(t.createdAt)}</td>
                  <td><b>{d.title}</b>{t.description && <small className="cell-note">{d.detail}</small>}</td>
                  <td>{t.category}</td>
                  <td className="mono ref">{t.reference.slice(0, 8)}</td>
                  <td><StatusBadge status={t.status} /></td>
                  <td className={`r num ${d.outgoing ? "down" : "up"}`}>{d.outgoing ? "−" : "+"}{money(t.amount)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
        {data && rows.length === 0 && <Empty>{filter ? `Nothing on this page matches “${filter}”.` : "No transactions on this account yet."}</Empty>}
        {accounts.data?.length === 0 && <Empty>Open an account to see its statement.</Empty>}
      </div>
      <Pager page={page} totalPages={data?.totalPages} onChange={setPage} />
    </>
  );
}

/* =============================== PAYEES =============================== */

export function Payees({ session, signOut, go }) {
  const token = session.token;
  const payees = useLoad(() => api("/api/payees", { token }), [token], signOut);
  const [form, setForm] = useState({ name: "", accountNumber: "" });
  const [msg, setMsg] = useState({ kind: "", text: "" });

  const add = async (e) => {
    e.preventDefault();
    try {
      const p = await api("/api/payees", { method: "POST", token, body: form });
      setMsg({ kind: "success", text: `${p.name} saved.` });
      setForm({ name: "", accountNumber: "" });
      payees.reload();
    } catch (err) {
      setMsg({ kind: "error", text: err.message });
    }
  };

  const remove = async (p) => {
    if (!window.confirm(`Remove ${p.name} from your payees?`)) return;
    try {
      await api(`/api/payees/${p.id}`, { method: "DELETE", token });
      payees.reload();
    } catch (err) {
      setMsg({ kind: "error", text: err.message });
    }
  };

  return (
    <>
      <PageHeader title="Payees" subtitle="Save people you pay often. They appear under Pay again on your home screen." />
      <form className="card card-b inline-form" onSubmit={add}>
        <label>Name<input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required maxLength={60} /></label>
        <label>SecureBank account number<input value={form.accountNumber} onChange={(e) => setForm({ ...form, accountNumber: e.target.value })} required placeholder="SB0000000000" /></label>
        <button className="btn btn-primary"><Icon name="plus" />Save payee</button>
      </form>
      {msg.text && <Notice kind={msg.kind}>{msg.text}</Notice>}

      <div className="card">
        {payees.data?.length === 0 && <Empty>No payees yet. Add someone above.</Empty>}
        <ul className="feed">
          {payees.data?.map((p) => {
            const [bg, fg] = tint(p.name);
            return (
              <li key={p.id}>
                <span className="fi round" style={{ background: bg, color: fg }}>{initials(p.name)}</span>
                <span><b>{p.name}</b><small className="mono">{p.accountNumber}</small></span>
                <span className="act">
                  <button className="btn btn-primary" onClick={() => go("move", { mode: "transfer", to: p.accountNumber })}>Pay</button>
                  <button className="btn btn-quiet" onClick={() => remove(p)}>Remove</button>
                </span>
              </li>
            );
          })}
        </ul>
      </div>
    </>
  );
}

/* =============================== ACCOUNTS & CARDS =============================== */

export function Accounts({ session, signOut, go, refreshCounts }) {
  const token = session.token;
  const accounts = useLoad(() => api("/api/accounts", { token }), [token], signOut);
  const cards = useLoad(() => api("/api/cards", { token }), [token], signOut);
  const [type, setType] = useState("SAVINGS");
  const [msg, setMsg] = useState({ kind: "", text: "" });

  const open = async (e) => {
    e.preventDefault();
    try {
      const a = await api("/api/accounts", { method: "POST", token, body: { type } });
      setMsg({ kind: "success", text: `${typeLabel(a.type)} account ${a.accountNumber} is open, and its debit card is ready.` });
      accounts.reload(); cards.reload(); refreshCounts();
    } catch (err) {
      setMsg({ kind: "error", text: err.message });
    }
  };

  const freeze = async (c) => {
    try {
      await api(`/api/cards/${c.id}/freeze`, { method: "PATCH", token, body: { frozen: !c.frozen } });
      cards.reload();
    } catch (err) {
      setMsg({ kind: "error", text: err.message });
    }
  };

  return (
    <>
      <PageHeader title="Accounts & cards" subtitle={`Up to five accounts, each with its own debit card. IFSC for all accounts: ${IFSC}.`} />
      <form className="card card-b inline-form" onSubmit={open}>
        <label>Account type
          <select value={type} onChange={(e) => setType(e.target.value)}>
            <option value="SAVINGS">Savings · 3.50% p.a.</option><option value="CURRENT">Current</option>
          </select>
        </label>
        <button className="btn btn-primary"><Icon name="plus" />Open account</button>
      </form>
      {msg.text && <Notice kind={msg.kind}>{msg.text}</Notice>}

      <div className="card table-scroll" style={{ marginBottom: 14 }}>
        <div className="card-h"><h2>Accounts</h2></div>
        <table>
          <thead><tr><th>Account</th><th>Type</th><th>Status</th><th>Opened</th><th className="r">Balance</th><th /></tr></thead>
          <tbody>
            {accounts.data?.map((a) => (
              <tr key={a.accountNumber}>
                <td className="mono">{a.accountNumber}</td><td>{typeLabel(a.type)}</td>
                <td><StatusBadge status={a.status} /></td><td>{dateTime(a.createdAt)}</td>
                <td className="r num"><b>{money(a.balance)}</b></td>
                <td className="r"><button className="link" onClick={() => go("statement", { accountNumber: a.accountNumber })}>Statement</button></td>
              </tr>
            ))}
          </tbody>
        </table>
        {accounts.data?.length === 0 && <Empty>No accounts yet.</Empty>}
      </div>

      <div className="card">
        <div className="card-h"><h2>Debit cards</h2></div>
        <ul className="feed">
          {cards.data?.map((c) => (
            <li key={c.id}>
              <span className="fi" style={{ background: c.frozen ? "var(--debit-soft)" : "var(--primary-soft)", color: c.frozen ? "var(--debit)" : "var(--primary)" }}><Icon name="card" /></span>
              <span><b className="mono">{c.maskedNumber}</b><small>Linked to {c.accountNumber} · valid thru {c.expiry}</small></span>
              <span className="act"><StatusBadge status={c.frozen ? "FROZEN" : "ACTIVE"} />
                <button className="btn btn-quiet" onClick={() => freeze(c)}>{c.frozen ? "Unfreeze" : "Freeze"}</button></span>
            </li>
          ))}
        </ul>
        {cards.data?.length === 0 && <Empty>Cards are issued automatically when you open an account.</Empty>}
      </div>
    </>
  );
}

/* =============================== HELP / TICKETS =============================== */

export function Help({ session, signOut }) {
  const token = session.token;
  const tickets = useLoad(() => api("/api/tickets", { token }), [token], signOut);
  const [message, setMessage] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true); setError("");
    try {
      setResult(await api("/api/tickets", { method: "POST", token, body: { message } }));
      setMessage("");
      tickets.reload();
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader title="Support" subtitle="Describe the problem in your own words. We route it to the right team automatically, and urgent issues go first." />
      <div className="grid g2 align-start">
        <form className="card card-b" onSubmit={submit}>
          <label>What’s happening?
            <textarea rows={5} value={message} onChange={(e) => setMessage(e.target.value)} minLength={10} maxLength={1000} required
                      placeholder="e.g. My transfer to my landlord has been on hold since morning and rent is due today" />
          </label>
          <div className="form-actions"><button className="btn btn-primary" disabled={busy}>{busy ? "Sending…" : "Raise ticket"}</button></div>
          <Notice>{error}</Notice>
          {result && (
            <div className="receipt" role="status">
              <h3>Ticket #{result.id} raised</h3>
              <p>Routed to <b>{result.categoryLabel}</b> · priority <StatusBadge status={result.urgency} /></p>
              {result.urgency === "URGENT" && <small>Urgent tickets are handled first.</small>}
            </div>
          )}
        </form>

        <div className="card">
          <div className="card-h"><h2>Your tickets</h2></div>
          {tickets.data?.length === 0 && <Empty>No tickets yet.</Empty>}
          <ul className="feed">
            {tickets.data?.map((t) => (
              <li key={t.id}>
                <span className="fi" style={{ background: "var(--blue-soft)", color: "var(--blue)" }}><Icon name="msg" /></span>
                <span><b>{t.categoryLabel}</b><small>{t.message}</small>
                  {t.resolution && <small className="resolution">Reply: {t.resolution}</small>}</span>
                <span className="stack-r"><StatusBadge status={t.status} /><small>{dateTime(t.createdAt)}</small></span>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </>
  );
}
