import { useEffect, useState } from "react";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import ApartmentForm from "./ApartmentForm";
import ResidentForm from "./ResidentForm";
import { describeError } from "./errors";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import {
    createApartment,
    createResident,
    getApartmentResidents,
    getApartments,
    removeApartment,
    searchResidents,
    updateApartment,
    updateResident,
} from "../../api/residents";

export default function ManageApartmentsPage() {
    const { token } = useAuth();
    const { data: apartments, error: loadError, loading, reload } =
        useAsync(() => getApartments(token), [token]);
    const [adding, setAdding] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [confirmingId, setConfirmingId] = useState(null);
    const [error, setError] = useState("");
    const [expandedId, setExpandedId] = useState(null);
    const [residents, setResidents] = useState(null);
    const [addingResidentId, setAddingResidentId] = useState(null);
    const [residentsKey, setResidentsKey] = useState(0);
    const [movingToId, setMovingToId] = useState(null);
    const [moveQuery, setMoveQuery] = useState("");
    const [candidates, setCandidates] = useState(null);

    useEffect(() => {
        if (movingToId === null) return undefined;
        let cancelled = false;
        const timer = setTimeout(() => {
            searchResidents(token, moveQuery, false)
                .then((data) => {
                    if (!cancelled) setCandidates(data.filter((r) => r.apartmentId !== movingToId));
                })
                .catch((e) => {
                    if (!cancelled) setError(describeError(e, "Couldn't search residents."));
                });
        }, moveQuery ? 300 : 0);
        return () => { cancelled = true; clearTimeout(timer); };
    }, [token, movingToId, moveQuery, residentsKey]);

    useEffect(() => {
        if (expandedId === null) return undefined;
        let cancelled = false;
        getApartmentResidents(token, expandedId)
            .then((data) => { if (!cancelled) setResidents(data); })
            .catch((e) => {
                if (!cancelled) setError(describeError(e, "Couldn't load the residents."));
            });
        return () => { cancelled = true; };
    }, [token, expandedId, residentsKey]);

    function toggleResidents(id) {
        setResidents(null);
        setAddingResidentId(null);
        setMovingToId(null);
        setExpandedId(expandedId === id ? null : id);
    }

    function startMoving(id) {
        setAddingResidentId(null);
        setMoveQuery("");
        setCandidates(null);
        setMovingToId(id);
    }

    async function handleMoveHere(resident, apartmentId) {
        try {
            await updateResident(token, resident.id, { apartmentId });
            setError("");
            setResidentsKey((k) => k + 1);
            reload();
        } catch (e) {
            setError(describeError(e, "Couldn't move the resident."));
        }
    }

    async function handleAddResident(values) {
        await createResident(token, values);
        setAddingResidentId(null);
        setResidentsKey((k) => k + 1);
        reload();
    }

    async function handleCreate(values) {
        await createApartment(token, values);
        setAdding(false);
        reload();
    }

    async function handleUpdate(id, values) {
        await updateApartment(token, id, values);
        setEditingId(null);
        reload();
    }

    async function handleRemove(id) {
        try {
            await removeApartment(token, id);
            setConfirmingId(null);
            setError("");
            reload();
        } catch (e) {
            setConfirmingId(null);
            setError(describeError(e, "Couldn't remove the apartment.", {
                409: "This apartment still has residents. Move or remove them first.",
            }));
        }
    }

    const sorted = apartments && [...apartments].sort((a, b) =>
        a.buildingName.localeCompare(b.buildingName)
        || a.apartmentNumber.localeCompare(b.apartmentNumber, undefined, { numeric: true }));

    return (
        <>
            <PageHeader eyebrow="Board" title="Manage apartments" />

            <div className="section-gap">
                {adding ? (
                    <ApartmentForm mode="create" onSubmit={handleCreate} onCancel={() => setAdding(false)} />
                ) : (
                    <button className="btn" type="button"
                            onClick={() => { setAdding(true); setEditingId(null); }}>
                        Add apartment
                    </button>
                )}
            </div>

            {(error || loadError) && (
                <Notice kind="error">{error || describeError(loadError, "Couldn't load apartments.")}</Notice>
            )}
            {loading && <Notice>Loading...</Notice>}
            {sorted && sorted.length === 0 && <Notice>No apartments yet.</Notice>}

            {sorted && sorted.length > 0 && (
                <ul className="list list--wide">
                    {sorted.map((a) => (
                        <li key={a.id} className="list-item">
                            {editingId === a.id ? (
                                <ApartmentForm
                                    mode="edit"
                                    initial={a}
                                    onSubmit={(values) => handleUpdate(a.id, values)}
                                    onCancel={() => setEditingId(null)}
                                />
                            ) : (
                                <>
                                    <div className="list-item__top">
                                        <span className="list-item__title">Apartment {a.apartmentNumber}</span>
                                        <span className="badges">
                                            <Badge variant={a.residentCount > 0 ? "success" : undefined}>
                                                {a.residentCount > 0 ? `${a.residentCount} resident${a.residentCount > 1 ? "s" : ""}` : "Vacant"}
                                            </Badge>
                                        </span>
                                    </div>
                                    <div className="list-item__meta">
                                        <span>Floor {a.floor}</span>
                                        <span>{a.buildingName}</span>
                                    </div>
                                    <div className="list-item__actions">
                                        <button className="btn-link" type="button"
                                                onClick={() => toggleResidents(a.id)}>
                                            {expandedId === a.id ? "Hide residents" : "Residents"}
                                        </button>
                                        <button className="btn-link" type="button"
                                                onClick={() => { setEditingId(a.id); setAdding(false); setConfirmingId(null); }}>
                                            Edit
                                        </button>
                                        {confirmingId !== a.id && (
                                            <button className="btn-link btn-link--danger" type="button"
                                                    onClick={() => setConfirmingId(a.id)}>
                                                Remove
                                            </button>
                                        )}
                                        {confirmingId === a.id && (
                                            <span className="list-item__confirm">
                                                Remove apartment {a.apartmentNumber}?
                                                <button className="btn-link btn-link--danger" type="button"
                                                        onClick={() => handleRemove(a.id)}>
                                                    Yes, remove
                                                </button>
                                                <button className="btn-link" type="button"
                                                        onClick={() => setConfirmingId(null)}>
                                                    Cancel
                                                </button>
                                            </span>
                                        )}
                                    </div>
                                    {expandedId === a.id && (
                                        <div className="section-gap">
                                            {residents === null && <Notice>Loading...</Notice>}
                                            {residents && residents.length === 0 && (
                                                <Notice>No one lives here yet.</Notice>
                                            )}
                                            {residents && residents.length > 0 && (
                                                <ul className="list">
                                                    {residents.map((r) => (
                                                        <li key={r.id} className="list-item">
                                                            <div className="list-item__top">
                                                                <span className="list-item__title">
                                                                    {r.firstName} {r.lastName}
                                                                </span>
                                                                {r.manager && <Badge variant="info">Board</Badge>}
                                                            </div>
                                                            <div className="list-item__meta">
                                                                <span>{r.email}</span>
                                                                {r.phone && <span>{r.phone}</span>}
                                                            </div>
                                                        </li>
                                                    ))}
                                                </ul>
                                            )}
                                            {movingToId === a.id && (
                                                <div className="form-card">
                                                    <div className="field">
                                                        <label className="label" htmlFor={`move-${a.id}`}>
                                                            Find an existing resident
                                                        </label>
                                                        <input id={`move-${a.id}`} className="input" type="search"
                                                               value={moveQuery} placeholder="Name or apartment number"
                                                               onChange={(e) => setMoveQuery(e.target.value)} />
                                                    </div>
                                                    {candidates === null && <Notice>Loading...</Notice>}
                                                    {candidates && candidates.length === 0 && (
                                                        <Notice>No other residents found.</Notice>
                                                    )}
                                                    {candidates && candidates.length > 0 && (
                                                        <ul className="list">
                                                            {candidates.map((r) => (
                                                                <li key={r.id} className="list-item">
                                                                    <div className="list-item__top">
                                                                        <span className="list-item__title">
                                                                            {r.firstName} {r.lastName}
                                                                        </span>
                                                                        <span className="list-item__sub">
                                                                            now in apartment {r.apartmentNumber}
                                                                        </span>
                                                                    </div>
                                                                    <div className="list-item__actions">
                                                                        <button className="btn-link" type="button"
                                                                                onClick={() => handleMoveHere(r, a.id)}>
                                                                            Move here
                                                                        </button>
                                                                    </div>
                                                                </li>
                                                            ))}
                                                        </ul>
                                                    )}
                                                    <div className="form-actions">
                                                        <button className="btn-link" type="button"
                                                                onClick={() => setMovingToId(null)}>
                                                            Done
                                                        </button>
                                                    </div>
                                                </div>
                                            )}
                                            {addingResidentId === a.id ? (
                                                <ResidentForm
                                                    mode="create"
                                                    initial={{ apartmentId: a.id }}
                                                    apartments={[a]}
                                                    lockApartment
                                                    onSubmit={handleAddResident}
                                                    onCancel={() => setAddingResidentId(null)}
                                                />
                                            ) : (
                                                <div className="list-item__actions">
                                                    <button className="btn-link" type="button"
                                                            onClick={() => { setAddingResidentId(a.id); setMovingToId(null); }}>
                                                        Add new resident
                                                    </button>
                                                    {movingToId !== a.id && (
                                                        <button className="btn-link" type="button"
                                                                onClick={() => startMoving(a.id)}>
                                                            Add existing resident
                                                        </button>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}
