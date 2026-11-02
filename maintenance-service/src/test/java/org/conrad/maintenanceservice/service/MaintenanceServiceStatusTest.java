package org.conrad.maintenanceservice.service;

import org.conrad.maintenanceservice.event.MaintenanceCompleted;
import org.conrad.maintenanceservice.model.MaintenanceRequest;
import org.conrad.maintenanceservice.model.MaintenanceStatus;
import org.conrad.maintenanceservice.model.MaintenanceWorker;
import org.conrad.maintenanceservice.repository.MaintenanceRequestRepository;
import org.conrad.maintenanceservice.repository.MaintenanceWorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceStatusTest {

    private static final Long REQUEST_ID = 10L;
    private static final Long WORKER_ID = 1L;

    @Mock
    private MaintenanceRequestRepository repository;
    @Mock
    private MaintenanceWorkerRepository workerRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MaintenanceService service;

    private MaintenanceRequest request;

    @BeforeEach
    void setUp() {
        MaintenanceWorker worker = new MaintenanceWorker();
        worker.setId(WORKER_ID);

        request = new MaintenanceRequest();
        request.setId(REQUEST_ID);
        request.setStatus(MaintenanceStatus.ASSIGNED);
        request.setAssignedWorker(worker);
    }

    @Test
    void assignedRequestCanBeStarted() {
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        MaintenanceRequest result = service.changeStatusAsWorker(REQUEST_ID, WORKER_ID, MaintenanceStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(MaintenanceStatus.IN_PROGRESS);
    }

    @Test
    void inProgressRequestCanBeCompleted() {
        request.setStatus(MaintenanceStatus.IN_PROGRESS);
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        MaintenanceRequest result = service.changeStatusAsWorker(REQUEST_ID, WORKER_ID, MaintenanceStatus.RESOLVED);

        assertThat(result.getStatus()).isEqualTo(MaintenanceStatus.RESOLVED);
    }

    @Test
    void cannotSkipFromAssignedToResolved() {
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.changeStatusAsWorker(REQUEST_ID, WORKER_ID, MaintenanceStatus.RESOLVED))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(request.getStatus()).isEqualTo(MaintenanceStatus.ASSIGNED);
    }

    @Test
    void cannotGoBackwards() {
        request.setStatus(MaintenanceStatus.IN_PROGRESS);
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.changeStatusAsWorker(REQUEST_ID, WORKER_ID, MaintenanceStatus.ASSIGNED))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void resolvedRequestCannotBeChanged() {
        request.setStatus(MaintenanceStatus.RESOLVED);
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.changeStatusAsWorker(REQUEST_ID, WORKER_ID, MaintenanceStatus.IN_PROGRESS))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void workerCannotChangeSomeoneElsesRequest() {
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.changeStatusAsWorker(REQUEST_ID, 99L, MaintenanceStatus.IN_PROGRESS))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(request.getStatus()).isEqualTo(MaintenanceStatus.ASSIGNED);
    }

    @Test
    void unassignedRequestCannotBeChangedByWorker() {
        request.setAssignedWorker(null);
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.changeStatusAsWorker(REQUEST_ID, WORKER_ID, MaintenanceStatus.IN_PROGRESS))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void unknownRequestGivesNotFound() {
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeStatusAsWorker(REQUEST_ID, WORKER_ID, MaintenanceStatus.IN_PROGRESS))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void completingRequestPublishesEvent() {
        request.setStatus(MaintenanceStatus.IN_PROGRESS);
        when(repository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        service.changeStatusAsWorker(REQUEST_ID, WORKER_ID, MaintenanceStatus.RESOLVED);

        verify(eventPublisher).publishEvent(any(MaintenanceCompleted.class));
    }
}
