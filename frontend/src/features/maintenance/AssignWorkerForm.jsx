import { useState } from "react";
import Badge from "../../components/Badge";
import { Priority } from "../../components/Indicators";
import { CATEGORY_LABELS, requestTitle } from "./labels";

/** Pick a worker for a request. Workers who handle the request's category are listed first. */
export default function AssignWorkerForm({ request, workers, reporter, onAssign, onCancel }) {
    const [workerId, setWorkerId] = useState(request.assignedWorkerId ?? null);
    const [saving, setSaving] = useState(false);

    const handles = (w) => w.specialties.includes(request.category);
    const sorted = [...workers].sort((a, b) => Number(handles(b)) - Number(handles(a)) || a.name.localeCompare(b.name));

    async function handleSubmit(e) {
        e.preventDefault();
        setSaving(true);
        try {
            await onAssign(workerId);
        } finally {
            setSaving(false);
        }
    }

    return (
        <form onSubmit={handleSubmit}>
            <div className="card" style={{ marginBottom: 18, background: "var(--canvas)" }}>
                <div className="section__head" style={{ marginBottom: 4 }}>
                    <strong>{requestTitle(request)}</strong>
                    <Priority priority={request.priority} suffix=" priority" />
                </div>
                <p>{request.description}</p>
                {reporter && <p className="small muted" style={{ marginTop: 6 }}>Reported by {reporter}</p>}
            </div>

            <span className="label">Worker</span>
            <div className="item-list card card--flush">
                {sorted.map((w) => (
                    <label key={w.id} className="item" style={{ cursor: "pointer" }}>
                        <input type="radio" name="worker" value={w.id} checked={workerId === w.id}
                               onChange={() => setWorkerId(w.id)} />
                        <span className="item__body">
                            <span className="item__title" style={{ display: "block" }}>{w.name}</span>
                            <span className="item__meta">
                                {w.specialties.map((s) => CATEGORY_LABELS[s] ?? s).join(", ")}
                            </span>
                        </span>
                        {handles(w) && <Badge>Handles {CATEGORY_LABELS[request.category].toLowerCase()}</Badge>}
                    </label>
                ))}
            </div>

            <div className="form-actions">
                <button className="btn" type="submit" disabled={!workerId || saving}>
                    {saving ? "Assigning…" : request.assignedWorkerId ? "Reassign" : "Assign"}
                </button>
                <button className="btn btn--secondary" type="button" onClick={onCancel}>Cancel</button>
            </div>
        </form>
    );
}
