import { useState } from "react";
import { describeError } from "./errors";

export default function ResidentForm({ mode, initial, apartments, isSelf = false, onSubmit, onCancel }) {
    const creating = mode === "create";
    const [form, setForm] = useState(() => ({
        firstName: initial?.firstName ?? "",
        lastName: initial?.lastName ?? "",
        email: initial?.email ?? "",
        phone: initial?.phone ?? "",
        password: "",
        manager: initial?.manager ?? false,
        apartmentId: initial?.apartmentId ?? "",
    }));
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        setSubmitting(true);

        const body = {
            firstName: form.firstName,
            lastName: form.lastName,
            email: form.email,
            phone: creating ? form.phone.trim() || null : form.phone,
            manager: form.manager,
            apartmentId: Number(form.apartmentId || apartments[0]?.id),
        };
        if (creating) {
            body.password = form.password;
        }

        try {
            await onSubmit(body);
        } catch (err) {
            setError(describeError(err, "Couldn't save the resident.", {
                409: "That email is already in use.",
                404: "That apartment no longer exists.",
            }));
            setSubmitting(false);
        }
    }

    const idPrefix = creating ? "new" : `edit-${initial.id}`;

    return (
        <form className="res-form-card" onSubmit={handleSubmit}>
            <div className="res-fields-2">
                <div className="res-field">
                    <label className="res-label" htmlFor={`${idPrefix}-first`}>First name</label>
                    <input id={`${idPrefix}-first`} className="res-input" value={form.firstName}
                           onChange={update("firstName")} maxLength={100} required />
                </div>
                <div className="res-field">
                    <label className="res-label" htmlFor={`${idPrefix}-last`}>Last name</label>
                    <input id={`${idPrefix}-last`} className="res-input" value={form.lastName}
                           onChange={update("lastName")} maxLength={100} required />
                </div>
            </div>

            <div className="res-field">
                <label className="res-label" htmlFor={`${idPrefix}-email`}>Email</label>
                <input id={`${idPrefix}-email`} className="res-input" type="email" value={form.email}
                       onChange={update("email")} required />
            </div>

            <div className="res-field">
                <label className="res-label" htmlFor={`${idPrefix}-phone`}>Phone</label>
                <input id={`${idPrefix}-phone`} className="res-input" type="tel" value={form.phone}
                       onChange={update("phone")} maxLength={20} placeholder="Optional" />
            </div>

            {creating && (
                <div className="res-field">
                    <label className="res-label" htmlFor={`${idPrefix}-password`}>Initial password</label>
                    <input id={`${idPrefix}-password`} className="res-input" type="password" value={form.password}
                           onChange={update("password")} minLength={6} required />
                    <p className="res-hint">The resident uses this to log in. Share it with them directly.</p>
                </div>
            )}

            <div className="res-field">
                <label className="res-label" htmlFor={`${idPrefix}-apartment`}>Apartment</label>
                <select id={`${idPrefix}-apartment`} className="res-input" value={form.apartmentId}
                        onChange={update("apartmentId")} required>
                    {!form.apartmentId && apartments.length > 0 && (
                        <option value="" disabled>Select an apartment</option>
                    )}
                    {apartments.map((a) => (
                        <option key={a.id} value={a.id}>
                            {a.apartmentNumber} · floor {a.floor} · {a.buildingName}
                        </option>
                    ))}
                </select>
            </div>

            <div className="res-field">
                <label className={`res-check${isSelf ? " res-check--disabled" : ""}`}>
                    <input
                        type="checkbox"
                        checked={form.manager}
                        disabled={isSelf}
                        onChange={(e) => setForm({ ...form, manager: e.target.checked })}
                    />
                    Board member
                </label>
                {isSelf && <p className="res-hint">You can't remove your own board access.</p>}
            </div>

            {error && <p className="res-error">{error}</p>}

            <div className="res-form-actions">
                <button className="res-submit res-submit--inline" type="submit" disabled={submitting}>
                    {submitting ? "Saving…" : creating ? "Add resident" : "Save changes"}
                </button>
                <button className="res-link-btn" type="button" onClick={onCancel}>Cancel</button>
            </div>
        </form>
    );
}
