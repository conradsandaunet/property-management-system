import { request } from "./client";

export const getMyNotifications = (token) =>
    request("/notifications/my", { token });

export const markNotificationRead = (token, id) =>
    request(`/notifications/${id}/read`, { token, method: "PATCH" });
