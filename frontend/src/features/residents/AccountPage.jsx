import { useState } from "react";
import { StatusDot } from "../../components/Indicators";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { getHousehold, updateMyContact } from "../../api/residents";
import { initials } from "../../lib/format";
import { describeError } from "./errors";

function ContactForm() {
    const { token, me, refreshMe } = useAuth();
    const toast = useToast();
    const [form, setForm] = useState({ email: me.email ?? "", phone: me.phone ?? "" });
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);

    const update = (field) => (e) => setForm({ ...form, [field]: e.target.value });
    const changed = form.email !== (me.email ?? "") || form.phone !== (me.phone ?? "");

    async function handleSubmit(e) {
        e.preventDefault();
        setError("");
        setSaving(true);
        try {
            await updateMyContact(token, form);
            await refreshMe();
            toast("Contact details saved");
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
        <form onSubmit={handleSubmit}>
            <div className="field">
                <label className="label" htmlFor="email">Email</label>
                <input id="email" className="input" type="email" value={form.email} onChange={update("email")} required />
            </div>
            <div className="field">
                <label className="label" htmlFor="phone">Phone</label>
                <input id="phone" className="input" type="tel" value={form.phone} onChange={update("phone")}
                       maxLength={20} placeholder="912 34 567" />
                <p className="hint">The board and maintenance workers use this to reach you.</p>
            </div>
            {error && <Notice kind="error">{error}</Notice>}
            <div className="form-actions">
                <button className="btn" type="submit" disabled={saving || !changed}>
                    {saving ? "Saving…" : "Save changes"}
                </button>
            </div>
        </form>
    );
}

export default function AccountPage() {
    const { token, me } = useAuth();
    const household = useAsync(() => getHousehold(token), [token]);

    return (
        <>
            <PageHeader title="Your account" />
            <div className="split">
                <section className="card">
                    <h2 className="section__title" style={{ marginBottom: 8 }}>Your home</h2>
                    <ul className="rows">
                        <li className="row"><span className="row__label">Apartment</span><span>{me.apartmentNumber}</span></li>
                        <li className="row"><span className="row__label">Floor</span><span>{me.floor}</span></li>
                        <li className="row"><span className="row__label">Building</span><span>{me.buildingName}</span></li>
                        <li className="row">
                            <span className="row__label">Status</span>
                            <StatusDot tone={me.active ? "success" : "neutral"}>{me.active ? "Active resident" : "Inactive"}</StatusDot>
                        </li>
                    </ul>

                    <h3 className="section__title" style={{ margin: "18px 0 8px" }}>Household</h3>
                    {household.loading && <Skeleton rows={2} />}
                    {household.error && <Notice kind="error">Couldn't load your household.</Notice>}
                    <ul className="rows">
                        {(household.data ?? []).map((m) => (
                            <li key={m.id} className="row" style={{ justifyContent: "flex-start" }}>
                                <span className="avatar" aria-hidden="true">{initials(m.firstName, m.lastName)}</span>
                                <span>{m.firstName} {m.lastName}{m.id === me.id && <span className="muted"> (you)</span>}</span>
                            </li>
                        ))}
                    </ul>
                </section>

                <section className="card">
                    <h2 className="section__title" style={{ marginBottom: 14 }}>Contact details</h2>
                    <ContactForm />
                </section>
            </div>
        </>
    );
}
