import { useCallback, useEffect, useState } from "react";
import { api } from "./api.js";
import Shell from "./components/Shell.jsx";
import AuthPage from "./pages/AuthPage.jsx";
import { Home, MoveMoney, Statement, Payees, Accounts, Help } from "./pages/CustomerPages.jsx";
import { Operations, Reviews, Complaints, FraudInsights, AdminAccounts, AuditTrail } from "./pages/AdminPages.jsx";

const SESSION_KEY = "securebank.session";

const CUSTOMER_NAV = [
  { items: [
    { id: "home", label: "Home", icon: "home" },
    { id: "accounts", label: "Accounts & cards", icon: "card" },
    { id: "move", label: "Move money", icon: "send" },
    { id: "statement", label: "Statements", icon: "list" },
    { id: "payees", label: "Payees", icon: "users" },
  ] },
  { title: "Help", items: [{ id: "help", label: "Support tickets", icon: "msg" }] },
];

const ADMIN_NAV = [
  { items: [{ id: "home", label: "Operations", icon: "home" }] },
  { title: "Risk", items: [
    { id: "reviews", label: "Review queue", icon: "shield", countKey: "pendingReviews" },
    { id: "fraud", label: "Fraud model", icon: "chart" },
  ] },
  { title: "Customers", items: [
    { id: "complaints", label: "Complaints", icon: "msg", countKey: "openTickets" },
    { id: "admin-accounts", label: "Accounts", icon: "card" },
  ] },
  { title: "Compliance", items: [{ id: "audit", label: "Audit trail", icon: "list" }] },
];

function loadSession() {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY));
  } catch {
    return null;
  }
}

export default function App() {
  const [session, setSession] = useState(loadSession);
  const [page, setPage] = useState("home");
  const [params, setParams] = useState({});
  const [search, setSearch] = useState("");
  const [counts, setCounts] = useState({});
  const [refreshTick, setRefreshTick] = useState(0);

  const signOut = useCallback(() => {
    localStorage.removeItem(SESSION_KEY);
    setSession(null);
    setPage("home");
  }, []);

  const signIn = (auth) => {
    localStorage.setItem(SESSION_KEY, JSON.stringify(auth));
    setSession(auth);
    setPage("home");
  };

  const go = (id, next = {}) => {
    setParams(next);
    setPage(id);
  };

  /** Pages call this after changing data so the badges in the sidebar and bell stay correct. */
  const refreshCounts = () => setRefreshTick((t) => t + 1);

  const isAdmin = session?.role === "ADMIN";

  useEffect(() => {
    if (!session) return;
    const load = isAdmin
      ? api("/api/admin/stats", { token: session.token }).then((s) => ({
          pendingReviews: s.pendingReviews, openTickets: s.openTickets, bell: s.pendingReviews + s.urgentTickets }))
      : api("/api/insights/overview", { token: session.token }).then((o) => ({ bell: o.heldCount }));
    load.then(setCounts).catch((e) => e.status === 401 && signOut());
  }, [session, isAdmin, page, refreshTick, signOut]);

  if (!session) return <AuthPage onSignedIn={signIn} />;

  const ctx = { session, go, signOut, params, search, refreshCounts };
  const pages = isAdmin
    ? {
        home: <Operations {...ctx} />,
        reviews: <Reviews {...ctx} />,
        fraud: <FraudInsights {...ctx} />,
        complaints: <Complaints {...ctx} />,
        "admin-accounts": <AdminAccounts {...ctx} />,
        audit: <AuditTrail {...ctx} />,
      }
    : {
        home: <Home {...ctx} />,
        accounts: <Accounts {...ctx} />,
        move: <MoveMoney {...ctx} />,
        statement: <Statement {...ctx} />,
        payees: <Payees {...ctx} />,
        help: <Help {...ctx} />,
      };

  const nav = isAdmin ? ADMIN_NAV : CUSTOMER_NAV;
  const current = pages[page] ? page : "home";

  return (
    <Shell
      nav={nav}
      current={current}
      counts={counts}
      session={session}
      onNavigate={go}
      onSignOut={signOut}
      search={search}
      onSearch={(q) => { setSearch(q); go(isAdmin ? "admin-accounts" : "statement"); }}
      onBell={() => go(isAdmin ? "reviews" : "statement")}
    >
      {pages[current]}
    </Shell>
  );
}
