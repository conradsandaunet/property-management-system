package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.ApartmentPhotoResponse;
import org.conrad.residentservice.model.ApartmentPhoto;
import org.conrad.residentservice.service.ApartmentPhotoService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/apartments/{apartmentId}/photos")
@PreAuthorize("hasRole('BOARD')")
public class ApartmentPhotoController {

    private final ApartmentPhotoService photoService;

    public ApartmentPhotoController(ApartmentPhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping
    public List<ApartmentPhotoResponse> list(@PathVariable Long apartmentId) {
        return photoService.list(apartmentId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApartmentPhotoResponse> upload(@PathVariable Long apartmentId,
                                                         @RequestParam("file") MultipartFile file) {
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Couldn't read the file");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(photoService.add(CurrentUser.name(), apartmentId, file.getOriginalFilename(), data));
    }

    @GetMapping("/{photoId}")
    public ResponseEntity<byte[]> download(@PathVariable Long apartmentId, @PathVariable Long photoId) {
        ApartmentPhoto photo = photoService.get(apartmentId, photoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(photo.getData());
    }

    @DeleteMapping("/{photoId}")
    public ResponseEntity<Void> remove(@PathVariable Long apartmentId, @PathVariable Long photoId) {
        photoService.remove(CurrentUser.name(), apartmentId, photoId);
        return ResponseEntity.noContent().build();
    }
}
