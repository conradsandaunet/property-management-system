package org.conrad.maintenanceservice.service;

import org.conrad.maintenanceservice.client.ResidentServiceClient;
import org.conrad.maintenanceservice.model.MaintenanceRequest;
import org.conrad.maintenanceservice.model.MaintenanceStatus;
import org.conrad.maintenanceservice.repository.MaintenanceRequestRepository;
import org.conrad.maintenanceservice.repository.MaintenanceWorkerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MaintenanceServiceCompletedTest {

    private static final Long WORKER_ID = 1L;

    @Mock
    private MaintenanceRequestRepository repository;
    @Mock
    private MaintenanceWorkerRepository workerRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ResidentServiceClient residentClient;

    @InjectMocks
    private MaintenanceService service;

    @Test
    void listTheResolvedRequestsOfTheWorker() {
        MaintenanceRequest done = new MaintenanceRequest();
        done.setId(10L);
        done.setStatus(MaintenanceStatus.RESOLVED);

        when(repository.findByAssignedWorkerIdAndStatusOrderByUpdatedAtDesc(WORKER_ID, MaintenanceStatus.RESOLVED))
                .thenReturn(List.of(done));

        assertThat(service.listCompletedBy(WORKER_ID)).containsExactly(done);
    }

    @Test
    void isEmptyWhenTheWorkerHasNotCompletedAnything() {
        when(repository.findByAssignedWorkerIdAndStatusOrderByUpdatedAtDesc(WORKER_ID, MaintenanceStatus.RESOLVED))
                .thenReturn(List.of());

        assertThat(service.listCompletedBy(WORKER_ID)).isEmpty();
    }
}
