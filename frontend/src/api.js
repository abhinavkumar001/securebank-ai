const BASE = import.meta.env.VITE_API_URL || "http://localhost:8080";

/** Calls the Spring Boot API and turns error responses into readable messages. */
export async function api(path, { method = "GET", body, token, idempotencyKey } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (idempotencyKey) headers["Idempotency-Key"] = idempotencyKey;

  let res;
  try {
    res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  } catch {
    throw new Error("Can’t reach the server. Check that the backend is running on port 8080.");
  }

  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const message = data?.details?.length
      ? `${data.error}: ${data.details.join(", ")}`
      : data?.error || `Request failed (${res.status})`;
    const error = new Error(message);
    error.status = res.status;
    throw error;
  }
  return data;
}

export const newKey = () => crypto.randomUUID();

const inr = new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" });
export const money = (n) => inr.format(Number(n || 0));

/** ₹1.85 Cr / ₹11.8 L style for dashboards. */
export function moneyShort(n) {
  const v = Number(n || 0);
  if (v >= 1e7) return `₹${(v / 1e7).toFixed(2)} Cr`;
  if (v >= 1e5) return `₹${(v / 1e5).toFixed(1)} L`;
  return inr.format(v).replace(/\.00$/, "");
}

export const dateTime = (iso) =>
  iso ? new Date(iso).toLocaleString("en-IN", {
    day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit",
  }) : "";

export const timeOnly = (iso) =>
  iso ? new Date(iso).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" }) : "";

/** "Today", "Yesterday" or "20 Oct 2026". */
export function dayLabel(iso) {
  const d = new Date(iso);
  const today = new Date();
  const yesterday = new Date();
  yesterday.setDate(today.getDate() - 1);
  if (d.toDateString() === today.toDateString()) return "Today";
  if (d.toDateString() === yesterday.toDateString()) return "Yesterday";
  return d.toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" });
}

export const initials = (name = "") =>
  name.split(/\s+/).filter((w) => /^[\p{L}\p{N}]/u.test(w)).map((w) => w[0]).join("").slice(0, 2).toUpperCase() || "?";

export const masked = (accountNumber = "") => `XXXX XXXX ${accountNumber.slice(-4)}`;

export const REASON_LABELS = {
  HIGH_VALUE: "Above ₹2,00,000 threshold",
  UNUSUAL_AMOUNT: "Unusually large amount",
  NEW_RECIPIENT: "New recipient",
  ODD_HOUR: "Odd hour",
  RAPID_REPEAT: "Rapid repeat transfers",
  HIGH_BALANCE_SHARE: "Drains most of the balance",
  UNUSUAL_PATTERN: "Unusual pattern",
};
