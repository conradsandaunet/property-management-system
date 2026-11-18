package org.conrad.billingservice;

import org.conrad.billingservice.client.ResidentServiceClient;
import org.conrad.billingservice.dto.ApartmentDto;
import org.conrad.billingservice.model.Invoice;
import org.conrad.billingservice.model.InvoiceStatus;
import org.conrad.billingservice.repository.InvoiceRepository;
import org.conrad.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: real HTTP requests through security, controller, service and the Postgres database,
 * each signed with a real JWT like the gateway forwards. Needs the billing-db from docker-compose.
 *
 * Only resident-service is replaced (it's another service, and this test is about billing):
 * it reports two apartments.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InvoiceApiTest {

    // A period and apartment ids no real or seeded data uses, so the tests can clean up after themselves.
    private static final String PERIOD = "2099-01";
    private static final long APARTMENT_A = 990_001L;
    private static final long APARTMENT_B = 990_002L;

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;
    @Autowired InvoiceRepository invoices;

    @MockitoBean ResidentServiceClient residentService;

    @BeforeEach
    void setUp() {
        when(residentService.getAllApartments()).thenReturn(List.of(
                new ApartmentDto(APARTMENT_A, "H0101"),
                new ApartmentDto(APARTMENT_B, "H0102")));
    }

    @AfterEach
    void tearDown() {
        invoices.deleteAll(invoices.findByPeriod(PERIOD));
    }

    private String board() {
        return "Bearer " + jwt.issueToken("990999", "board@test", true, null);
    }

    private String residentIn(long apartmentId) {
        return "Bearer " + jwt.issueToken("990100", "resident@test", false, apartmentId);
    }

    private Invoice invoiceFor(long apartmentId) {
        return invoices.findByPeriod(PERIOD).stream()
                .filter(i -> i.getApartmentId() == apartmentId)
                .findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Generating monthly invoices creates one per apartment, and running it again creates none")
    void generatingTwiceDoesNotCreateDuplicates() throws Exception {
        // Given two apartments and no invoices for the period
        // When the board generates invoices
        mvc.perform(post("/invoices/generate").param("period", PERIOD).header("Authorization", board()))
                // Then each apartment gets one
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // When the board (or a double click) runs it again
        mvc.perform(post("/invoices/generate").param("period", PERIOD).header("Authorization", board()))
                // Then nothing new is created
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        assertThat(invoices.findByPeriod(PERIOD)).hasSize(2);
    }

    @Test
    @DisplayName("A resident pays their invoice, it can't be paid twice, and the board's overview reflects it")
    void residentPaysAndBoardSeesIt() throws Exception {
        // Given invoices for both apartments
        mvc.perform(post("/invoices/generate").param("period", PERIOD).header("Authorization", board()))
                .andExpect(status().isOk());
        Long invoiceId = invoiceFor(APARTMENT_A).getId();

        // When the resident in apartment A pays
        mvc.perform(post("/invoices/{id}/pay", invoiceId).header("Authorization", residentIn(APARTMENT_A)))
                // Then it is paid
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        // And paying again is rejected instead of silently "paying" twice
        mvc.perform(post("/invoices/{id}/pay", invoiceId).header("Authorization", residentIn(APARTMENT_A)))
                .andExpect(status().isConflict());

        // And the board's overview counts one paid and one still outstanding
        // (2099 is in the future, so the other one is pending rather than overdue)
        mvc.perform(get("/invoices/overview").param("period", PERIOD).header("Authorization", board()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paid").value(1))
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.overdue").value(0));
    }

    @Test
    @DisplayName("A resident can neither see nor pay another apartment's invoice")
    void residentCannotTouchAnotherApartmentsInvoice() throws Exception {
        // Given an invoice for apartment B
        mvc.perform(post("/invoices/generate").param("period", PERIOD).header("Authorization", board()))
                .andExpect(status().isOk());
        Long neighboursInvoice = invoiceFor(APARTMENT_B).getId();

        // When a resident of apartment A tries to read or pay it
        // Then it looks like it doesn't exist, so they can't even learn that it's there
        mvc.perform(get("/invoices/{id}", neighboursInvoice).header("Authorization", residentIn(APARTMENT_A)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/invoices/{id}/pay", neighboursInvoice).header("Authorization", residentIn(APARTMENT_A)))
                .andExpect(status().isNotFound());

        // And the neighbour's invoice is untouched
        assertThat(invoices.findById(neighboursInvoice).orElseThrow().getStatus()).isEqualTo(InvoiceStatus.PENDING);
    }

    @Test
    @DisplayName("Only the board can generate invoices")
    void residentCannotGenerateInvoices() throws Exception {
        mvc.perform(post("/invoices/generate").param("period", PERIOD).header("Authorization", residentIn(APARTMENT_A)))
                .andExpect(status().isForbidden());

        assertThat(invoices.findByPeriod(PERIOD)).isEmpty();
    }
}
