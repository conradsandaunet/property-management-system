package org.conrad.residentservice.config;

import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Building;
import org.conrad.residentservice.repository.ApartmentPhotoRepository;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.conrad.residentservice.repository.BuildingFacadeRepository;
import org.conrad.residentservice.repository.BuildingRepository;
import org.conrad.residentservice.service.ApartmentPhotoService;
import org.conrad.residentservice.service.BuildingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demo photos from {@code resources/demo-pictures}: {@code apartment-<n>-<room>.jpg} goes to
 * apartment id n, and {@code building-<n>-facade.jpg} becomes the facade of building id n. Runs after
 * {@link DemoResidentSeed}, only for the demo buildings, and each part only when its table is empty.
 */
@Component
@Order(3)
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true", matchIfMissing = true)
public class DemoPhotoSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoPhotoSeed.class);
    private static final Pattern APARTMENT_FILE = Pattern.compile("apartment-(\\d+)-.*\\.jpg");
    private static final Pattern FACADE_FILE = Pattern.compile("building-(\\d+)-facade\\.jpg");
    private static final Set<String> DEMO_BUILDINGS = Set.of(BoardAccountSeed.BUILDING, DemoResidentSeed.SECOND_BUILDING);
    private static final String ACTOR = "demo-seed";

    private final ApartmentPhotoRepository photoRepository;
    private final BuildingFacadeRepository facadeRepository;
    private final ApartmentRepository apartmentRepository;
    private final BuildingRepository buildingRepository;
    private final ApartmentPhotoService photoService;
    private final BuildingService buildingService;

    public DemoPhotoSeed(ApartmentPhotoRepository photoRepository, BuildingFacadeRepository facadeRepository,
                         ApartmentRepository apartmentRepository, BuildingRepository buildingRepository,
                         ApartmentPhotoService photoService, BuildingService buildingService) {
        this.photoRepository = photoRepository;
        this.facadeRepository = facadeRepository;
        this.apartmentRepository = apartmentRepository;
        this.buildingRepository = buildingRepository;
        this.photoService = photoService;
        this.buildingService = buildingService;
    }

    // One transaction for the whole run: apartment.getBuilding() is lazy and needs an open session.
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        List<Resource> files = Arrays.stream(new PathMatchingResourcePatternResolver()
                        .getResources("classpath:demo-pictures/*.jpg"))
                .sorted(Comparator.comparing(Resource::getFilename))
                .toList();

        int apartmentPhotos = 0;
        if (photoRepository.count() == 0) {
            for (Apartment apartment : apartmentRepository.findAll()) {
                // The ids in the file names only mean something for the demo apartments.
                if (!DEMO_BUILDINGS.contains(apartment.getBuilding().getName())) continue;
                for (Resource file : files) {
                    if (idIn(APARTMENT_FILE, file.getFilename()) == apartment.getId()) {
                        photoService.add(ACTOR, apartment.getId(), file.getFilename(), file.getContentAsByteArray());
                        apartmentPhotos++;
                    }
                }
            }
        }

        int facades = 0;
        if (facadeRepository.count() == 0) {
            for (Building building : buildingRepository.findAll()) {
                if (!DEMO_BUILDINGS.contains(building.getName())) continue;
                for (Resource file : files) {
                    if (idIn(FACADE_FILE, file.getFilename()) == building.getId()) {
                        buildingService.setFacade(ACTOR, building.getId(), file.getFilename(), file.getContentAsByteArray());
                        facades++;
                    }
                }
            }
        }

        if (apartmentPhotos + facades > 0) {
            log.info("Seeded {} demo apartment photos and {} building facades", apartmentPhotos, facades);
        }
    }

    /** The number in the file name, or -1 if the name doesn't match. */
    private static long idIn(Pattern pattern, String fileName) {
        Matcher m = pattern.matcher(fileName);
        return m.matches() ? Long.parseLong(m.group(1)) : -1;
    }
}
