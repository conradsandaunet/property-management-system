package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.BuildingRequest;
import org.conrad.residentservice.dto.BuildingResponse;
import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Building;
import org.conrad.residentservice.repository.BuildingRepository;
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
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private org.conrad.residentservice.repository.BuildingFacadeRepository facadeRepository;

    @InjectMocks
    private BuildingService service;

    private Building building(Long id, String name, int apartments) {
        Building b = Building.builder().id(id).name(name).apartments(new ArrayList<>()).build();
        for (int i = 0; i < apartments; i++) {
            b.getApartments().add(Apartment.builder().id(id * 100 + i).build());
        }
        return b;
    }

    private void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(status));
    }

    @Test
    void getAll_isSortedByNameIgnoringCaseWithApartmentCounts() {
        when(buildingRepository.findAll())
                .thenReturn(List.of(building(2L, "harbour", 1), building(1L, "Alpha", 3)));

        List<BuildingResponse> result = service.getAll();

        assertThat(result).extracting(BuildingResponse::name).containsExactly("Alpha", "harbour");
        assertThat(result).extracting(BuildingResponse::apartmentCount).containsExactly(3, 1);
    }

    @Test
    void create_savesTrimmedName() {
        when(buildingRepository.existsByNameIgnoreCase("Annex")).thenReturn(false);
        when(buildingRepository.save(any(Building.class))).thenAnswer(i -> {
            Building b = i.getArgument(0);
            b.setId(5L);
            return b;
        });

        BuildingResponse response = service.create("1", new BuildingRequest("  Annex "));

        assertThat(response).isEqualTo(new BuildingResponse(5L, "Annex", 0, false));
    }

    @Test
    void create_duplicateNameIgnoringCase_isConflict() {
        when(buildingRepository.existsByNameIgnoreCase("annex")).thenReturn(true);

        assertStatus(() -> service.create("1", new BuildingRequest("annex")), HttpStatus.CONFLICT);
        verify(buildingRepository, never()).save(any());
    }

    @Test
    void create_rejectsBlankMissingAndTooLongNames() {
        assertStatus(() -> service.create("1", new BuildingRequest("   ")), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create("1", new BuildingRequest(null)), HttpStatus.BAD_REQUEST);
        assertStatus(() -> service.create("1", new BuildingRequest("x".repeat(256))), HttpStatus.BAD_REQUEST);
        verify(buildingRepository, never()).save(any());
    }

    @Test
    void update_renamesTheBuilding() {
        Building b = building(1L, "Main", 2);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(buildingRepository.existsByNameIgnoreCase("Central")).thenReturn(false);

        BuildingResponse response = service.update("1", 1L, new BuildingRequest("Central"));

        assertThat(response.name()).isEqualTo("Central");
        assertThat(b.getName()).isEqualTo("Central");
    }

    @Test
    void update_onlyChangingCaseIsNotAConflictWithItself() {
        Building b = building(1L, "Main", 0);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b));

        service.update("1", 1L, new BuildingRequest("MAIN"));

        assertThat(b.getName()).isEqualTo("MAIN");
        verify(buildingRepository, never()).existsByNameIgnoreCase(any());
    }

    @Test
    void update_toAnotherBuildingsName_isConflict() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building(1L, "Main", 0)));
        when(buildingRepository.existsByNameIgnoreCase("Annex")).thenReturn(true);

        assertStatus(() -> service.update("1", 1L, new BuildingRequest("Annex")), HttpStatus.CONFLICT);
    }

    @Test
    void update_unknownBuilding_isNotFound() {
        when(buildingRepository.findById(9L)).thenReturn(Optional.empty());

        assertStatus(() -> service.update("1", 9L, new BuildingRequest("X")), HttpStatus.NOT_FOUND);
    }

    @Test
    void remove_emptyBuilding_isDeleted() {
        Building b = building(1L, "Main", 0);
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b));

        service.remove("1", 1L);

        verify(buildingRepository).delete(b);
    }

    @Test
    void remove_buildingWithApartments_isConflictAndKept() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(building(1L, "Main", 2)));

        assertStatus(() -> service.remove("1", 1L), HttpStatus.CONFLICT);
        verify(buildingRepository, never()).delete(any());
    }

    @Test
    void remove_unknownBuilding_isNotFound() {
        when(buildingRepository.findById(9L)).thenReturn(Optional.empty());

        assertStatus(() -> service.remove("1", 9L), HttpStatus.NOT_FOUND);
    }
}
