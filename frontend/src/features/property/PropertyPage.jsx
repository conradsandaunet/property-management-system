import { useEffect, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import FacadeImage from "../../components/FacadeImage";
import Badge from "../../components/Badge";
import ConfirmDialog from "../../components/ConfirmDialog";
import Drawer from "../../components/Drawer";
import EmptyState from "../../components/EmptyState";
import Icon from "../../components/Icon";
import Menu from "../../components/Menu";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import {
    createBuilding,
    createResident,
    getApartments,
    getBuildings,
    removeResident,
    searchResidents,
    updateResident,
} from "../../api/residents";
import { apartmentLabel } from "../../lib/format";
import BuildingForm from "../residents/BuildingForm";
import ResidentForm from "../residents/ResidentForm";

const TABS = [
    { value: "buildings", label: "Buildings" },
    { value: "residents", label: "Residents" },
];

const byNumber = (a, b) =>
    a.buildingName.localeCompare(b.buildingName)
    || a.apartmentNumber.localeCompare(b.apartmentNumber, undefined, { numeric: true });

function BuildingCards({ buildings, apartments, onAdd }) {
    if (buildings.error) return <Notice kind="error">Couldn't load the buildings.</Notice>;
    if (buildings.loading) return <Skeleton rows={3} />;
    if (buildings.data.length === 0) {
        return (
            <EmptyState title="No buildings yet" action={<button className="btn" type="button" onClick={onAdd}>Add building</button>}>
                Add a building, then its apartments and the people who live there.
            </EmptyState>
        );
    }

    const inBuilding = (id) => (apartments.data ?? []).filter((a) => a.buildingId === id);

    return (
        <div className="building-cards">
            {buildings.data.map((b) => {
                const apts = inBuilding(b.id);
                const vacant = apts.filter((a) => a.residentCount === 0).length;
                const people = apts.reduce((sum, a) => sum + a.residentCount, 0);
                return (
                    <Link key={b.id} className="building-card" to={`buildings/${b.id}`}>
                        {b.hasFacade
                            ? <FacadeImage buildingId={b.id} alt={`${b.name} facade`} />
                            : <div className="facade"><span className="facade__placeholder"><Icon name="building" size={28} /></span></div>}
                        <div className="building-card__body">
                            <div className="building-card__name">{b.name}</div>
                            <div className="small muted">
                                {b.apartmentCount} apartment{b.apartmentCount === 1 ? "" : "s"}
                                {apartments.data && ` · ${vacant} vacant · ${people} resident${people === 1 ? "" : "s"}`}
                            </div>
                        </div>
                    </Link>
                );
            })}
        </div>
    );
}

function ResidentsTab({ token, apartments, reloadKey, onEdit, onRemove, onReactivate }) {
    const { me } = useAuth();
    const [query, setQuery] = useState("");
    const [includeRemoved, setIncludeRemoved] = useState(false);
    const [residents, setResidents] = useState(null);
    const [error, setError] = useState(false);

    useEffect(() => {
        let cancelled = false;
        const timer = setTimeout(() => {
            searchResidents(token, query, includeRemoved)
                .then((data) => { if (!cancelled) { setResidents(data); setError(false); } })
                .catch(() => { if (!cancelled) setError(true); });
        }, query ? 250 : 0);
        return () => { cancelled = true; clearTimeout(timer); };
    }, [token, query, includeRemoved, reloadKey]);

    const apt = (id) => apartments.find((a) => a.id === id);

    return (
        <>
            <div className="toolbar">
                <input className="input input--search" type="search" placeholder="Name or apartment number"
                       aria-label="Search residents" value={query} onChange={(e) => setQuery(e.target.value)} />
                <label className="check">
                    <input type="checkbox" checked={includeRemoved} onChange={(e) => setIncludeRemoved(e.target.checked)} />
                    Show removed
                </label>
            </div>

            {error && <Notice kind="error">Couldn't load the residents.</Notice>}
            {residents === null && !error && <Skeleton rows={5} />}
            {residents && residents.length === 0 && <EmptyState compact>No residents match.</EmptyState>}

            {residents && residents.length > 0 && (
                <div className="table-wrap">
                    <table className="table">
                        <thead>
                            <tr>
                                <th>Name</th>
                                <th className="hide-sm">Contact</th>
                                <th>Apartment</th>
                                <th className="actions"><span className="sr-only">Actions</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            {residents.map((r) => {
                                const isSelf = r.id === me.id;
                                return (
                                    <tr key={r.id} style={r.active ? undefined : { opacity: 0.6 }}>
                                        <td>
                                            <span className="table__primary">{r.firstName} {r.lastName}</span>
                                            {isSelf && <span className="muted"> (you)</span>}{" "}
                                            <span className="badges" style={{ verticalAlign: "middle" }}>
                                                {r.manager && <Badge variant="info">Board</Badge>}
                                                {!r.active && <Badge>Removed</Badge>}
                                            </span>
                                        </td>
                                        <td className="hide-sm">
                                            <div>{r.email}</div>
                                            {r.phone && <div className="table__sub">{r.phone}</div>}
                                        </td>
                                        <td>
                                            <Link to={`apartments/${r.apartmentId}`}>
                                                {apartmentLabel(apt(r.apartmentId) ?? { apartmentNumber: r.apartmentNumber })}
                                            </Link>
                                        </td>
                                        <td className="actions">
                                            <Menu items={r.active ? [
                                                { label: "Edit", onClick: () => onEdit(r) },
                                                !isSelf && { label: "Remove", danger: true, onClick: () => onRemove(r) },
                                            ] : [
                                                { label: "Reactivate", onClick: () => onReactivate(r) },
                                            ]} />
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}
        </>
    );
}

export default function PropertyPage() {
    const { token, me, refreshMe } = useAuth();
    const toast = useToast();
    const navigate = useNavigate();
    const [params, setParams] = useSearchParams();
    const tab = params.get("tab") === "residents" ? "residents" : "buildings";
    const apartments = useAsync(() => getApartments(token), [token]);
    const buildings = useAsync(() => getBuildings(token), [token]);
    const [residentsKey, setResidentsKey] = useState(0);
    // { kind: "apartment" | "resident" | "building", item? }
    const [drawer, setDrawer] = useState(null);
    const [confirm, setConfirm] = useState(null);

    const close = () => setDrawer(null);
    const reloadAll = () => { apartments.reload(); buildings.reload(); setResidentsKey((k) => k + 1); };

    async function saveResident(values) {
        if (drawer.item) {
            await updateResident(token, drawer.item.id, values);
            if (drawer.item.id === me.id) refreshMe();
            toast(`${values.firstName} ${values.lastName} updated`);
        } else {
            await createResident(token, values);
            toast(`${values.firstName} ${values.lastName} added`);
        }
        close();
        reloadAll();
    }

    async function saveBuilding(values) {
        const created = await createBuilding(token, values);
        close();
        toast(`${values.name} added. Add its apartments here.`);
        navigate(`buildings/${created.id}`);
    }

    async function runConfirm() {
        const { item } = confirm;
        try {
            await removeResident(token, item.id);
            toast(`${item.firstName} ${item.lastName} removed`);
            reloadAll();
        } catch (e) {
            toast(e.status === 409 ? "You can't remove yourself" : "Something went wrong", { kind: "error" });
        }
        setConfirm(null);
    }

    async function reactivate(r) {
        try {
            await updateResident(token, r.id, { active: true });
            toast(`${r.firstName} ${r.lastName} reactivated`);
            reloadAll();
        } catch {
            toast("Couldn't reactivate", { kind: "error" });
        }
    }

    const action = {
        residents: (
            <button className="btn" type="button" onClick={() => setDrawer({ kind: "resident" })}>
                <Icon name="plus" size={16} /> New resident
            </button>
        ),
        buildings: (
            <button className="btn" type="button" onClick={() => setDrawer({ kind: "building" })}>
                <Icon name="plus" size={16} /> New building
            </button>
        ),
    }[tab];

    const drawerTitle = drawer && {
        resident: drawer.item ? "Edit resident" : "New resident",
        building: "New building",
    }[drawer.kind];

    return (
        <>
            <PageHeader title="Property" subtitle="Buildings, apartments and the people who live there." actions={action} />

            <nav className="tabs" aria-label="Property">
                {TABS.map((t) => (
                    <button key={t.value} type="button" className={`tabs__link${tab === t.value ? " is-active" : ""}`}
                            aria-current={tab === t.value ? "page" : undefined}
                            onClick={() => setParams(t.value === "buildings" ? {} : { tab: t.value }, { replace: true })}>
                        {t.label}
                    </button>
                ))}
            </nav>

            {tab === "buildings" && (
                <BuildingCards buildings={buildings} apartments={apartments} onAdd={() => setDrawer({ kind: "building" })} />
            )}
            {tab === "residents" && (
                <ResidentsTab token={token} apartments={apartments.data ?? []} reloadKey={residentsKey}
                              onEdit={(r) => setDrawer({ kind: "resident", item: r })}
                              onRemove={(r) => setConfirm({ kind: "removeResident", item: r })}
                              onReactivate={reactivate} />
            )}

            <Drawer open={Boolean(drawer)} title={drawerTitle} onClose={close}>
                {drawer?.kind === "resident" && (
                    <ResidentForm mode={drawer.item ? "edit" : "create"} initial={drawer.item}
                                  apartments={(apartments.data ?? []).slice().sort(byNumber)}
                                  isSelf={drawer.item?.id === me.id} onSubmit={saveResident} onCancel={close} />
                )}
                {drawer?.kind === "building" && (
                    <BuildingForm onSubmit={saveBuilding} onCancel={close} />
                )}
            </Drawer>

            <ConfirmDialog
                open={Boolean(confirm)} danger
                title={`Remove ${confirm?.item.firstName} ${confirm?.item.lastName}?`}
                confirmLabel="Remove" onConfirm={runConfirm} onCancel={() => setConfirm(null)}>
                They can no longer log in. You can reactivate them later from "Show removed".
            </ConfirmDialog>
        </>
    );
}
