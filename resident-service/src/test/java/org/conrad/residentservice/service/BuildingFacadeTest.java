package org.conrad.residentservice.service;

import org.conrad.residentservice.model.Building;
import org.conrad.residentservice.model.BuildingFacade;
import org.conrad.residentservice.repository.BuildingFacadeRepository;
import org.conrad.residentservice.repository.BuildingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildingFacadeTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};

    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private BuildingFacadeRepository facadeRepository;

    @InjectMocks
    private BuildingService service;

    private void assertStatus(Runnable action, HttpStatus status) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(status));
    }

    @Test
    void setFacade_storesTheImageWithTheTypeFromItsBytes() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(Building.builder().id(1L).name("Main").build()));

        service.setFacade("9", 1L, "front.jpg", JPEG);

        ArgumentCaptor<BuildingFacade> saved = ArgumentCaptor.forClass(BuildingFacade.class);
        verify(facadeRepository).save(saved.capture());
        assertThat(saved.getValue().getBuildingId()).isEqualTo(1L);
        assertThat(saved.getValue().getContentType()).isEqualTo("image/jpeg");
        assertThat(saved.getValue().getSizeBytes()).isEqualTo(JPEG.length);
    }

    @Test
    void setFacade_rejectsFilesThatAreNotImages() {
        when(buildingRepository.findById(1L)).thenReturn(Optional.of(Building.builder().id(1L).name("Main").build()));

        assertStatus(() -> service.setFacade("9", 1L, "front.jpg", "<html>".getBytes()), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verify(facadeRepository, never()).save(any());
    }

    @Test
    void setFacade_unknownBuildingIsNotFound() {
        when(buildingRepository.findById(9L)).thenReturn(Optional.empty());

        assertStatus(() -> service.setFacade("9", 9L, "front.jpg", JPEG), HttpStatus.NOT_FOUND);
        verify(facadeRepository, never()).save(any());
    }

    @Test
    void getFacade_buildingWithoutOneIsNotFound() {
        when(facadeRepository.findById(1L)).thenReturn(Optional.empty());

        assertStatus(() -> service.getFacade(1L), HttpStatus.NOT_FOUND);
    }

    @Test
    void getAll_tellsWhichBuildingsHaveAFacade() {
        when(buildingRepository.findAll()).thenReturn(List.of(
                Building.builder().id(1L).name("Main").build(),
                Building.builder().id(2L).name("Annex").build()));
        when(facadeRepository.findBuildingIds()).thenReturn(Set.of(1L));

        assertThat(service.getAll()).extracting(b -> b.name() + ":" + b.hasFacade())
                .containsExactly("Annex:false", "Main:true");
    }
}
