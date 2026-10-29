import { useState } from "react";
import Badge from "../../components/Badge";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import ApartmentForm from "./ApartmentForm";
import { describeError } from "./errors";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import {
    createApartment,
    getApartments,
    removeApartment,
    updateApartment,
} from "../../api/residents";

export default function ManageApartmentsPage() {
    const { token } = useAuth();
    const { data: apartments, error: loadError, loading, reload } =
        useAsync(() => getApartments(token), [token]);
    const [adding, setAdding] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [confirmingId, setConfirmingId] = useState(null);
    const [error, setError] = useState("");

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
                                </>
                            )}
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}
