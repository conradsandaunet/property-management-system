import { useState } from "react";
import Notice from "../../components/Notice";
import { CATEGORY_LABELS, PRIORITY_LABELS } from "./labels";

export default function NewRequestForm({ onSubmit, onCancel }) {
    const [form, setForm] = useState({ category: "PLUMBING", location: "", description: "", priority: "MEDIUM" });
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        setSubmitting(true);
        try {
            await onSubmit(form);
        } catch {
            setError("Couldn't send the request. Try again.");
            setSubmitting(false);
        }
    }

    return (
        <form onSubmit={handleSubmit}>
            <div className="field">
                <label className="label" htmlFor="req-category">What's the problem?</label>
                <select id="req-category" className="input" value={form.category} onChange={update("category")}>
                    {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                        <option key={value} value={value}>{label}</option>
                    ))}
                </select>
            </div>
            <div className="field">
                <label className="label" htmlFor="req-location">Where?</label>
                <input id="req-location" className="input" value={form.location} onChange={update("location")}
                       placeholder="Bathroom" maxLength={100} required />
            </div>
            <div className="field">
                <label className="label" htmlFor="req-description">Description</label>
                <textarea id="req-description" className="input" rows={4} value={form.description}
                          onChange={update("description")} placeholder="The tap under the sink is leaking" required />
            </div>
            <div className="field">
                <span className="label">How urgent is it?</span>
                <div className="choices">
                    {Object.entries(PRIORITY_LABELS).map(([value, label]) => (
                        <label key={value} className={`choice${form.priority === value ? " is-selected" : ""}`}>
                            <input type="radio" name="priority" value={value} checked={form.priority === value}
                                   onChange={update("priority")} />
                            {label}
                        </label>
                    ))}
                </div>
            </div>
            {error && <Notice kind="error">{error}</Notice>}
            <div className="form-actions">
                <button className="btn" type="submit" disabled={submitting}>
                    {submitting ? "Sending…" : "Send request"}
                </button>
                <button className="btn btn--secondary" type="button" onClick={onCancel}>Cancel</button>
            </div>
        </form>
    );
}
