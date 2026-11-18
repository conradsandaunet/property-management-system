import { useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ConfirmDialog from "../../components/ConfirmDialog";
import Drawer from "../../components/Drawer";
import EmptyState from "../../components/EmptyState";
import FacadeImage from "../../components/FacadeImage";
import Icon from "../../components/Icon";
import Menu from "../../components/Menu";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useAuth } from "../../auth/useAuth";
import {
    createApartment,
    getApartments,
    getBuildings,
    removeBuilding,
    removeBuildingFacade,
    setBuildingFacade,
    updateBuilding,
} from "../../api/residents";
import ApartmentForm from "../residents/ApartmentForm";
import BuildingForm from "../residents/BuildingForm";

const ACCEPT = "image/jpeg,image/png,image/webp";

export default function BuildingPage() {
    const id = Number(useParams().id);
    const { token } = useAuth();
    const toast = useToast();
    const navigate = useNavigate();
    const buildings = useAsync(() => getBuildings(token), [token]);
    const apartments = useAsync(() => getApartments(token), [token]);
    const [drawer, setDrawer] = useState(null); // "apartment" | "rename"
    const [confirm, setConfirm] = useState(null); // "building" | "facade"
    const [facadeVersion, setFacadeVersion] = useState(0);
    const [uploading, setUploading] = useState(false);
    const fileInput = useRef(null);

    const building = buildings.data?.find((b) => b.id === id);
    const list = (apartments.data ?? [])
        .filter((a) => a.buildingId === id)
        .sort((a, b) => a.apartmentNumber.localeCompare(b.apartmentNumber, undefined, { numeric: true }));
    const vacant = list.filter((a) => a.residentCount === 0).length;
    const close = () => setDrawer(null);

    async function handleFacade(event) {
        const file = event.target.files[0];
        event.target.value = "";
        if (!file) return;
        if (file.size > 5 * 1024 * 1024) {
            toast("The photo must be at most 5 MB", { kind: "error" });
            return;
        }
        setUploading(true);
        try {
            await setBuildingFacade(token, id, file);
            toast("Facade photo updated");
            setFacadeVersion((v) => v + 1);
            buildings.reload();
        } catch (e) {
            toast(e.status === 415 ? "Use a JPEG, PNG or WebP image" : "Couldn't upload the photo", { kind: "error" });
        } finally {
            setUploading(false);
        }
    }

    async function handleNewApartment(values) {
        const created = await createApartment(token, values);
        close();
        toast(`Apartment ${created.apartmentNumber} added`);
        apartments.reload();
        buildings.reload();
    }

    async function handleRename(values) {
        await updateBuilding(token, id, values);
        close();
        toast("Building renamed");
        buildings.reload();
        apartments.reload();
    }

    async function handleConfirm() {
        try {
            if (confirm === "building") {
                await removeBuilding(token, id);
                toast(`${building.name} removed`);
                navigate("/board/property", { replace: true });
                return;
            }
            await removeBuildingFacade(token, id);
            toast("Facade photo removed");
            buildings.reload();
        } catch (e) {
            toast(e.status === 409 ? "Move or remove its apartments first" : "Something went wrong", { kind: "error" });
        }
        setConfirm(null);
    }

    if (buildings.error) return <Notice kind="error">Couldn't load the building.</Notice>;
    if (buildings.loading) return <Skeleton rows={4} />;
    if (!building) {
        return (
            <EmptyState title="Building not found" action={<Link className="btn" to="/board/property">Back to property</Link>}>
                It may have been removed.
            </EmptyState>
        );
    }

    return (
        <>
            <PageHeader
                breadcrumbs={[{ label: "Property", to: "/board/property" }]}
                title={building.name}
                subtitle={`${list.length} apartment${list.length === 1 ? "" : "s"} · ${vacant} vacant`}
                actions={(
                    <>
                        <button className="btn" type="button" onClick={() => setDrawer("apartment")}>
                            <Icon name="plus" size={16} /> New apartment
                        </button>
                        <Menu items={[
                            { label: "Rename", onClick: () => setDrawer("rename") },
                            { label: building.hasFacade ? "Change facade photo" : "Add facade photo", onClick: () => fileInput.current.click() },
                            building.hasFacade && { label: "Remove facade photo", onClick: () => setConfirm("facade") },
                            "separator",
                            { label: "Remove building", danger: true, onClick: () => setConfirm("building") },
                        ]} />
                    </>
                )}
            />

            <input ref={fileInput} type="file" accept={ACCEPT} hidden onChange={handleFacade} />
            {building.hasFacade ? (
                <FacadeImage buildingId={id} version={facadeVersion} alt={`${building.name} facade`} className="facade facade--hero" />
            ) : (
                <button type="button" className="facade-empty" disabled={uploading} onClick={() => fileInput.current.click()}>
                    <Icon name="photo" size={24} />
                    {uploading ? "Uploading…" : "Add a facade photo"}
                </button>
            )}

            <section className="section">
                <div className="section__head"><h2 className="section__title">Apartments</h2></div>
                {apartments.loading && <Skeleton rows={4} />}
                {apartments.data && list.length === 0 && (
                    <EmptyState compact>No apartments in this building yet.</EmptyState>
                )}
                {list.length > 0 && (
                    <div className="table-wrap">
                        <table className="table">
                            <thead>
                                <tr>
                                    <th>Apartment</th>
                                    <th>Floor</th>
                                    <th>Residents</th>
                                    <th className="actions"><span className="sr-only">Open</span></th>
                                </tr>
                            </thead>
                            <tbody>
                                {list.map((a) => (
                                    <tr key={a.id} className="is-clickable" onClick={() => navigate(`/board/property/apartments/${a.id}`)}>
                                        <td className="table__primary">
                                            <Link to={`/board/property/apartments/${a.id}`} onClick={(e) => e.stopPropagation()}
                                                  style={{ color: "inherit", textDecoration: "none" }}>
                                                {a.apartmentNumber}
                                            </Link>
                                        </td>
                                        <td>{a.floor}</td>
                                        <td>
                                            {a.residentCount > 0
                                                ? `${a.residentCount} resident${a.residentCount > 1 ? "s" : ""}`
                                                : <span className="muted">Vacant</span>}
                                        </td>
                                        <td className="actions muted"><Icon name="chevron" size={16} /></td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </section>

            <Drawer open={drawer === "apartment"} title={`New apartment in ${building.name}`} onClose={close}>
                <ApartmentForm mode="create" initial={{ buildingId: id }} buildings={buildings.data}
                               onSubmit={handleNewApartment} onCancel={close} />
            </Drawer>
            <Drawer open={drawer === "rename"} title="Rename building" onClose={close}>
                <BuildingForm initial={building} onSubmit={handleRename} onCancel={close} />
            </Drawer>

            <ConfirmDialog open={Boolean(confirm)} danger confirmLabel="Remove"
                           title={confirm === "building" ? `Remove ${building.name}?` : "Remove the facade photo?"}
                           onConfirm={handleConfirm} onCancel={() => setConfirm(null)}>
                {confirm === "building" && (list.length > 0
                    ? "It still has apartments. Remove or move them first, or removing will fail."
                    : "This can't be undone.")}
            </ConfirmDialog>
        </>
    );
}
