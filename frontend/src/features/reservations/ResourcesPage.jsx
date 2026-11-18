import { useState } from "react";
import { StatusDot } from "../../components/Indicators";
import ConfirmDialog from "../../components/ConfirmDialog";
import Drawer from "../../components/Drawer";
import EmptyState from "../../components/EmptyState";
import Menu from "../../components/Menu";
import Notice from "../../components/Notice";
import PageHeader from "../../components/PageHeader";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAsync } from "../../hooks/useAsync";
import { useDirectory } from "../../hooks/useDirectory";
import { useAuth } from "../../auth/useAuth";
import { blockResource, getReservationsForResource, getResources, setResourceStatus, unblockResource } from "../../api/reservations";
import { formatRelativeDay } from "../../lib/format";
import { TYPE_LABELS, formatDay, formatTime, isBlockedNow, upcomingBlock } from "./format";

function statusOf(r) {
    if (r.status === "MAINTENANCE") return { label: "Closed", tone: "warning", note: "Until further notice" };
    const block = upcomingBlock(r);
    if (block && isBlockedNow(r)) return { label: "Closed", tone: "warning", note: `Until ${formatDay(block.to)} ${formatTime(block.to)}` };
    if (block) return { label: "Open", tone: "success", note: `Closed ${formatDay(block.from)} – ${formatDay(block.to)}` };
    return { label: "Open", tone: "success", note: null };
}

function BlockForm({ onSubmit, onCancel }) {
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [error, setError] = useState("");
    const [saving, setSaving] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        if (from >= to) {
            setError("The end has to be after the start.");
            return;
        }
        setError("");
        setSaving(true);
        try {
            await onSubmit(from, to);
        } catch {
            setError("Couldn't block the resource.");
            setSaving(false);
        }
    }

    return (
        <form onSubmit={handleSubmit}>
            <div className="field">
                <label className="label" htmlFor="block-from">From</label>
                <input id="block-from" className="input" type="datetime-local" value={from}
                       onChange={(e) => setFrom(e.target.value)} required />
            </div>
            <div className="field">
                <label className="label" htmlFor="block-to">To</label>
                <input id="block-to" className="input" type="datetime-local" value={to}
                       onChange={(e) => setTo(e.target.value)} required />
                <p className="hint">Residents can't book it in this period. Existing bookings are kept.</p>
            </div>
            {error && <Notice kind="error">{error}</Notice>}
            <div className="form-actions">
                <button className="btn" type="submit" disabled={saving}>{saving ? "Saving…" : "Block"}</button>
                <button className="btn btn--secondary" type="button" onClick={onCancel}>Cancel</button>
            </div>
        </form>
    );
}

function ResourceBookings({ token, resource, dir }) {
    const { data, error, loading } = useAsync(() => getReservationsForResource(token, resource.id), [token, resource.id]);

    if (error) return <Notice kind="error">Couldn't load the bookings.</Notice>;
    if (loading) return <Skeleton rows={3} />;

    const upcoming = data
        .filter((r) => new Date(r.endTime) > new Date())
        .sort((a, b) => a.startTime.localeCompare(b.startTime));
    if (upcoming.length === 0) return <EmptyState compact>No upcoming bookings.</EmptyState>;

    return (
        <ul className="item-list card card--flush">
            {upcoming.map((r) => (
                <li key={r.id} className="item">
                    <div className="item__body">
                        <div className="item__title">{formatRelativeDay(r.startTime)} · {formatTime(r.startTime)}–{formatTime(r.endTime)}</div>
                        <div className="item__meta">{dir.residentName(r.residentId)} · {dir.residentApartment(r.residentId)}</div>
                    </div>
                </li>
            ))}
        </ul>
    );
}

export default function ResourcesPage() {
    const { token } = useAuth();
    const toast = useToast();
    const dir = useDirectory(token);
    const { data: resources, error, loading, reload } = useAsync(() => getResources(token), [token]);
    const [viewing, setViewing] = useState(null);
    const [blocking, setBlocking] = useState(null);
    const [closing, setClosing] = useState(null);

    async function run(action, message) {
        try {
            await action();
            toast(message);
            reload();
        } catch {
            toast("Couldn't update the resource", { kind: "error" });
        }
    }

    async function handleBlock(from, to) {
        await blockResource(token, blocking.id, from, to);
        toast(`${blocking.name} blocked ${formatDay(from)} – ${formatDay(to)}`);
        setBlocking(null);
        reload();
    }

    return (
        <>
            <PageHeader title="Resources" subtitle="Shared spaces residents can book, and when they're closed." />

            {error && <Notice kind="error">Couldn't load the resources.</Notice>}
            {loading && <Skeleton rows={4} />}
            {resources && resources.length === 0 && (
                <EmptyState title="No shared resources">Resources such as guest parking and the common room show up here.</EmptyState>
            )}

            {resources && resources.length > 0 && (
                <div className="table-wrap">
                    <table className="table">
                        <thead>
                            <tr>
                                <th>Resource</th>
                                <th>Status</th>
                                <th className="actions"><span className="sr-only">Actions</span></th>
                            </tr>
                        </thead>
                        <tbody>
                            {resources.map((r) => {
                                const s = statusOf(r);
                                return (
                                    <tr key={r.id}>
                                        <td>
                                            <div className="table__primary">{r.name}</div>
                                            <div className="table__sub">{TYPE_LABELS[r.type] ?? r.type}</div>
                                        </td>
                                        <td>
                                            <StatusDot tone={s.tone}>{s.label}</StatusDot>
                                            {s.note && <div className="table__sub" style={{ marginTop: 2 }}>{s.note}</div>}
                                        </td>
                                        <td className="actions">
                                            <span style={{ display: "inline-flex", gap: 4, alignItems: "center" }}>
                                                <button type="button" className="btn btn--ghost btn--sm" onClick={() => setViewing(r)}>
                                                    Bookings
                                                </button>
                                                <Menu items={[
                                                    { label: "Block for a period", onClick: () => setBlocking(r) },
                                                    r.from && {
                                                        label: "Remove scheduled block",
                                                        onClick: () => run(() => unblockResource(token, r.id), `Block removed from ${r.name}`),
                                                    },
                                                    "separator",
                                                    r.status === "MAINTENANCE"
                                                        ? { label: "Reopen", onClick: () => run(() => setResourceStatus(token, r.id, "AVAILABLE"), `${r.name} reopened`) }
                                                        : { label: "Close until further notice", danger: true, onClick: () => setClosing(r) },
                                                ]} />
                                            </span>
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}

            <Drawer open={Boolean(viewing)} title={viewing ? `Bookings · ${viewing.name}` : ""} onClose={() => setViewing(null)}>
                {viewing && <ResourceBookings token={token} resource={viewing} dir={dir} />}
            </Drawer>

            <Drawer open={Boolean(blocking)} title={blocking ? `Block ${blocking.name}` : ""} onClose={() => setBlocking(null)}>
                {blocking && <BlockForm onSubmit={handleBlock} onCancel={() => setBlocking(null)} />}
            </Drawer>

            <ConfirmDialog open={Boolean(closing)} title={closing ? `Close ${closing.name}?` : ""} confirmLabel="Close" danger
                           onConfirm={async () => { await run(() => setResourceStatus(token, closing.id, "MAINTENANCE"), `${closing.name} closed`); setClosing(null); }}
                           onCancel={() => setClosing(null)}>
                Residents can't book it until you reopen it. Existing bookings are kept.
            </ConfirmDialog>
        </>
    );
}
