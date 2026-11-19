package org.conrad.maintenanceservice.service;

import org.conrad.maintenanceservice.client.ResidentServiceClient;
import org.conrad.maintenanceservice.dto.MaintenanceCreateRequest;
import org.conrad.maintenanceservice.dto.ResidentDto;
import org.conrad.maintenanceservice.model.MaintenanceCategory;
import org.conrad.maintenanceservice.model.MaintenancePriority;
import org.conrad.maintenanceservice.model.MaintenanceRequest;
import org.conrad.maintenanceservice.repository.MaintenanceRequestRepository;
import org.conrad.maintenanceservice.repository.MaintenanceWorkerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceCreateTest {

    private static final Long RESIDENT_ID = 2L;
    private static final String AUTHORIZATION = "Bearer token";

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

    private final MaintenanceCreateRequest input = new MaintenanceCreateRequest(
            MaintenanceCategory.PLUMBING, "Bathroom", "Leaking pipe", MaintenancePriority.HIGH);

    @Test
    void storesApartmentDetailsFromResidentService() {
        when(residentClient.getCurrentResident(AUTHORIZATION)).thenReturn(Optional.of(
                new ResidentDto(RESIDENT_ID, 2L, "102", "Solsiden Borettslag")));
        when(repository.save(any(MaintenanceRequest.class))).thenAnswer(i -> i.getArgument(0));

        MaintenanceRequest result = service.create(RESIDENT_ID, AUTHORIZATION, input);

        assertThat(result.getApartmentNumber()).isEqualTo("102");
        assertThat(result.getBuildingName()).isEqualTo("Solsiden Borettslag");
    }

    @Test
    void stillCreatesRequestWhenResidentServiceIsUnavailable() {
        when(residentClient.getCurrentResident(AUTHORIZATION)).thenReturn(Optional.empty());
        when(repository.save(any(MaintenanceRequest.class))).thenAnswer(i -> i.getArgument(0));

        MaintenanceRequest result = service.create(RESIDENT_ID, AUTHORIZATION, input);

        assertThat(result.getResidentId()).isEqualTo(RESIDENT_ID);
        assertThat(result.getApartmentNumber()).isNull();
    }
}