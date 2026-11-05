package org.conrad.maintenanceservice.dto;

// The temporary password is only ever returned here, once, when the worker is created.
public record WorkerCreatedResponse(WorkerResponse worker, String temporaryPassword) {
}
