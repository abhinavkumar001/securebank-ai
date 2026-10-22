import { useState } from "react";
import { api } from "../api.js";
import { Notice } from "../ui.jsx";

export default function AuthPage({ onSignedIn }) {
  const [mode, setMode] = useState("login");
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError("");
    try {
      const body = mode === "login" ? { email: form.email, password: form.password } : form;
      const auth = await api(`/api/auth/${mode === "login" ? "login" : "register"}`, { method: "POST", body });
      onSignedIn({ token: auth.token, userId: auth.userId, name: auth.name, role: auth.role, previousLoginAt: auth.previousLoginAt });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth">
      <section className="auth-panel">
        <div className="brand brand-large">
          <span className="mark" aria-hidden="true">S</span>
          <span className="brand-name">SecureBank</span>
        </div>
        <p className="auth-lede">
          Every transfer is locked, checked for fraud and written to an audit trail before a single rupee moves.
        </p>
      </section>

      <form className="auth-form" onSubmit={submit}>
        <h1>{mode === "login" ? "Sign in" : "Open your profile"}</h1>

        {mode === "register" && (
          <label>
            Full name
            <input value={form.name} onChange={update("name")} required maxLength={100} autoComplete="name" />
          </label>
        )}
        <label>
          Email
          <input type="email" value={form.email} onChange={update("email")} required autoComplete="email" />
        </label>
        <label>
          Password
          <input
            type="password"
            value={form.password}
            onChange={update("password")}
            required
            minLength={mode === "register" ? 8 : undefined}
            autoComplete={mode === "login" ? "current-password" : "new-password"}
          />
          {mode === "register" && <small className="hint">At least 8 characters, with a letter and a number.</small>}
        </label>

        <Notice>{error}</Notice>

        <button className="btn btn-primary btn-block" disabled={busy}>
          {busy ? "Please wait…" : mode === "login" ? "Sign in" : "Create profile"}
        </button>

        <p className="switch-mode">
          {mode === "login" ? "New to SecureBank?" : "Already have a profile?"}{" "}
          <button
            type="button"
            className="link-button"
            onClick={() => { setMode(mode === "login" ? "register" : "login"); setError(""); }}
          >
            {mode === "login" ? "Create a profile" : "Sign in instead"}
          </button>
        </p>
      </form>
    </div>
  );
}
