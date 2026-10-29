import { useState } from "react";
import Notice from "../../components/Notice";
import { describeError } from "./errors";

export default function ApartmentForm({ mode, initial, onSubmit, onCancel }) {
    const creating = mode === "create";
    const [form, setForm] = useState(() => ({
        apartmentNumber: initial?.apartmentNumber ?? "",
        floor: initial?.floor ?? "",
        buildingName: initial?.buildingName ?? "",
    }));
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        setSubmitting(true);

        try {
            await onSubmit({
                apartmentNumber: form.apartmentNumber,
                floor: Number(form.floor),
                buildingName: form.buildingName,
            });
        } catch (err) {
            setError(describeError(err, "Couldn't save the apartment.", {
                409: "That apartment already exists in this building.",
            }));
            setSubmitting(false);
        }
    }

    const idPrefix = creating ? "new-apt" : `edit-apt-${initial.id}`;

    return (
        <form className="form-card" onSubmit={handleSubmit}>
            <div className="field-row">
                <div className="field">
                    <label className="label" htmlFor={`${idPrefix}-number`}>Apartment number</label>
                    <input id={`${idPrefix}-number`} className="input" value={form.apartmentNumber}
                           onChange={update("apartmentNumber")} maxLength={10} required />
                </div>
                <div className="field">
                    <label className="label" htmlFor={`${idPrefix}-floor`}>Floor</label>
                    <input id={`${idPrefix}-floor`} className="input" type="number" value={form.floor}
                           onChange={update("floor")} required />
                </div>
            </div>

            <div className="field">
                <label className="label" htmlFor={`${idPrefix}-building`}>Building name</label>
                <input id={`${idPrefix}-building`} className="input" value={form.buildingName}
                       onChange={update("buildingName")} maxLength={255} required />
            </div>

            {error && <Notice kind="error">{error}</Notice>}

            <div className="form-actions">
                <button className="btn" type="submit" disabled={submitting}>
                    {submitting ? "Saving…" : creating ? "Add apartment" : "Save changes"}
                </button>
                <button className="btn-link" type="button" onClick={onCancel}>Cancel</button>
            </div>
        </form>
    );
}
