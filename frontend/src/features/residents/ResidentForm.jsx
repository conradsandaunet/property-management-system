import { useState } from "react";
import Notice from "../../components/Notice";
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
        <form className="form-card" onSubmit={handleSubmit}>
            <div className="field-row">
                <div className="field">
                    <label className="label" htmlFor={`${idPrefix}-first`}>First name</label>
                    <input id={`${idPrefix}-first`} className="input" value={form.firstName}
                           onChange={update("firstName")} maxLength={100} required />
                </div>
                <div className="field">
                    <label className="label" htmlFor={`${idPrefix}-last`}>Last name</label>
                    <input id={`${idPrefix}-last`} className="input" value={form.lastName}
                           onChange={update("lastName")} maxLength={100} required />
                </div>
            </div>

            <div className="field">
                <label className="label" htmlFor={`${idPrefix}-email`}>Email</label>
                <input id={`${idPrefix}-email`} className="input" type="email" value={form.email}
                       onChange={update("email")} required />
            </div>

            <div className="field">
                <label className="label" htmlFor={`${idPrefix}-phone`}>Phone</label>
                <input id={`${idPrefix}-phone`} className="input" type="tel" value={form.phone}
                       onChange={update("phone")} maxLength={20} placeholder="Optional" />
            </div>

            {creating && (
                <div className="field">
                    <label className="label" htmlFor={`${idPrefix}-password`}>Initial password</label>
                    <input id={`${idPrefix}-password`} className="input" type="password" value={form.password}
                           onChange={update("password")} minLength={6} required />
                    <p className="hint">The resident uses this to log in. Share it with them directly.</p>
                </div>
            )}

            <div className="field">
                <label className="label" htmlFor={`${idPrefix}-apartment`}>Apartment</label>
                <select id={`${idPrefix}-apartment`} className="input" value={form.apartmentId}
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

            <div className="field">
                <label className={`check${isSelf ? " is-disabled" : ""}`}>
                    <input
                        type="checkbox"
                        checked={form.manager}
                        disabled={isSelf}
                        onChange={(e) => setForm({ ...form, manager: e.target.checked })}
                    />
                    Board member
                </label>
                {isSelf && <p className="hint">You can't remove your own board access.</p>}
            </div>

            {error && <Notice kind="error">{error}</Notice>}

            <div className="form-actions">
                <button className="btn" type="submit" disabled={submitting}>
                    {submitting ? "Saving…" : creating ? "Add resident" : "Save changes"}
                </button>
                <button className="btn-link" type="button" onClick={onCancel}>Cancel</button>
            </div>
        </form>
    );
}
