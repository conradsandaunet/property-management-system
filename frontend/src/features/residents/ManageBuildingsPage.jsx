import { useState } from "react";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import { describeError } from "./errors";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import { createBuilding, getBuildings, removeBuilding, updateBuilding } from "../../api/residents";

function BuildingForm({ initial, onSubmit, onCancel }) {
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
        <form className="form-card" onSubmit={handleSubmit}>
            <div className="field">
                <label className="label" htmlFor="building-name">Building name</label>
                <input id="building-name" className="input" value={name} maxLength={255} required
                       onChange={(e) => setName(e.target.value)} />
            </div>
            {error && <Notice kind="error">{error}</Notice>}
            <div className="form-actions">
                <button className="btn" type="submit" disabled={submitting}>
                    {submitting ? "Saving…" : initial ? "Save changes" : "Add building"}
                </button>
                <button className="btn-link" type="button" onClick={onCancel}>Cancel</button>
            </div>
        </form>
    );
}

export default function ManageBuildingsPage() {
    const { token } = useAuth();
    const { data: buildings, error: loadError, loading, reload } = useAsync(() => getBuildings(token), [token]);
    const [adding, setAdding] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [confirmingId, setConfirmingId] = useState(null);
    const [error, setError] = useState("");

    async function handleCreate(values) {
        await createBuilding(token, values);
        setAdding(false);
        reload();
    }

    async function handleUpdate(id, values) {
        await updateBuilding(token, id, values);
        setEditingId(null);
        reload();
    }

    async function handleRemove(id) {
        try {
            await removeBuilding(token, id);
            setError("");
        } catch (e) {
            setError(describeError(e, "Couldn't remove the building.", {
                409: "This building still has apartments. Move or remove them first.",
            }));
        }
        setConfirmingId(null);
        reload();
    }

    return (
        <>
            <PageHeader eyebrow="Board" title="Manage buildings" />

            <div className="section-gap">
                {adding ? (
                    <BuildingForm onSubmit={handleCreate} onCancel={() => setAdding(false)} />
                ) : (
                    <button className="btn" type="button" onClick={() => { setAdding(true); setEditingId(null); }}>
                        Add building
                    </button>
                )}
            </div>

            {(error || loadError) && (
                <Notice kind="error">{error || describeError(loadError, "Couldn't load buildings.")}</Notice>
            )}
            {loading && <Notice>Loading...</Notice>}
            {buildings && buildings.length === 0 && <Notice>No buildings yet.</Notice>}

            {buildings && buildings.length > 0 && (
                <ul className="list list--wide">
                    {buildings.map((b) => (
                        <li key={b.id} className="list-item">
                            {editingId === b.id ? (
                                <BuildingForm initial={b} onSubmit={(values) => handleUpdate(b.id, values)}
                                              onCancel={() => setEditingId(null)} />
                            ) : (
                                <>
                                    <div className="list-item__top">
                                        <span className="list-item__title">{b.name}</span>
                                        <span className="badges">
                                            <Badge variant={b.apartmentCount > 0 ? "success" : undefined}>
                                                {b.apartmentCount} apartment{b.apartmentCount === 1 ? "" : "s"}
                                            </Badge>
                                        </span>
                                    </div>
                                    <div className="list-item__actions">
                                        <button className="btn-link" type="button"
                                                onClick={() => { setEditingId(b.id); setAdding(false); setConfirmingId(null); }}>
                                            Rename
                                        </button>
                                        {confirmingId !== b.id && (
                                            <button className="btn-link btn-link--danger" type="button"
                                                    onClick={() => setConfirmingId(b.id)}>
                                                Remove
                                            </button>
                                        )}
                                        {confirmingId === b.id && (
                                            <span className="list-item__confirm">
                                                Remove {b.name}?
                                                <button className="btn-link btn-link--danger" type="button"
                                                        onClick={() => handleRemove(b.id)}>
                                                    Yes, remove
                                                </button>
                                                <button className="btn-link" type="button"
                                                        onClick={() => setConfirmingId(null)}>
                                                    Cancel
                                                </button>
                                            </span>
                                        )}
                                    </div>
                                </>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}
