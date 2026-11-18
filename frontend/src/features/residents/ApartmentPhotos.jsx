import { useEffect, useRef, useState } from "react";
import ConfirmDialog from "../../components/ConfirmDialog";
import Icon from "../../components/Icon";
import Skeleton from "../../components/Skeleton";
import { useToast } from "../../components/toastContext";
import { useAuth } from "../../auth/useAuth";
import { getApartmentPhotoBlob, getApartmentPhotos, removeApartmentPhoto, uploadApartmentPhoto } from "../../api/residents";

const ACCEPT = "image/jpeg,image/png,image/webp";
const MAX_BYTES = 5 * 1024 * 1024;
const MAX_PHOTOS = 10;

/** One photo, fetched with the token (an <img src> can't send it) and shown from an object URL. */
function Photo({ apartmentId, photo, onRemove }) {
    const { token } = useAuth();
    const [src, setSrc] = useState(null);
    const [failed, setFailed] = useState(false);

    useEffect(() => {
        let cancelled = false;
        let url = null;
        getApartmentPhotoBlob(token, apartmentId, photo.id)
            .then((blob) => {
                if (cancelled) return;
                url = URL.createObjectURL(blob);
                setSrc(url);
            })
            .catch(() => { if (!cancelled) setFailed(true); });
        return () => {
            cancelled = true;
            if (url) URL.revokeObjectURL(url);
        };
    }, [token, apartmentId, photo.id]);

    return (
        <li className="photo">
            {src && <img className="photo__img" src={src} alt={photo.fileName} />}
            {!src && <span className="photo__placeholder">{failed ? "Couldn't load" : ""}</span>}
            <button type="button" className="btn btn--secondary btn--icon photo__remove" aria-label={`Remove ${photo.fileName}`}
                    onClick={() => onRemove(photo)}>
                <Icon name="x" size={16} />
            </button>
        </li>
    );
}

export default function ApartmentPhotos({ apartmentId }) {
    const { token } = useAuth();
    const toast = useToast();
    const [photos, setPhotos] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [removing, setRemoving] = useState(null);
    const input = useRef(null);

    useEffect(() => {
        let cancelled = false;
        getApartmentPhotos(token, apartmentId)
            .then((data) => { if (!cancelled) setPhotos(data); })
            .catch(() => { if (!cancelled) setPhotos([]); });
        return () => { cancelled = true; };
    }, [token, apartmentId]);

    async function handleFiles(event) {
        const files = Array.from(event.target.files);
        event.target.value = "";
        setUploading(true);
        let added = 0;
        for (const file of files) {
            if (file.size > MAX_BYTES) {
                toast(`${file.name} is larger than 5 MB`, { kind: "error" });
                continue;
            }
            try {
                const saved = await uploadApartmentPhoto(token, apartmentId, file);
                setPhotos((current) => [...(current ?? []), saved]);
                added++;
            } catch (e) {
                toast({
                    409: `An apartment can have at most ${MAX_PHOTOS} photos`,
                    413: `${file.name} is larger than 5 MB`,
                    415: `${file.name} isn't a JPEG, PNG or WebP image`,
                }[e.status] ?? `Couldn't upload ${file.name}`, { kind: "error" });
                break;
            }
        }
        if (added > 0) toast(added === 1 ? "Photo added" : `${added} photos added`);
        setUploading(false);
    }

    async function handleRemove() {
        try {
            await removeApartmentPhoto(token, apartmentId, removing.id);
            setPhotos((current) => current.filter((p) => p.id !== removing.id));
            toast("Photo removed");
        } catch {
            toast("Couldn't remove the photo", { kind: "error" });
        }
        setRemoving(null);
    }

    if (photos === null) return <Skeleton rows={1} />;

    return (
        <>
            <ul className="photos">
                {photos.map((p) => (
                    <Photo key={p.id} apartmentId={apartmentId} photo={p} onRemove={setRemoving} />
                ))}
                {photos.length < MAX_PHOTOS && (
                    <li>
                        <input ref={input} type="file" accept={ACCEPT} multiple hidden onChange={handleFiles} />
                        <button type="button" className="photo-add" disabled={uploading} onClick={() => input.current.click()}
                                style={{ width: "100%" }}>
                            <Icon name="photo" size={22} />
                            <span className="small">{uploading ? "Uploading…" : "Add photos"}</span>
                        </button>
                    </li>
                )}
            </ul>
            <p className="hint">JPEG, PNG or WebP, up to 5 MB each, at most {MAX_PHOTOS} photos.</p>

            <ConfirmDialog open={Boolean(removing)} title="Remove this photo?" confirmLabel="Remove" danger
                           onConfirm={handleRemove} onCancel={() => setRemoving(null)} />
        </>
    );
}
