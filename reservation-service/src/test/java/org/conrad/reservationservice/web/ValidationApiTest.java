package org.conrad.reservationservice.web;

import org.conrad.reservationservice.service.ReservationService;
import org.conrad.reservationservice.service.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A request with missing required fields is answered with 400 before the services are called. No database. */
class ValidationApiTest {

    private ReservationService reservationService;
    private ResourceService resourceService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        resourceService = mock(ResourceService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new ReservationController(reservationService),
                new ResourceController(resourceService, reservationService)).build();
    }

    @Test
    void aBookingWithoutAResourceIs400() throws Exception {
        mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTime\":\"2030-01-01T10:00:00\",\"endTime\":\"2030-01-01T11:00:00\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reservationService);
    }

    @Test
    void aBookingWithoutTimesIs400() throws Exception {
        mvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON).content("{\"resourceId\":1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reservationService);
    }

    @Test
    void blockingAResourceWithoutAnEndIs400() throws Exception {
        mvc.perform(post("/resources/1/block").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2030-01-01T10:00:00\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(resourceService);
    }
}
