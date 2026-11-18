import { useEffect, useState } from "react";
import Icon from "./Icon";
import { useAuth } from "../auth/useAuth";
import { getBuildingFacadeBlob } from "../api/residents";

/**
 * A building's facade photo, fetched with the token. Shows `fallback` (or nothing) when the
 * building has none. Change `version` to reload it after an upload.
 */
export default function FacadeImage({ buildingId, alt, className = "facade", version = 0, fallback = null }) {
    const { token } = useAuth();
    const [state, setState] = useState({ src: null, missing: false });

    useEffect(() => {
        let cancelled = false;
        let url = null;
        getBuildingFacadeBlob(token, buildingId)
            .then((blob) => {
                if (cancelled) return;
                if (!blob) {
                    setState({ src: null, missing: true });
                    return;
                }
                url = URL.createObjectURL(blob);
                setState({ src: url, missing: false });
            })
            .catch(() => { if (!cancelled) setState({ src: null, missing: true }); });
        return () => {
            cancelled = true;
            if (url) URL.revokeObjectURL(url);
        };
    }, [token, buildingId, version]);

    if (state.missing) return fallback;
    return (
        <div className={className}>
            {state.src
                ? <img className="facade__img" src={state.src} alt={alt} />
                : <span className="facade__placeholder" aria-hidden="true"><Icon name="building" size={28} /></span>}
        </div>
    );
}
