import { useState } from "react";
import Notice from "../../components/Notice";
import { describeError } from "./errors";

export default function BuildingForm({ initial, onSubmit, onCancel }) {
    const [name, setName] = useState(initial?.name ?? "");
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        setSubmitting(true);
        try {
            await onSubmit({ name });
        } catch (err) {
            setError(describeError(err, "Couldn't save the building.", {
                409: "A building with that name already exists.",
            }));
            setSubmitting(false);
        }
    }

    return (
        <form onSubmit={handleSubmit}>
            <div className="field">
                <label className="label" htmlFor="building-name">Name</label>
                <input id="building-name" className="input" value={name} maxLength={255} required
                       placeholder="Solsiden Borettslag" onChange={(e) => setName(e.target.value)} />
            </div>
            {error && <Notice kind="error">{error}</Notice>}
            <div className="form-actions">
                <button className="btn" type="submit" disabled={submitting}>
                    {submitting ? "Saving…" : initial ? "Save changes" : "Add building"}
                </button>
                <button className="btn btn--secondary" type="button" onClick={onCancel}>Cancel</button>
            </div>
        </form>
    );
}
