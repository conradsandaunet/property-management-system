import { useEffect, useState } from "react";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import ResidentForm from "./ResidentForm";
import { describeError } from "./errors";
import { useAuth } from "../../auth/useAuth";
import {
    createResident,
    getApartments,
    removeResident,
    searchResidents,
    updateResident,
} from "../../api/residents";

export default function ManageResidentsPage() {
    const { token, me, refreshMe } = useAuth();
    const meId = me.id;
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
        if (id === meId) refreshMe();
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
        <>
            <PageHeader eyebrow="Board" title="Manage residents" />

            <div className="toolbar">
                <div className="field">
                    <label className="label" htmlFor="search">Search</label>
                    <input
                        id="search"
                        className="input"
                        type="search"
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                        placeholder="Name or apartment number"
                    />
                </div>
                <label className="check">
                    <input
                        type="checkbox"
                        checked={includeInactive}
                        onChange={(e) => setIncludeInactive(e.target.checked)}
                    />
                    Include removed
                </label>
            </div>

            <div className="section-gap">
                {adding ? (
                    <ResidentForm
                        mode="create"
                        apartments={apartments}
                        onSubmit={handleCreate}
                        onCancel={() => setAdding(false)}
                    />
                ) : (
                    <button className="btn" type="button"
                            onClick={() => { setAdding(true); setEditingId(null); }}>
                        Add resident
                    </button>
                )}
            </div>

            {error && <Notice kind="error">{error}</Notice>}
            {residents === null && !error && <Notice>Loading...</Notice>}
            {residents && residents.length === 0 && <Notice>No residents found.</Notice>}

            {residents && residents.length > 0 && (
                <ul className="list list--wide">
                    {residents.map((r) => {
                        const isSelf = r.id === meId;
                        return (
                            <li key={r.id} className="list-item">
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
                                        <div className="list-item__top">
                                            <span className="list-item__title">
                                                {r.firstName} {r.lastName}
                                                {isSelf && <span className="list-item__sub"> (you)</span>}
                                            </span>
                                            <span className="badges">
                                                {r.manager && <Badge variant="info">Board</Badge>}
                                                <Badge variant={r.active ? "success" : undefined}>
                                                    {r.active ? "Active" : "Removed"}
                                                </Badge>
                                            </span>
                                        </div>
                                        <div className="list-item__meta">
                                            <span>{r.email}</span>
                                            {r.phone && <span>{r.phone}</span>}
                                            <span>Apartment {r.apartmentNumber}</span>
                                        </div>
                                        <div className="list-item__actions">
                                            {r.active ? (
                                                <>
                                                    <button className="btn-link" type="button"
                                                            onClick={() => { setEditingId(r.id); setAdding(false); setConfirmingId(null); }}>
                                                        Edit
                                                    </button>
                                                    {!isSelf && confirmingId !== r.id && (
                                                        <button className="btn-link btn-link--danger" type="button"
                                                                onClick={() => setConfirmingId(r.id)}>
                                                            Remove
                                                        </button>
                                                    )}
                                                    {confirmingId === r.id && (
                                                        <span className="list-item__confirm">
                                                            Remove {r.firstName}?
                                                            <button className="btn-link btn-link--danger" type="button"
                                                                    onClick={() => handleRemove(r.id)}>
                                                                Yes, remove
                                                            </button>
                                                            <button className="btn-link" type="button"
                                                                    onClick={() => setConfirmingId(null)}>
                                                                Cancel
                                                            </button>
                                                        </span>
                                                    )}
                                                </>
                                            ) : (
                                                <button className="btn-link" type="button"
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
        </>
    );
}
