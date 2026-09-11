import { useEffect, useState } from "react";
import { getMe } from "./api";
import "./TestPage.css";

export default function TestPage({ token, onLogout }) {
  const [me, setMe] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    getMe(token)
      .then(setMe)
      .catch(() => setError("Couldn't load your account."));
  }, [token]);

  return (
    <div className="dash-shell">
      <div className="dash-panel">
        <div className="dash-eyebrow">
          <span className="dash-eyebrow-mark" />
          <span className="dash-eyebrow-text">Resident Portal</span>
        </div>

        <h1 className="dash-heading">
          {me ? `Welcome, ${me.firstName}` : "Welcome"}
        </h1>
        <hr className="dash-rule" />

        {error && <p className="dash-error">{error}</p>}

        {me && (
          <ul className="dash-list">
            <li className="dash-row">
              <span className="dash-row-label">Name</span>
              <span className="dash-row-value">
                {me.firstName} {me.lastName}
              </span>
            </li>
            <li className="dash-row">
              <span className="dash-row-label">Email</span>
              <span className="dash-row-value">{me.email}</span>
            </li>
            <li className="dash-row">
              <span className="dash-row-label">Role</span>
              <span className="dash-row-value is-role">
                {me.manager ? "Board" : "Resident"}
              </span>
            </li>
          </ul>
        )}

        <button className="dash-logout" onClick={onLogout}>
          Log out
        </button>
      </div>
    </div>
  );
}
