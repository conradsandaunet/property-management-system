package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.ApartmentPhotoResponse;
import org.conrad.residentservice.model.ApartmentPhoto;
import org.conrad.residentservice.repository.ApartmentPhotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ApartmentPhotoService {

    private static final Logger log = LoggerFactory.getLogger(ApartmentPhotoService.class);

    static final int MAX_BYTES = 5 * 1024 * 1024;
    static final int MAX_PHOTOS_PER_APARTMENT = 10;
    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final ApartmentPhotoRepository photoRepository;
    private final ApartmentService apartmentService;

    public ApartmentPhotoService(ApartmentPhotoRepository photoRepository, ApartmentService apartmentService) {
        this.photoRepository = photoRepository;
        this.apartmentService = apartmentService;
    }

    @Transactional(readOnly = true)
    public List<ApartmentPhotoResponse> list(Long apartmentId) {
        apartmentService.requireExists(apartmentId);
        return photoRepository.findByApartmentIdOrderByIdAsc(apartmentId).stream()
                .map(p -> new ApartmentPhotoResponse(p.getId(), p.getFileName(), p.getContentType(), p.getSizeBytes()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ApartmentPhoto get(Long apartmentId, Long photoId) {
        return photoRepository.findByIdAndApartmentId(photoId, apartmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found"));
    }

    @Transactional
    public ApartmentPhotoResponse add(String actor, Long apartmentId, String fileName, byte[] data) {
        apartmentService.requireExists(apartmentId);

        if (data == null || data.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }
        if (data.length > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "The photo must be at most " + (MAX_BYTES / 1024 / 1024) + " MB");
        }
        // Decide the type from the bytes, not from what the client claims.
        String contentType = sniffImageType(data);
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPEG, PNG and WebP images are allowed");
        }
        if (photoRepository.countByApartmentId(apartmentId) >= MAX_PHOTOS_PER_APARTMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An apartment can have at most " + MAX_PHOTOS_PER_APARTMENT + " photos");
        }

        ApartmentPhoto photo = photoRepository.save(ApartmentPhoto.builder()
                .apartmentId(apartmentId)
                .fileName(cleanFileName(fileName))
                .contentType(contentType)
                .sizeBytes(data.length)
                .data(data)
                .build());

        log.info("Board member (residentId={}) added photoId={} to apartmentId={}", actor, photo.getId(), apartmentId);
        return new ApartmentPhotoResponse(photo.getId(), photo.getFileName(), photo.getContentType(), photo.getSizeBytes());
    }

    @Transactional
    public void remove(String actor, Long apartmentId, Long photoId) {
        ApartmentPhoto photo = get(apartmentId, photoId);
        photoRepository.delete(photo);
        log.info("Board member (residentId={}) removed photoId={} from apartmentId={}", actor, photoId, apartmentId);
    }

    static String sniffImageType(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G'
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return "image/png";
        }
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    /** Keeps just the base name and drops control characters. The name is only a label, never a path. */
    private static String cleanFileName(String name) {
        String cleaned = name == null ? "" : name.replaceAll("[\\\\/]+", "/");
        cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").trim();
        if (cleaned.isEmpty()) {
            return "photo";
        }
        return cleaned.length() > MAX_FILE_NAME_LENGTH ? cleaned.substring(0, MAX_FILE_NAME_LENGTH) : cleaned;
    }
}
