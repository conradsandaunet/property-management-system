import { request } from "./client";

export const createMaintenance = (token, data) =>
    request("/maintenance", { token, method: "POST", body: data});

export const getMyMaintenance = (token) =>
    request("/maintenance/my", { token });

export const getOpenMaintenance = (token) =>
    request("/maintenance/open", { token });

export const getWorkers = (token) =>
    request("/workers", { token });

export const assignWorker = (token, requestId, workerId) =>
    request(`/maintenance/${requestId}/assignment`, {token, method: "PUT", body: {
        workerId }});

export const getAssignedMaintenance = (token) =>
    request("/maintenance/assigned", { token });

export const getCompletedMaintenance = (token) => request("/maintenance/completed", { token });

export const changeMaintenanceStatus = (token, requestId, status) =>
    request(`/maintenance/${requestId}/status`, { token, method: "PATCH", body: { status } });

export const getComments = (token, requestId) =>
    request(`/maintenance/${requestId}/comments`, { token });

export const addComment = (token, requestId, text) =>
    request(`/maintenance/${requestId}/comments`, { token, method: "POST", body: { text } });
