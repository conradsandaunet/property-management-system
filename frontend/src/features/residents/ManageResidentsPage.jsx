import { useEffect, useState } from "react";
import PageShell from "./PageShell";
import ResidentForm from "./ResidentForm";
import { describeError } from "./errors";
import { getMe } from "../../api/auth";
import {
    createResident,
    getApartments,
    removeResident,
    searchResidents,
    updateResident,
} from "../../api/residents";

export default function ManageResidentsPage({ token }) {
    const [meId, setMeId] = useState(null);
    const [apartments, setApartments] = useState([]);
    const [query, setQuery] = useState("");
    const [includeInactive, setIncludeInactive] = useState(false);
    const [residents, setResidents] = useState(null);
    const [error, setError] = useState("");
    const [adding, setAdding] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [confirmingId, setConfirmingId] = useState(null);
    const [reloadKey, setReloadKey] = useState(0);

    useEffect(() => {
        getMe(token).then((me) => setMeId(me.id)).catch(() => {});
        getApartments(token)
            .then(setApartments)
            .catch((e) => setError(describeError(e, "Couldn't load apartments.")));
    }, [token]);

    useEffect(() => {
        let cancelled = false;
        const timer = setTimeout(() => {
            searchResidents(token, query, includeInactive)
                .then((data) => {
                    if (!cancelled) {
                        setResidents(data);
                        setError("");
                    }
                })
                .catch((e) => {
                    if (!cancelled) setError(describeError(e, "Couldn't load residents."));
                });
        }, query ? 300 : 0);
        return () => {
            cancelled = true;
            clearTimeout(timer);
        };
    }, [token, query, includeInactive, reloadKey]);

    const reload = () => setReloadKey((k) => k + 1);

    async function handleCreate(values) {
        await createResident(token, values);
        setAdding(false);
        reload();
    }

    async function handleUpdate(id, values) {
        await updateResident(token, id, values);
        setEditingId(null);
        reload();
    }

    async function handleRemove(id) {
        try {
            await removeResident(token, id);
            setConfirmingId(null);
            reload();
        } catch (e) {
            setError(describeError(e, "Couldn't remove the resident.", {
                409: "You can't remove yourself.",
            }));
        }
    }

    async function handleReactivate(id) {
        try {
            await updateResident(token, id, { active: true });
            reload();
        } catch (e) {
            setError(describeError(e, "Couldn't reactivate the resident."));
        }
    }

    return (
        <PageShell eyebrow="Board" title="Manage residents" wide>
            <div className="res-toolbar">
                <div className="res-field">
                    <label className="res-label" htmlFor="search">Search</label>
                    <input
                        id="search"
                        className="res-input"
                        type="search"
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                        placeholder="Name or apartment number"
                    />
                </div>
                <label className="res-check">
                    <input
                        type="checkbox"
                        checked={includeInactive}
                        onChange={(e) => setIncludeInactive(e.target.checked)}
                    />
                    Include removed
                </label>
            </div>

            {adding ? (
                <div className="res-add">
                    <ResidentForm
                        mode="create"
                        apartments={apartments}
                        onSubmit={handleCreate}
                        onCancel={() => setAdding(false)}
                    />
                </div>
            ) : (
                <div className="res-add">
                    <button className="res-submit res-submit--inline" type="button"
                            onClick={() => { setAdding(true); setEditingId(null); }}>
                        Add resident
                    </button>
                </div>
            )}

            {error && <p className="res-error">{error}</p>}
            {residents === null && !error && <p className="res-muted">Loading...</p>}
            {residents && residents.length === 0 && <p className="res-muted">No residents found.</p>}

            {residents && residents.length > 0 && (
                <ul className="res-list">
                    {residents.map((r) => {
                        const isSelf = r.id === meId;
                        return (
                            <li key={r.id} className="res-item">
                                {editingId === r.id ? (
                                    <ResidentForm
                                        mode="edit"
                                        initial={r}
                                        apartments={apartments}
                                        isSelf={isSelf}
                                        onSubmit={(values) => handleUpdate(r.id, values)}
                                        onCancel={() => setEditingId(null)}
                                    />
                                ) : (
                                    <>
                                        <div className="res-item-top">
                                            <span className="res-item-title">
                                                {r.firstName} {r.lastName}
                                                {isSelf && <span className="res-self"> (you)</span>}
                                            </span>
                                            <span className="res-badges">
                                                {r.manager && <span className="res-badge res-badge--board">Board</span>}
                                                <span className={`res-badge res-badge--${r.active ? "active" : "removed"}`}>
                                                    {r.active ? "Active" : "Removed"}
                                                </span>
                                            </span>
                                        </div>
                                        <div className="res-item-meta">
                                            <span>{r.email}</span>
                                            {r.phone && <span>{r.phone}</span>}
                                            <span>Apartment {r.apartmentNumber}</span>
                                        </div>
                                        <div className="res-actions">
                                            {r.active ? (
                                                <>
                                                    <button className="res-link-btn" type="button"
                                                            onClick={() => { setEditingId(r.id); setAdding(false); setConfirmingId(null); }}>
                                                        Edit
                                                    </button>
                                                    {!isSelf && confirmingId !== r.id && (
                                                        <button className="res-link-btn res-link-btn--danger" type="button"
                                                                onClick={() => setConfirmingId(r.id)}>
                                                            Remove
                                                        </button>
                                                    )}
                                                    {confirmingId === r.id && (
                                                        <span className="res-confirm">
                                                            Remove {r.firstName}?
                                                            <button className="res-link-btn res-link-btn--danger" type="button"
                                                                    onClick={() => handleRemove(r.id)}>
                                                                Yes, remove
                                                            </button>
                                                            <button className="res-link-btn" type="button"
                                                                    onClick={() => setConfirmingId(null)}>
                                                                Cancel
                                                            </button>
                                                        </span>
                                                    )}
                                                </>
                                            ) : (
                                                <button className="res-link-btn" type="button"
                                                        onClick={() => handleReactivate(r.id)}>
                                                    Reactivate
                                                </button>
                                            )}
                                        </div>
                                    </>
                                )}
                            </li>
                        );
                    })}
                </ul>
            )}
        </PageShell>
    );
}
