import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMyMaintenance} from "../../api/maintenance";

const STATUS_LABELS = {
    SUBMITTED: "Submitted",
    ASSIGNED: "Assigned",
    IN_PROGRESS: "In progress",
    RESOLVED: "Resolved",
};

const CATEGORY_LABELS = {
    PLUMBING: "Plumbing",
    ELECTRICAL: "Electrical",
    HEATING: "Heating",
    APPLIANCE: "Appliance",
    OTHER: "Other",
};

const PRIORITY_LABELS = {
    LOW: "Low",
    MEDIUM: "Medium",
    HIGH: "High",
    URGENT: "Urgent",
};

function formatDate(iso) {
    return new Date(iso).toLocaleDateString("en-GB", {
        day: "numeric",
        month: "short",
        year: "numeric"
    });
}

export default function MyRequestPage({ token }) {
    const [requests, setRequests] = useState([]);
    const [error, setError] = useState("");

    useEffect(() => {
        getMyMaintenance(token)
            .then(setRequests)
            .catch(() => setError("Couldn't load your request."));
    }, [token]);

    return (
        <div className="maint-shell">
            <div className="maint-panel maint-panel--wide">
                <Link className="main-back" to="/">← Back</Link>

                <div className="maint-eyebrow">
                    <span className="maint-eyebrow-mark"/>
                    <span className="maint-eyebrow-text">Maintenance</span>
                </div>

                <h1 className="maint-heading">My requests</h1>
                <hr className="maint-rule"/>

                <Link className="maint-submit maint-submit--link" to="/maintenance/new">
                    Report a new issue
                </Link>

                {error && <p className="maint-error">{error}</p>}

                {requests === null && !error && <p className="maint-muted">Loading...</p>}

                {requests && requests.length === 0 && (
                    <p className="maint-muted">You haven't reported any issues yet.</p>
                )}

                {requests && requests.length > 0 && (
                    <ul className="maint-list">
                        {requests.map((r) => (
                            <li key={r.id} className="maint-item">
                                <div className="maint-item-top">
                  <span className="maint-item-title">
                    {CATEGORY_LABELS[r.category] ?? r.category} · {r.location}
                  </span>
                                    <span className={`maint-status maint-status--${r.status.toLowerCase()}`}>
                    {STATUS_LABELS[r.status] ?? r.status}
                  </span>
                                </div>
                                <p className="maint-item-desc">{r.description}</p>
                                <div className="maint-item-meta">
                                    <span>{formatDate(r.createdAt)}</span>
                                    <span>Priority: {PRIORITY_LABELS[r.priority] ?? r.priority}</span>
                                </div>
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        </div>
    );
}