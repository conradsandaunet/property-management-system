import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { createMaintenance } from "../../api/maintenance";
import "./maintenance.css";

const CATEGORIES = [
    { value: "PLUMBING", label: "Plumbing" },
    { value: "ELECTRICAL", label: "Electrical" },
    { value: "HEATING", label: "Heating" },
    { value: "APPLIANCE", label: "Appliance" },
    { value: "OTHER", label: "Other" },
];

const PRIORITIES = [
    { value: "LOW", label: "Low" },
    { value: "MEDIUM", label: "Medium" },
    { value: "HIGH", label: "High" },
    { value: "URGENT", label: "Urgent" },
];

export default function NewRequestPage({ token }) {
    const navigate = useNavigate();
    const [form, setForm] = useState({
        category: CATEGORIES[0].value,
        location: "",
        description: "",
        priority: "MEDIUM",
    });
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    const update = (field) => (e) => setForm({...form, [field]: e.target.value});

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        setSubmitting(true);
        try {
            await createMaintenance(token, form);
            navigate("/maintenance");
        } catch {
            setError("Couldn't create the request.");
            setSubmitting(false);
        }
    }

    return (
        <div className="maint-shell">
            <div className="maint-panel">
                <button type="button" className="maint-back" onClick={() => navigate(-1)}>
                    ← Back
                </button>

                <div className="maint-eyebrow">
                    <span className="maint-eyebrow-mark" />
                    <span className="maint-eyebrow-text">Maintenance</span>
                </div>

                <h1 className="maint-heading">Report an issue</h1>
                <hr className="maint-rule" />

                <form onSubmit={handleSubmit}>
                    <div className="maint-field">
                        <label className="maint-label" htmlFor="category">Category</label>
                        <select
                            id="category"
                            className="maint-input"
                            value={form.category}
                            onChange={update("category")}
                        >
                            {CATEGORIES.map((c) => (
                                <option key={c.value} value={c.value}>{c.label}</option>
                            ))}
                        </select>
                    </div>

                    <div className="maint-field">
                        <label className="maint-label" htmlFor="location">Location</label>
                        <input
                            id="location"
                            className="maint-input"
                            value={form.location}
                            onChange={update("location")}
                            placeholder="e.g. Bathroom"
                            maxLength={100}
                            required
                        />
                    </div>

                    <div className="maint-field">
                        <label className="maint-label" htmlFor="description">Description</label>
                        <textarea
                            id="description"
                            className="maint-input maint-textarea"
                            value={form.description}
                            onChange={update("description")}
                            placeholder="Describe the issue"
                            rows={4}
                            required
                        />
                    </div>

                    <div className="maint-field">
                        <span className="maint-label">Priority</span>
                        <div className="maint-choices">
                            {PRIORITIES.map((p) => (
                                <label
                                    key={p.value}
                                    className={`maint-choice${form.priority === p.value ? " is-selected" : ""}`}
                                >
                                    <input
                                        type="radio"
                                        name="priority"
                                        value={p.value}
                                        checked={form.priority === p.value}
                                        onChange={update("priority")}
                                    />
                                    {p.label}
                                </label>
                            ))}
                        </div>
                    </div>

                    {error && <p className="maint-error">{error}</p>}

                    <button className="maint-submit" type="submit" disabled={submitting}>
                        {submitting ? "Submitting…" : "Submit request"}
                    </button>
                </form>
            </div>
        </div>
    );
}