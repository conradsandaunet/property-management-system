import { request } from "./client";

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
