import { useEffect, useState } from "react";
import PageShell from "./PageShell";
import { describeError } from "./errors";
import { getMe } from "../../api/auth";
import { updateMyContact } from "../../api/residents";

export default function ContactPage({ token }) {
    const [form, setForm] = useState(null);
    const [error, setError] = useState("");
    const [saved, setSaved] = useState(false);
    const [saving, setSaving] = useState(false);

    useEffect(() => {
        getMe(token)
            .then((me) => setForm({ email: me.email ?? "", phone: me.phone ?? "" }))
            .catch((e) => setError(describeError(e, "Couldn't load your details.")));
    }, [token]);

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
        <PageShell eyebrow="Residents" title="Contact details">
            {!form && !error && <p className="res-muted">Loading...</p>}
            {!form && error && <p className="res-error">{error}</p>}

            {form && (
                <form onSubmit={handleSubmit}>
                    <div className="res-field">
                        <label className="res-label" htmlFor="email">Email</label>
                        <input
                            id="email"
                            className="res-input"
                            type="email"
                            value={form.email}
                            onChange={update("email")}
                            required
                        />
                    </div>

                    <div className="res-field">
                        <label className="res-label" htmlFor="phone">Phone</label>
                        <input
                            id="phone"
                            className="res-input"
                            type="tel"
                            value={form.phone}
                            onChange={update("phone")}
                            maxLength={20}
                            placeholder="Optional"
                        />
                    </div>

                    {error && <p className="res-error">{error}</p>}
                    {saved && <p className="res-notice">Saved.</p>}

                    <button className="res-submit" type="submit" disabled={saving}>
                        {saving ? "Saving…" : "Save changes"}
                    </button>
                </form>
            )}
        </PageShell>
    );
}
