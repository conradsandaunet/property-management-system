package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.ApartmentPhotoResponse;
import org.conrad.residentservice.model.ApartmentPhoto;
import org.conrad.residentservice.repository.ApartmentPhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApartmentPhotoServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @Mock
    private ApartmentPhotoRepository photoRepository;
    @Mock
    private ApartmentService apartmentService;

    @InjectMocks
    private ApartmentPhotoService service;

    @BeforeEach
    void saveReturnsTheEntity() {
        org.mockito.Mockito.lenient().when(photoRepository.save(any(ApartmentPhoto.class))).thenAnswer(i -> {
            ApartmentPhoto p = i.getArgument(0);
            p.setId(42L);
            return p;
        });
    }

    private ResponseStatusException failure(Runnable action) {
        return (ResponseStatusException) org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class, action::run);
    }

    @Test
    void add_storesTheImageWithTheTypeDetectedFromItsBytes() {
        ApartmentPhotoResponse response = service.add("1", 7L, "living-room.png", PNG);

        ArgumentCaptor<ApartmentPhoto> saved = ArgumentCaptor.forClass(ApartmentPhoto.class);
        verify(photoRepository).save(saved.capture());
        assertThat(saved.getValue().getApartmentId()).isEqualTo(7L);
        assertThat(saved.getValue().getContentType()).isEqualTo("image/png");
        assertThat(saved.getValue().getSizeBytes()).isEqualTo(PNG.length);
        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.fileName()).isEqualTo("living-room.png");
    }

    @Test
    void add_acceptsJpegAndWebp() {
        assertThat(service.add("1", 7L, "a.jpg", JPEG).contentType()).isEqualTo("image/jpeg");
        assertThat(service.add("1", 7L, "a.webp", WEBP).contentType()).isEqualTo("image/webp");
    }

    @Test
    void add_rejectsNonImagesEvenWhenNamedLikeOne() {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes();

        ResponseStatusException e = failure(() -> service.add("1", 7L, "photo.png", html));

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        verify(photoRepository, never()).save(any());
    }

    @Test
    void add_rejectsEmptyFiles() {
        ResponseStatusException e = failure(() -> service.add("1", 7L, "a.png", new byte[0]));

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void add_rejectsTooLargeFiles() {
        byte[] big = new byte[ApartmentPhotoService.MAX_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);

        ResponseStatusException e = failure(() -> service.add("1", 7L, "a.png", big));

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        verify(photoRepository, never()).save(any());
    }

    @Test
    void add_rejectsMoreThanTheLimitPerApartment() {
        when(photoRepository.countByApartmentId(7L)).thenReturn((long) ApartmentPhotoService.MAX_PHOTOS_PER_APARTMENT);

        ResponseStatusException e = failure(() -> service.add("1", 7L, "a.png", PNG));

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(photoRepository, never()).save(any());
    }

    @Test
    void add_unknownApartmentIs404AndStoresNothing() {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Apartment not found"))
                .when(apartmentService).requireExists(99L);

        ResponseStatusException e = failure(() -> service.add("1", 99L, "a.png", PNG));

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(photoRepository, never()).save(any());
    }

    @Test
    void add_keepsOnlyTheBaseNameOfTheUploadedFileName() {
        assertThat(service.add("1", 7L, "C:\\Users\\me\\..\\room.png", PNG).fileName()).isEqualTo("room.png");
        assertThat(service.add("1", 7L, "../../etc/passwd", PNG).fileName()).isEqualTo("passwd");
        assertThat(service.add("1", 7L, null, PNG).fileName()).isEqualTo("photo");
    }

    @Test
    void get_photoOfAnotherApartmentIs404() {
        when(photoRepository.findByIdAndApartmentId(5L, 7L)).thenReturn(Optional.empty());

        ResponseStatusException e = failure(() -> service.get(7L, 5L));

        assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void remove_deletesThePhoto() {
        ApartmentPhoto photo = ApartmentPhoto.builder().id(5L).apartmentId(7L).build();
        when(photoRepository.findByIdAndApartmentId(5L, 7L)).thenReturn(Optional.of(photo));

        service.remove("1", 7L, 5L);

        verify(photoRepository).delete(photo);
    }

    @Test
    void remove_unknownPhotoIs404() {
        when(photoRepository.findByIdAndApartmentId(5L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove("1", 7L, 5L)).isInstanceOf(ResponseStatusException.class);
        verify(photoRepository, never()).delete(any());
    }
}
