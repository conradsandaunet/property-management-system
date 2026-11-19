import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import Badge from "../../components/Badge";
import ConfirmDialog from "../../components/ConfirmDialog";
import Drawer from "../../components/Drawer";
import EmptyState from "../../components/EmptyState";
import Menu from "../../components/Menu";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import {
    createResident,
    getApartmentResidents,
    getApartments,
    getBuildings,
    removeApartment,
    searchResidents,
    updateApartment,
    updateResident,
} from "../../api/residents";
import { apartmentLabel, initials } from "../../lib/format";
import ApartmentForm from "../residents/ApartmentForm";
import ApartmentPhotos from "../residents/ApartmentPhotos";
import ResidentForm from "../residents/ResidentForm";

function MoveResidentHere({ token, apartment, apartments, onMove }) {
    const [query, setQuery] = useState("");
    const [candidates, setCandidates] = useState(null);

    useEffect(() => {
        let cancelled = false;
        const timer = setTimeout(() => {
            searchResidents(token, query, false)
                .then((data) => { if (!cancelled) setCandidates(data.filter((r) => r.apartmentId !== apartment.id)); })
                .catch(() => { if (!cancelled) setCandidates([]); });
        }, query ? 250 : 0);
        return () => { cancelled = true; clearTimeout(timer); };
    }, [token, query, apartment.id]);

    const label = (id) => apartmentLabel(apartments.find((a) => a.id === id));

    return (
        <>
            <div className="field">
                <label className="label" htmlFor="move-search">Find a resident</label>
                <input id="move-search" className="input" type="search" value={query} placeholder="Name or apartment number"
                       onChange={(e) => setQuery(e.target.value)} />
            </div>
            {candidates === null && <Skeleton rows={3} />}
            {candidates && candidates.length === 0 && <EmptyState compact>No other residents found.</EmptyState>}
            {candidates && candidates.length > 0 && (
                <ul className="item-list card card--flush">
                    {candidates.map((r) => (
                        <li key={r.id} className="item">
                            <div className="item__body">
                                <div className="item__title">{r.firstName} {r.lastName}</div>
                                <div className="item__meta">Now in {label(r.apartmentId) || r.apartmentNumber}</div>
                            </div>
                            <button type="button" className="btn btn--secondary btn--sm" onClick={() => onMove(r)}>
                                Move here
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
}

export default function ApartmentDetailPage() {
    const { id: idParam } = useParams();
    const id = Number(idParam);
    const navigate = useNavigate();
    const toast = useToast();
    const { token } = useAuth();
    const apartments = useAsync(() => getApartments(token), [token]);
    const buildings = useAsync(() => getBuildings(token), [token]);
    const residents = useAsync(() => getApartmentResidents(token, id), [token, id]);
    const [drawer, setDrawer] = useState(null); // "edit" | "newResident" | "move"
    const [confirmRemove, setConfirmRemove] = useState(false);

    const apartment = apartments.data?.find((a) => a.id === id);
    const close = () => setDrawer(null);

    async function handleEdit(values) {
        await updateApartment(token, id, values);
        close();
        toast("Apartment updated");
        apartments.reload();
    }

    async function handleNewResident(values) {
        await createResident(token, values);
        close();
        toast(`${values.firstName} ${values.lastName} moved in`);
        residents.reload();
        apartments.reload();
    }

    async function handleMove(resident) {
        try {
            await updateResident(token, resident.id, { apartmentId: id });
            toast(`${resident.firstName} ${resident.lastName} moved to ${apartment.apartmentNumber}`);
            close();
            residents.reload();
            apartments.reload();
        } catch {
            toast("Couldn't move the resident", { kind: "error" });
        }
    }

    async function handleRemove() {
        try {
            await removeApartment(token, id);
            toast(`Apartment ${apartment.apartmentNumber} removed`);
            navigate("/board/property", { replace: true });
        } catch (e) {
            toast(e.status === 409 ? "Move or remove the residents first" : "Couldn't remove the apartment", { kind: "error" });
            setConfirmRemove(false);
        }
    }

    if (apartments.error) return <Notice kind="error" onRetry={apartments.reload}>Couldn't load the apartment.</Notice>;
    if (apartments.loading) return <Skeleton rows={4} />;
    if (!apartment) {
        return (
            <EmptyState title="Apartment not found" action={<Link className="btn" to="/board/property">Back to property</Link>}>
                It may have been removed.
            </EmptyState>
        );
    }

    const list = residents.data ?? [];

    return (
        <>
            <PageHeader
                breadcrumbs={[
                    { label: "Property", to: "/board/property" },
                    { label: apartment.buildingName, to: `/board/property/buildings/${apartment.buildingId}` },
                ]}
                title={`Apartment ${apartment.apartmentNumber}`}
                subtitle={`Floor ${apartment.floor} · ${apartment.buildingName}`}
                actions={(
                    <Menu items={[
                        { label: "Edit apartment", onClick: () => setDrawer("edit") },
                        { label: "Remove apartment", danger: true, onClick: () => setConfirmRemove(true) },
                    ]} />
                )}
            />

            <section className="section">
                <div className="section__head">
                    <h2 className="section__title">
                        Residents {list.length === 0 && residents.data && <Badge>Vacant</Badge>}
                    </h2>
                    <span style={{ display: "inline-flex", gap: 8 }}>
                        <button type="button" className="btn btn--secondary btn--sm" onClick={() => setDrawer("move")}>
                            Move someone here
                        </button>
                        <button type="button" className="btn btn--sm" onClick={() => setDrawer("newResident")}>
                            New resident
                        </button>
                    </span>
                </div>
                {residents.loading && <Skeleton rows={2} />}
                {residents.error && <Notice kind="error" onRetry={residents.reload}>Couldn't load the residents.</Notice>}
                {residents.data && list.length === 0 && (
                    <EmptyState compact>No one lives here yet. Add a new resident or move someone in.</EmptyState>
                )}
                {list.length > 0 && (
                    <ul className="item-list card card--flush">
                        {list.map((r) => (
                            <li key={r.id} className="item">
                                <span className="avatar" aria-hidden="true">{initials(r.firstName, r.lastName)}</span>
                                <div className="item__body">
                                    <div className="item__title">
                                        {r.firstName} {r.lastName} {r.manager && <Badge variant="info">Board</Badge>}
                                    </div>
                                    <div className="item__meta">
                                        <span>{r.email}</span>
                                        {r.phone && <span>{r.phone}</span>}
                                    </div>
                                </div>
                            </li>
                        ))}
                    </ul>
                )}
            </section>

            <section className="section">
                <div className="section__head"><h2 className="section__title">Photos</h2></div>
                <ApartmentPhotos apartmentId={id} />
            </section>

            <Drawer open={drawer === "edit"} title="Edit apartment" onClose={close}>
                <ApartmentForm mode="edit" initial={apartment} buildings={buildings.data ?? []} onSubmit={handleEdit} onCancel={close} />
            </Drawer>
            <Drawer open={drawer === "newResident"} title={`New resident in ${apartment.apartmentNumber}`} onClose={close}>
                <ResidentForm mode="create" initial={{ apartmentId: id }} apartments={[apartment]} lockApartment
                              onSubmit={handleNewResident} onCancel={close} />
            </Drawer>
            <Drawer open={drawer === "move"} title={`Move someone to ${apartment.apartmentNumber}`} onClose={close}>
                <MoveResidentHere token={token} apartment={apartment} apartments={apartments.data} onMove={handleMove} />
            </Drawer>

            <ConfirmDialog open={confirmRemove} title={`Remove apartment ${apartment.apartmentNumber}?`} confirmLabel="Remove" danger
                           onConfirm={handleRemove} onCancel={() => setConfirmRemove(false)}>
                {list.length > 0
                    ? "It still has residents. Move them out first, or removing will fail."
                    : "This also deletes its photos. It can't be undone."}
            </ConfirmDialog>
        </>
    );
}
