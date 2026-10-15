import { useState } from "react";
import Notice from "../../components/Notice";
import { useAuth } from "../../auth/useAuth";
import { describeError } from "./errors";
import { updateMyContact } from "../../api/residents";

export default function ContactPage() {
    const { token, me, refreshMe } = useAuth();
    const [form, setForm] = useState({ email: me.email ?? "", phone: me.phone ?? "" });
    const [error, setError] = useState("");
    const [saved, setSaved] = useState(false);
    const [saving, setSaving] = useState(false);

    const update = (field) => (e) => {
        setSaved(false);
        setForm({ ...form, [field]: e.target.value });
    };

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        setSaved(false);
        setSaving(true);
        try {
            await updateMyContact(token, form);
            await refreshMe();
            setSaved(true);
        } catch (err) {
            setError(describeError(err, "Couldn't save your details.", {
                409: "That email is already in use.",
                400: "Enter a valid email, and a phone number of at most 20 characters.",
            }));
        } finally {
            setSaving(false);
        }
    }

    return (
        <form className="form" onSubmit={handleSubmit}>
            <div className="field">
                <label className="label" htmlFor="email">Email</label>
                <input id="email" className="input" type="email" value={form.email}
                       onChange={update("email")} required />
            </div>

            <div className="field">
                <label className="label" htmlFor="phone">Phone</label>
                <input id="phone" className="input" type="tel" value={form.phone}
                       onChange={update("phone")} maxLength={20} placeholder="Optional" />
            </div>

            {error && <Notice kind="error">{error}</Notice>}
            {saved && <Notice kind="success">Saved.</Notice>}

            <button className="btn btn--block" type="submit" disabled={saving}>
                {saving ? "Saving…" : "Save changes"}
            </button>
        </form>
    );
}
