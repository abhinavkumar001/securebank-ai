import { useEffect, useRef, useState } from "react";
import Icon from "./Icon.jsx";
import { api } from "../api.js";

const STARTERS = ["Why is my transfer held?", "How do I freeze my card?", "What is my IFSC code?"];

export default function Assistant({ session, onClose }) {
  const [messages, setMessages] = useState([
    { from: "bot", text: "Hi! Ask me about transfers, holds, cards, payees or statements. Answers come from the bank’s policy documents." },
  ]);
  const [suggestions, setSuggestions] = useState(STARTERS);
  const [question, setQuestion] = useState("");
  const [busy, setBusy] = useState(false);
  const listRef = useRef(null);

  useEffect(() => {
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: "smooth" });
  }, [messages]);

  const ask = async (text) => {
    const q = text.trim();
    if (!q || busy) return;
    setMessages((m) => [...m, { from: "me", text: q }]);
    setQuestion("");
    setBusy(true);
    try {
      const a = await api("/api/assistant/ask", { method: "POST", token: session.token, body: { question: q } });
      setMessages((m) => [...m, { from: "bot", text: a.answer, source: a.source, confidence: a.confidence }]);
      if (a.related?.length) setSuggestions(a.related);
    } catch (e) {
      setMessages((m) => [...m, { from: "bot", text: e.message }]);
    } finally {
      setBusy(false);
    }
  };

  return (
    <aside className="assist" aria-label="SecureBank Assist">
      <div className="assist-h">
        <span className="mark">S</span><b>SecureBank Assist</b><span className="tag">AI</span>
        <button className="icon-btn" onClick={onClose} aria-label="Close assistant"><Icon name="x" /></button>
      </div>
      <div className="msgs" ref={listRef} aria-live="polite">
        {messages.map((m, i) => (
          <div key={i} className={`msg ${m.from}`}>
            {m.text}
            {m.source && (
              <div className="src"><b>Source</b>{m.source}<small> · match {Math.round(m.confidence * 100)}%</small></div>
            )}
          </div>
        ))}
        {busy && <div className="msg bot typing">Looking that up…</div>}
      </div>
      <div className="sugg">
        {suggestions.map((s) => <button key={s} onClick={() => ask(s)}>{s}</button>)}
      </div>
      <form className="ask" onSubmit={(e) => { e.preventDefault(); ask(question); }}>
        <input value={question} onChange={(e) => setQuestion(e.target.value)} maxLength={500}
               placeholder="Ask about your banking…" aria-label="Ask the assistant" />
        <button className="btn btn-primary" disabled={busy || !question.trim()} aria-label="Send"><Icon name="send" /></button>
      </form>
      <p className="disc">Answers come from bank policy. For anything about your own money, raise a support ticket.</p>
    </aside>
  );
}
