package org.conrad.residentservice.web;

import org.conrad.residentservice.service.ApartmentService;
import org.conrad.residentservice.service.AuthService;
import org.conrad.residentservice.service.BuildingService;
import org.conrad.residentservice.service.ResidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bad input must be answered with 400 before the service is called. Before the annotations, a new
 * resident with a missing name or password reached the database and failed with 500. The services are
 * mocks, so this needs no database.
 */
class ValidationApiTest {

    private ResidentService residentService;
    private AuthService authService;
    private ApartmentService apartmentService;
    private BuildingService buildingService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        residentService = mock(ResidentService.class);
        authService = mock(AuthService.class);
        apartmentService = mock(ApartmentService.class);
        buildingService = mock(BuildingService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new ResidentController(residentService),
                new AuthController(authService),
                new ApartmentController(apartmentService),
                new BuildingController(buildingService)).build();
    }

    @Test
    void creatingAResidentWithoutNameAndPasswordIs400() throws Exception {
        mvc.perform(post("/residents").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"kari@example.com\",\"manager\":false,\"apartmentId\":1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(residentService);
    }

    @Test
    void creatingAResidentWithAShortPasswordIs400() throws Exception {
        mvc.perform(post("/residents").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"Kari\",\"lastName\":\"N\",\"email\":\"kari@example.com\","
                                + "\"password\":\"short\",\"manager\":false,\"apartmentId\":1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(residentService);
    }

    @Test
    void loginWithAnEmptyBodyIs400() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void anApartmentWithoutAFloorIs400() throws Exception {
        mvc.perform(post("/apartments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apartmentNumber\":\"101\",\"buildingId\":1}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(apartmentService);
    }

    @Test
    void aBuildingWithABlankNameIs400() throws Exception {
        mvc.perform(post("/buildings").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(buildingService);
    }

    @Test
    void anEmailThatIsNotAnEmailIsRejectedOnUpdate() throws Exception {
        mvc.perform(patch("/residents/me").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(residentService);
    }
}
