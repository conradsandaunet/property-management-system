import { useEffect, useRef, useState } from "react";
import Notice from "../../components/Notice";
import { useAuth } from "../../auth/useAuth";
import { describeError } from "./errors";
import {
    getApartmentPhotoBlob,
    getApartmentPhotos,
    removeApartmentPhoto,
    uploadApartmentPhoto,
} from "../../api/residents";
import "./photos.css";

const ACCEPT = "image/jpeg,image/png,image/webp";
const MAX_BYTES = 5 * 1024 * 1024;

/** One photo. Fetched with the token and shown from an object URL that is freed on unmount. */
function Photo({ apartmentId, photo, onRemove }) {
    const { token } = useAuth();
    const [src, setSrc] = useState(null);
    const [failed, setFailed] = useState(false);
    const [confirming, setConfirming] = useState(false);

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
            <div className="photo__frame">
                {src && <img className="photo__img" src={src} alt={photo.fileName} />}
                {!src && <span className="photo__placeholder">{failed ? "Couldn't load" : "Loading..."}</span>}
            </div>
            <div className="photo__bar">
                <span className="photo__name" title={photo.fileName}>{photo.fileName}</span>
                {confirming ? (
                    <span>
                        <button className="btn-link btn-link--danger" type="button" onClick={() => onRemove(photo)}>
                            Yes, remove
                        </button>
                        <button className="btn-link" type="button" onClick={() => setConfirming(false)}>
                            Cancel
                        </button>
                    </span>
                ) : (
                    <button className="btn-link btn-link--danger" type="button" onClick={() => setConfirming(true)}>
                        Remove
                    </button>
                )}
            </div>
        </li>
    );
}

export default function ApartmentPhotos({ apartmentId }) {
    const { token } = useAuth();
    const [photos, setPhotos] = useState(null);
    const [error, setError] = useState("");
    const [uploading, setUploading] = useState(false);
    const input = useRef(null);

    useEffect(() => {
        let cancelled = false;
        getApartmentPhotos(token, apartmentId)
            .then((data) => { if (!cancelled) setPhotos(data); })
            .catch((e) => {
                if (!cancelled) setError(describeError(e, "Couldn't load the photos."));
            });
        return () => { cancelled = true; };
    }, [token, apartmentId]);

    async function handleFiles(event) {
        const files = Array.from(event.target.files);
        event.target.value = "";
        if (files.length === 0) return;

        setError("");
        setUploading(true);
        for (const file of files) {
            if (file.size > MAX_BYTES) {
                setError(`${file.name} is larger than 5 MB.`);
                continue;
            }
            try {
                const saved = await uploadApartmentPhoto(token, apartmentId, file);
                setPhotos((current) => [...(current ?? []), saved]);
            } catch (e) {
                setError(describeError(e, `Couldn't upload ${file.name}.`, {
                    409: "An apartment can have at most 10 photos.",
                    413: `${file.name} is larger than 5 MB.`,
                    415: `${file.name} isn't a JPEG, PNG or WebP image.`,
                }));
                break;
            }
        }
        setUploading(false);
    }

    async function handleRemove(photo) {
        try {
            await removeApartmentPhoto(token, apartmentId, photo.id);
            setPhotos((current) => current.filter((p) => p.id !== photo.id));
            setError("");
        } catch (e) {
            setError(describeError(e, "Couldn't remove the photo."));
        }
    }

    return (
        <div className="section-gap">
            {error && <Notice kind="error">{error}</Notice>}
            {photos === null && !error && <Notice>Loading...</Notice>}
            {photos && photos.length === 0 && <Notice>No photos yet.</Notice>}
            {photos && photos.length > 0 && (
                <ul className="photos">
                    {photos.map((p) => (
                        <Photo key={p.id} apartmentId={apartmentId} photo={p} onRemove={handleRemove} />
                    ))}
                </ul>
            )}
            <div className="list-item__actions">
                <input ref={input} type="file" accept={ACCEPT} multiple hidden onChange={handleFiles} />
                <button className="btn-link" type="button" disabled={uploading}
                        onClick={() => input.current.click()}>
                    {uploading ? "Uploading..." : "Add photos"}
                </button>
                <span className="list-item__sub">JPEG, PNG or WebP, up to 5 MB each, max 10</span>
            </div>
        </div>
    );
}
