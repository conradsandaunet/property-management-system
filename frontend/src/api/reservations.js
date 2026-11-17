import { request } from "./client";

// ---- resources ----

export const getResources = (token) =>
    request("/resources", { token });

export const getAvailability = (token, resourceId, date) =>
    request(`/resources/${resourceId}/availability?date=${date}`, { token });

// ---- resident: own reservations ----

export const createReservation = (token, data) =>
    request("/reservations", { token, method: "POST", body: data });

export const getMyReservations = (token) =>
    request("/reservations/me", { token });

export const cancelReservation = (token, id) =>
    request(`/reservations/${id}`, { token, method: "DELETE" });

// ---- board ----

export const getReservationsForResource = (token, resourceId) =>
    request(`/reservations?resourceId=${resourceId}`, { token });

export const blockResource = (token, id, from, to) =>
    request(`/resources/${id}/block`, { token, method: "POST", body: { from, to } });

export const unblockResource = (token, id) =>
    request(`/resources/${id}/block`, { token, method: "DELETE" });

export const setResourceStatus = (token, id, status) =>
    request(`/resources/${id}/status?status=${status}`, { token, method: "PATCH" });
