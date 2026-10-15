import { Link } from "react-router-dom";
import "./residents.css";

export default function PageShell({ eyebrow, title, wide = false, children }) {
    return (
        <div className="res-shell">
            <div className={`res-panel${wide ? " res-panel--wide" : ""}`}>
                <Link className="res-back" to="/">← Back</Link>

                <div className="res-eyebrow">
                    <span className="res-eyebrow-mark" />
                    <span className="res-eyebrow-text">{eyebrow}</span>
                </div>

                <h1 className="res-heading">{title}</h1>
                <hr className="res-rule" />

                {children}
            </div>
        </div>
    );
}
