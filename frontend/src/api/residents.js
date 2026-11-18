import { request, requestBlob } from "./client";

export const getHousehold = (token) =>
    request("/residents/household", { token });

export const updateMyContact = (token, data) =>
    request("/residents/me", { token, method: "PATCH", body: data });

export const getOverview = (token) =>
    request("/apartments/overview", { token });

export const getApartments = (token) =>
    request("/apartments", { token });

export const searchResidents = (token, query = "", includeInactive = false) => {
    const params = new URLSearchParams({ query, includeInactive });
    return request(`/residents/search?${params}`, { token });
};

export const createResident = (token, data) =>
    request("/residents", { token, method: "POST", body: data });

export const updateResident = (token, id, data) =>
    request(`/residents/${id}`, { token, method: "PATCH", body: data });

export const removeResident = (token, id) =>
    request(`/residents/${id}`, { token, method: "DELETE" });

export const createApartment = (token, data) =>
    request("/apartments", { token, method: "POST", body: data });

export const updateApartment = (token, id, data) =>
    request(`/apartments/${id}`, { token, method: "PATCH", body: data });

export const removeApartment = (token, id) =>
    request(`/apartments/${id}`, { token, method: "DELETE" });

export const getBuildings = (token) =>
    request("/buildings", { token });

export const createBuilding = (token, data) =>
    request("/buildings", { token, method: "POST", body: data });

export const updateBuilding = (token, id, data) =>
    request(`/buildings/${id}`, { token, method: "PATCH", body: data });

export const removeBuilding = (token, id) =>
    request(`/buildings/${id}`, { token, method: "DELETE" });

export const getApartmentResidents = (token, id) =>
    request(`/apartments/${id}/residents`, { token });


export const getApartmentPhotos = (token, apartmentId) =>
    request(`/apartments/${apartmentId}/photos`, { token });

export const uploadApartmentPhoto = (token, apartmentId, file) => {
    const form = new FormData();
    form.append("file", file);
    return request(`/apartments/${apartmentId}/photos`, { token, method: "POST", body: form });
};

export const removeApartmentPhoto = (token, apartmentId, photoId) =>
    request(`/apartments/${apartmentId}/photos/${photoId}`, { token, method: "DELETE" });

/** Photos need the token, so they can't be a plain <img src>. Returns a Blob. */
export const getApartmentPhotoBlob = (token, apartmentId, photoId) =>
    requestBlob(`/apartments/${apartmentId}/photos/${photoId}`, { token });
