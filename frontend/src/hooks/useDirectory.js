import { useMemo } from "react";
import { useAsync } from "./useAsync";
import { getApartments, searchResidents } from "../api/residents";
import { apartmentLabel } from "../lib/format";

/**
 * Board pages only: the other services store ids, the names live in resident-service.
 * Loads apartments and residents once and turns ids into readable labels.
 */
export function useDirectory(token) {
    const apartments = useAsync(() => getApartments(token), [token]);
    const residents = useAsync(() => searchResidents(token, "", true), [token]);

    const reloadApartments = apartments.reload;
    const reloadResidents = residents.reload;

    return useMemo(() => {
        const aptById = new Map((apartments.data ?? []).map((a) => [a.id, a]));
        const resById = new Map((residents.data ?? []).map((r) => [r.id, r]));
        return {
            loading: apartments.loading || residents.loading,
            apartments: apartments.data ?? [],
            reload: () => { reloadApartments(); reloadResidents(); },
            apartment: (id) => aptById.get(id),
            apartmentLabel: (id) => (aptById.has(id) ? apartmentLabel(aptById.get(id)) : `Apartment #${id}`),
            resident: (id) => resById.get(id),
            residentName: (id) => {
                const r = resById.get(id);
                return r ? `${r.firstName} ${r.lastName}` : `Resident #${id}`;
            },
            residentApartment: (id) => {
                const r = resById.get(id);
                return r ? apartmentLabel(aptById.get(r.apartmentId) ?? { apartmentNumber: r.apartmentNumber }) : "";
            },
        };
    }, [apartments.data, residents.data, apartments.loading, residents.loading, reloadApartments, reloadResidents]);
}
