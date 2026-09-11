import { useState } from "react";
import { login } from "./api";
import "./LoginPage.css";

export default function LoginPage({ onLogin }) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  async function handleSubmit(e) {
    e.preventDefault();
    setError("");

    try {
      const token = await login(email, password);
      onLogin(token);
    } catch {
      setError("Wrong email or password.");
    }
  }

  return (
    <div className="auth-shell">
      <div className="auth-panel">
        <div className="auth-eyebrow">
          <span className="auth-eyebrow-mark" />
          <span className="auth-eyebrow-text">Resident Portal</span>
        </div>

        <h1 className="auth-heading">Sign in</h1>
        <hr className="auth-rule" />

        <form onSubmit={handleSubmit} noValidate>
          <div className="auth-field">
            <label className="auth-label" htmlFor="email">
              Email
            </label>
            <input
              id="email"
              className="auth-input"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
            />
          </div>

          <div className="auth-field">
            <label className="auth-label" htmlFor="password">
              Password
            </label>
            <input
              id="password"
              className="auth-input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              required
            />
          </div>

          {error && <p className="auth-error">{error}</p>}

          <button className="auth-submit" type="submit">
            Sign in
          </button>
        </form>
      </div>
    </div>
  );
}
