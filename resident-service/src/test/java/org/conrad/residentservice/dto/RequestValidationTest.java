package org.conrad.residentservice.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** The input rules written as annotations on the request records. No Spring and no database. */
class RequestValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static Set<String> invalidFields(Object request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    private static ResidentCreateRequest resident(String first, String last, String email, String phone,
                                                  String password, Long apartmentId) {
        return new ResidentCreateRequest(first, last, email, phone, password, false, apartmentId);
    }

    // --- create resident: this had no validation at all before ---

    @Test
    void aCompleteResidentIsAccepted() {
        assertThat(invalidFields(resident("Kari", "Nordmann", "kari@example.com", "91000001", "longenough", 1L)))
                .isEmpty();
    }

    @Test
    void phoneIsOptional() {
        assertThat(invalidFields(resident("Kari", "Nordmann", "kari@example.com", null, "longenough", 1L)))
                .isEmpty();
    }

    @Test
    void missingOrBlankFieldsAreRejected() {
        assertThat(invalidFields(resident(null, "  ", null, null, null, null)))
                .containsExactlyInAnyOrder("firstName", "lastName", "email", "password", "apartmentId");
    }

    @Test
    void anEmailThatIsNotAnEmailIsRejected() {
        assertThat(invalidFields(resident("Kari", "N", "not-an-email", null, "longenough", 1L)))
                .containsExactly("email");
    }

    @Test
    void valuesLongerThanTheDatabaseColumnsAreRejected() {
        assertThat(invalidFields(resident("x".repeat(101), "x".repeat(101), "a@b.no", "1".repeat(21),
                "longenough", 1L)))
                .containsExactlyInAnyOrder("firstName", "lastName", "phone");
    }

    @Test
    void aShortPasswordIsRejected() {
        assertThat(invalidFields(resident("Kari", "N", "a@b.no", null, "short77", 1L))).containsExactly("password");
        assertThat(invalidFields(resident("Kari", "N", "a@b.no", null, "exactly8", 1L))).isEmpty();
    }

    @Test
    void aPasswordBcryptCouldNotUseInFullIsRejected() {
        assertThat(invalidFields(resident("Kari", "N", "a@b.no", null, "x".repeat(73), 1L)))
                .containsExactly("password");
    }

    // --- login ---

    @Test
    void loginNeedsBothFields() {
        assertThat(invalidFields(new LoginRequest(null, ""))).containsExactlyInAnyOrder("email", "password");
        assertThat(invalidFields(new LoginRequest("kari@example.com", "pw"))).isEmpty();
    }

    // --- apartments and buildings ---

    @Test
    void anApartmentNeedsNumberFloorAndBuilding() {
        assertThat(invalidFields(new ApartmentCreateRequest(" ", null, null)))
                .containsExactlyInAnyOrder("apartmentNumber", "floor", "buildingId");
        assertThat(invalidFields(new ApartmentCreateRequest("101", 1, 1L))).isEmpty();
    }

    @Test
    void anApartmentNumberLongerThanTheColumnIsRejected() {
        assertThat(invalidFields(new ApartmentCreateRequest("1".repeat(11), 1, 1L)))
                .containsExactly("apartmentNumber");
    }

    @Test
    void aBuildingNeedsAName() {
        assertThat(invalidFields(new BuildingRequest(null))).containsExactly("name");
        assertThat(invalidFields(new BuildingRequest("   "))).containsExactly("name");
        assertThat(invalidFields(new BuildingRequest("x".repeat(256)))).containsExactly("name");
        assertThat(invalidFields(new BuildingRequest("Solsiden"))).isEmpty();
    }

    // --- updates: a field left out means "unchanged", so null must stay valid ---

    @Test
    void anUpdateWithNothingSetIsValid() {
        assertThat(invalidFields(new ApartmentUpdateRequest(null, null, null))).isEmpty();
        assertThat(invalidFields(new ResidentUpdateRequest(null, null))).isEmpty();
        assertThat(invalidFields(new ResidentBoardUpdateRequest(null, null, null, null, null, null, null)))
                .isEmpty();
    }

    @Test
    void anUpdateStillLimitsWhatIsSent() {
        assertThat(invalidFields(new ResidentUpdateRequest("not-an-email", "1".repeat(21))))
                .containsExactlyInAnyOrder("email", "phone");
        assertThat(invalidFields(new ResidentBoardUpdateRequest("x".repeat(101), null, "bad", null, null, null, null)))
                .containsExactlyInAnyOrder("firstName", "email");
        assertThat(invalidFields(new ApartmentUpdateRequest("1".repeat(11), null, null)))
                .containsExactly("apartmentNumber");
    }
}
