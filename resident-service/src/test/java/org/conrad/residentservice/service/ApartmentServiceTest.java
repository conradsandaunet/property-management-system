package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.ApartmentCreateRequest;
import org.conrad.residentservice.dto.ApartmentResponse;
import org.conrad.residentservice.dto.ApartmentUpdateRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    // ---- create ----

    @Test
    void create_savesTrimmedApartment() {
        when(apartmentRepository.existsByBuildingNameIgnoreCaseAndApartmentNumberIgnoreCase("Main", "A1"))
                .thenReturn(false);

        ApartmentResponse response = apartmentService.create("1", new ApartmentCreateRequest(" A1 ", 2, " Main "));

        assertThat(response.apartmentNumber()).isEqualTo("A1");
        assertThat(response.buildingName()).isEqualTo("Main");
        assertThat(response.floor()).isEqualTo(2);
        assertThat(response.residentCount()).isZero();
        verify(apartmentRepository).save(any(Apartment.class));
    }

    @Test
    void create_duplicateNumberInSameBuilding_isConflict() {
        when(apartmentRepository.existsByBuildingNameIgnoreCaseAndApartmentNumberIgnoreCase("Main", "A1"))
                .thenReturn(true);

        assertThatThrownBy(() -> apartmentService.create("1", new ApartmentCreateRequest("A1", 1, "Main")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(apartmentRepository, never()).save(any());
    }

    @Test
    void create_rejectsBlankOrTooLongOrMissingFields() {
        assertBadRequest(new ApartmentCreateRequest("  ", 1, "Main"));
        assertBadRequest(new ApartmentCreateRequest("A1", 1, null));
        assertBadRequest(new ApartmentCreateRequest("A1", null, "Main"));
        assertBadRequest(new ApartmentCreateRequest("12345678901", 1, "Main"));
        verify(apartmentRepository, never()).save(any());
    }

    private void assertBadRequest(ApartmentCreateRequest request) {
        assertThatThrownBy(() -> apartmentService.create("1", request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // ---- update ----

    private Apartment existing() {
        return Apartment.builder().id(10L).apartmentNumber("A1").floor(1).buildingName("Main").build();
    }

    @Test
    void update_changesOnlyProvidedFields() {
        Apartment apartment = existing();
        when(apartmentRepository.findById(10L)).thenReturn(Optional.of(apartment));

        ApartmentResponse response = apartmentService.update("1", 10L, new ApartmentUpdateRequest(null, 3, null));

        assertThat(response.floor()).isEqualTo(3);
        assertThat(response.apartmentNumber()).isEqualTo("A1");
        assertThat(response.buildingName()).isEqualTo("Main");
        verify(apartmentRepository, never())
                .existsByBuildingNameIgnoreCaseAndApartmentNumberIgnoreCase(any(), any());
    }

    @Test
    void update_renameToExistingApartment_isConflict() {
        when(apartmentRepository.findById(10L)).thenReturn(Optional.of(existing()));
        when(apartmentRepository.existsByBuildingNameIgnoreCaseAndApartmentNumberIgnoreCase("Main", "B2"))
                .thenReturn(true);

        assertThatThrownBy(() -> apartmentService.update("1", 10L, new ApartmentUpdateRequest("B2", null, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void update_sameNumberDifferentCase_isNotAConflictWithItself() {
        Apartment apartment = existing();
        when(apartmentRepository.findById(10L)).thenReturn(Optional.of(apartment));

        apartmentService.update("1", 10L, new ApartmentUpdateRequest("a1", null, null));

        assertThat(apartment.getApartmentNumber()).isEqualTo("a1");
    }

    @Test
    void update_unknownApartment_isNotFound() {
        when(apartmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apartmentService.update("1", 99L, new ApartmentUpdateRequest(null, 2, null)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    // ---- remove ----

    @Test
    void remove_emptyApartment_isDeleted() {
        Apartment apartment = existing();
        when(apartmentRepository.findById(10L)).thenReturn(Optional.of(apartment));
        when(residentRepository.existsByApartmentId(10L)).thenReturn(false);

        apartmentService.remove("1", 10L);

        verify(apartmentRepository).delete(apartment);
    }

    @Test
    void remove_apartmentWithResidents_isConflict() {
        when(apartmentRepository.findById(10L)).thenReturn(Optional.of(existing()));
        when(residentRepository.existsByApartmentId(10L)).thenReturn(true);

        assertThatThrownBy(() -> apartmentService.remove("1", 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(apartmentRepository, never()).delete(any(Apartment.class));
    }

    @Test
    void remove_unknownApartment_isNotFound() {
        when(apartmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apartmentService.remove("1", 99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
