package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.ApartmentResponse;
import org.conrad.residentservice.dto.BuildingOverviewResponse;
import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.conrad.residentservice.repository.ResidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApartmentServiceTest {

    @Mock
    private ApartmentRepository apartmentRepository;
    @Mock
    private ResidentRepository residentRepository;

    @InjectMocks
    private ApartmentService apartmentService;

    private Resident resident(Long id, boolean active) {
        return Resident.builder().id(id).firstName("R" + id).lastName("X").email(id + "@x.no")
                .passwordHash("h").active(active).build();
    }

    @Test
    void getOverview_derivesVacantFromTotalAndOccupied() {
        when(apartmentRepository.count()).thenReturn(5L);
        when(residentRepository.countOccupiedApartments()).thenReturn(3L);
        when(residentRepository.countByActiveTrue()).thenReturn(7L);

        BuildingOverviewResponse overview = apartmentService.getOverview();

        assertThat(overview.totalApartments()).isEqualTo(5);
        assertThat(overview.occupiedApartments()).isEqualTo(3);
        assertThat(overview.vacantApartments()).isEqualTo(2);
        assertThat(overview.totalResidents()).isEqualTo(7);
    }

    @Test
    void getOverview_emptyBuilding_isAllZero() {
        when(apartmentRepository.count()).thenReturn(0L);
        when(residentRepository.countOccupiedApartments()).thenReturn(0L);
        when(residentRepository.countByActiveTrue()).thenReturn(0L);

        BuildingOverviewResponse overview = apartmentService.getOverview();

        assertThat(overview).isEqualTo(new BuildingOverviewResponse(0, 0, 0, 0));
    }

    @Test
    void getAll_countsOnlyActiveResidents() {
        Apartment apartment = Apartment.builder().id(10L).apartmentNumber("A1").floor(1).buildingName("Main")
                .residents(new ArrayList<>(List.of(resident(1L, true), resident(2L, false), resident(3L, true))))
                .build();
        when(apartmentRepository.findAll()).thenReturn(List.of(apartment));

        List<ApartmentResponse> result = apartmentService.getAll();

        assertThat(result).hasSize(1);
        ApartmentResponse response = result.get(0);
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.apartmentNumber()).isEqualTo("A1");
        assertThat(response.floor()).isEqualTo(1);
        assertThat(response.buildingName()).isEqualTo("Main");
        assertThat(response.residentCount()).isEqualTo(2);
    }

    @Test
    void getAll_apartmentWithoutResidents_hasZeroResidentCount() {
        Apartment vacant = Apartment.builder().id(11L).apartmentNumber("A2").floor(1).buildingName("Main").build();
        when(apartmentRepository.findAll()).thenReturn(List.of(vacant));

        assertThat(apartmentService.getAll()).singleElement()
                .extracting(ApartmentResponse::residentCount).isEqualTo(0);
    }
}
