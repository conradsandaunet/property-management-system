package org.conrad.residentservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

// The demo photos are seeded through the same checks as an upload, so a bad file would break start-up.
class DemoPicturesTest {

    @Test
    void everyDemoPictureIsAnAcceptedImageUnderTheSizeLimit() throws Exception {
        Resource[] pictures = new PathMatchingResourcePatternResolver().getResources("classpath:demo-pictures/*.jpg");

        assertThat(pictures).isNotEmpty();
        for (Resource picture : pictures) {
            byte[] data = picture.getContentAsByteArray();
            assertThat(ApartmentPhotoService.sniffImageType(data)).as(picture.getFilename()).isEqualTo("image/jpeg");
            assertThat(data.length).as(picture.getFilename()).isLessThanOrEqualTo(ApartmentPhotoService.MAX_BYTES);
        }
    }
}
