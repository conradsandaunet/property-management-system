package org.conrad.residentservice.config;

import org.conrad.residentservice.repository.ApartmentPhotoRepository;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.conrad.residentservice.service.ApartmentPhotoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demo photos from {@code resources/demo-pictures}: {@code apartment-<n>-<room>.jpg} goes to apartment
 * id n, and {@code building-facade.jpg} to every apartment (listed first). Runs after
 * {@link DemoResidentSeed}, only on a database without photos, and only for the demo apartments.
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoPhotoSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoPhotoSeed.class);
    private static final Pattern APARTMENT_FILE = Pattern.compile("apartment-(\\d+)-.*\\.jpg");
    private static final String FACADE = "building-facade.jpg";

    private final ApartmentPhotoRepository photoRepository;
    private final ApartmentRepository apartmentRepository;
    private final ApartmentPhotoService photoService;

    public DemoPhotoSeed(ApartmentPhotoRepository photoRepository, ApartmentRepository apartmentRepository,
                         ApartmentPhotoService photoService) {
        this.photoRepository = photoRepository;
        this.apartmentRepository = apartmentRepository;
        this.photoService = photoService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (photoRepository.count() > 0) {
            return;
        }

        List<Resource> files = Arrays.stream(new PathMatchingResourcePatternResolver()
                        .getResources("classpath:demo-pictures/*.jpg"))
                // The facade first, so it is the first photo of every apartment.
                .sorted(Comparator.comparing((Resource r) -> !FACADE.equals(r.getFilename()))
                        .thenComparing(Resource::getFilename))
                .toList();

        int added = 0;
        for (var apartment : apartmentRepository.findAll()) {
            // Only the demo apartments: the ids in the file names mean nothing in a database with other data.
            if (!DemoResidentSeed.BUILDING.equals(apartment.getBuildingName())) {
                continue;
            }
            for (Resource file : files) {
                if (belongsTo(file.getFilename(), apartment.getId())) {
                    photoService.add("demo-seed", apartment.getId(), file.getFilename(), file.getContentAsByteArray());
                    added++;
                }
            }
        }
        log.info("Seeded {} demo apartment photos", added);
    }

    private static boolean belongsTo(String fileName, Long apartmentId) {
        if (FACADE.equals(fileName)) {
            return true;
        }
        Matcher m = APARTMENT_FILE.matcher(fileName);
        return m.matches() && Long.parseLong(m.group(1)) == apartmentId;
    }
}
