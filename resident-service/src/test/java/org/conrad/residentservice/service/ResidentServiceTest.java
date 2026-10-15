package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.HouseholdMemberResponse;
import org.conrad.residentservice.dto.ResidentBoardUpdateRequest;
import org.conrad.residentservice.dto.ResidentCreateRequest;
import org.conrad.residentservice.dto.ResidentSummaryResponse;
import org.conrad.residentservice.dto.ResidentUpdateRequest;
import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ApartmentRepository;
import org.conrad.residentservice.repository.ResidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResidentServiceTest {

    @Mock
    private ResidentRepository residentRepository;
    @Mock
    private ApartmentRepository apartmentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ResidentService residentService;

    private Apartment apartment1;
    private Apartment apartment2;

    @BeforeEach
    void setUp() {
        apartment1 = Apartment.builder().id(10L).apartmentNumber("A1").floor(1).buildingName("Main").build();
        apartment2 = Apartment.builder().id(20L).apartmentNumber("A2").floor(2).buildingName("Main").build();
    }

    private Resident resident(Long id, String first, String last, String email, boolean manager, boolean active, Apartment apartment) {
        return Resident.builder()
                .id(id)
                .firstName(first)
                .lastName(last)
                .email(email)
                .phone("111")
                .passwordHash("hash")
                .manager(manager)
                .active(active)
                .apartment(apartment)
                .build();
    }

    private static void assertStatus(Throwable thrown, HttpStatus expected) {
        assertThat(thrown).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(expected.value()));
    }

    // ---- create ----

    @Test
    void create_savesResidentWithEncodedPassword() {
        when(apartmentRepository.findById(10L)).thenReturn(Optional.of(apartment1));
        when(passwordEncoder.encode("secret")).thenReturn("hashed");

        residentService.create("1", new ResidentCreateRequest("Ola", "Nordmann", "ola@x.no", "123", "secret", true, 10L));

        ArgumentCaptor<Resident> captor = ArgumentCaptor.forClass(Resident.class);
        verify(residentRepository).save(captor.capture());
        Resident saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("Ola");
        assertThat(saved.getLastName()).isEqualTo("Nordmann");
        assertThat(saved.getEmail()).isEqualTo("ola@x.no");
        assertThat(saved.getPhone()).isEqualTo("123");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.isManager()).isTrue();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getApartment()).isSameAs(apartment1);
    }

    @Test
    void create_unknownApartment_throwsNotFoundAndDoesNotSave() {
        when(apartmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> residentService.create("1",
                new ResidentCreateRequest("Ola", "Nordmann", "ola@x.no", null, "secret", false, 99L)))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));

        verify(residentRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    // ---- household ----

    @Test
    void household_returnsActiveApartmentMembersSortedByLastThenFirstName() {
        Resident caller = resident(1L, "Kari", "Aas", "kari@x.no", false, true, apartment1);
        when(residentRepository.findById(1L)).thenReturn(Optional.of(caller));
        when(residentRepository.findByApartmentIdAndActiveTrue(10L)).thenReturn(List.of(
                resident(3L, "Zoe", "Berg", "zoe@x.no", false, true, apartment1),
                resident(2L, "Anna", "Berg", "anna@x.no", false, true, apartment1),
                caller));

        List<HouseholdMemberResponse> household = residentService.household(1L);

        assertThat(household).extracting(HouseholdMemberResponse::firstName)
                .containsExactly("Kari", "Anna", "Zoe");
    }

    @Test
    void household_inactiveCaller_throwsForbidden() {
        when(residentRepository.findById(1L))
                .thenReturn(Optional.of(resident(1L, "Kari", "Aas", "kari@x.no", false, false, apartment1)));

        assertThatThrownBy(() -> residentService.household(1L))
                .satisfies(t -> assertStatus(t, HttpStatus.FORBIDDEN));

        verify(residentRepository, never()).findByApartmentIdAndActiveTrue(any());
    }

    @Test
    void household_unknownCaller_throwsNotFound() {
        when(residentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> residentService.household(1L))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    // ---- updateOwnContact ----

    @Test
    void updateOwnContact_updatesEmailAndTrimsPhone() {
        Resident me = resident(1L, "Kari", "Aas", "old@x.no", false, true, apartment1);
        when(residentRepository.findById(1L)).thenReturn(Optional.of(me));
        when(residentRepository.existsByEmail("new@x.no")).thenReturn(false);

        ResidentSummaryResponse response = residentService.updateOwnContact(1L, new ResidentUpdateRequest("new@x.no", "  99887766 "));

        assertThat(response.email()).isEqualTo("new@x.no");
        assertThat(response.phone()).isEqualTo("99887766");
        verify(residentRepository).save(me);
    }

    @Test
    void updateOwnContact_sameEmail_skipsUniquenessCheck() {
        Resident me = resident(1L, "Kari", "Aas", "same@x.no", false, true, apartment1);
        when(residentRepository.findById(1L)).thenReturn(Optional.of(me));

        residentService.updateOwnContact(1L, new ResidentUpdateRequest("same@x.no", null));

        verify(residentRepository, never()).existsByEmail(any());
    }

    @Test
    void updateOwnContact_duplicateEmail_throwsConflictAndDoesNotSave() {
        Resident me = resident(1L, "Kari", "Aas", "old@x.no", false, true, apartment1);
        when(residentRepository.findById(1L)).thenReturn(Optional.of(me));
        when(residentRepository.existsByEmail("taken@x.no")).thenReturn(true);

        assertThatThrownBy(() -> residentService.updateOwnContact(1L, new ResidentUpdateRequest("taken@x.no", null)))
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));

        verify(residentRepository, never()).save(any());
        assertThat(me.getEmail()).isEqualTo("old@x.no");
    }

    @Test
    void updateOwnContact_blankEmail_throwsBadRequest() {
        when(residentRepository.findById(1L))
                .thenReturn(Optional.of(resident(1L, "Kari", "Aas", "old@x.no", false, true, apartment1)));

        assertThatThrownBy(() -> residentService.updateOwnContact(1L, new ResidentUpdateRequest("   ", null)))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateOwnContact_phoneLongerThan20Characters_throwsBadRequest() {
        when(residentRepository.findById(1L))
                .thenReturn(Optional.of(resident(1L, "Kari", "Aas", "old@x.no", false, true, apartment1)));

        assertThatThrownBy(() -> residentService.updateOwnContact(1L, new ResidentUpdateRequest(null, "1".repeat(21))))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
    }

    @Test
    void updateOwnContact_blankPhone_clearsPhone() {
        Resident me = resident(1L, "Kari", "Aas", "old@x.no", false, true, apartment1);
        when(residentRepository.findById(1L)).thenReturn(Optional.of(me));

        ResidentSummaryResponse response = residentService.updateOwnContact(1L, new ResidentUpdateRequest(null, "   "));

        assertThat(response.phone()).isNull();
    }

    @Test
    void updateOwnContact_nullFields_leaveValuesUnchanged() {
        Resident me = resident(1L, "Kari", "Aas", "old@x.no", false, true, apartment1);
        when(residentRepository.findById(1L)).thenReturn(Optional.of(me));

        ResidentSummaryResponse response = residentService.updateOwnContact(1L, new ResidentUpdateRequest(null, null));

        assertThat(response.email()).isEqualTo("old@x.no");
        assertThat(response.phone()).isEqualTo("111");
        verify(residentRepository, never()).existsByEmail(any());
    }

    @Test
    void updateOwnContact_inactiveCaller_throwsForbidden() {
        when(residentRepository.findById(1L))
                .thenReturn(Optional.of(resident(1L, "Kari", "Aas", "old@x.no", false, false, apartment1)));

        assertThatThrownBy(() -> residentService.updateOwnContact(1L, new ResidentUpdateRequest("a@x.no", null)))
                .satisfies(t -> assertStatus(t, HttpStatus.FORBIDDEN));

        verify(residentRepository, never()).save(any());
    }

    // ---- update (board) ----

    @Test
    void update_appliesOnlyProvidedFields() {
        Resident target = resident(2L, "Ola", "Nordmann", "ola@x.no", false, true, apartment1);
        when(residentRepository.findById(2L)).thenReturn(Optional.of(target));

        ResidentSummaryResponse response = residentService.update("1", 2L,
                new ResidentBoardUpdateRequest(null, "Hansen", null, null, null, null, null));

        assertThat(response.firstName()).isEqualTo("Ola");
        assertThat(response.lastName()).isEqualTo("Hansen");
        assertThat(response.email()).isEqualTo("ola@x.no");
        assertThat(response.apartmentId()).isEqualTo(10L);
        verify(residentRepository).save(target);
    }

    @Test
    void update_movesResidentToOtherApartment() {
        Resident target = resident(2L, "Ola", "Nordmann", "ola@x.no", false, true, apartment1);
        when(residentRepository.findById(2L)).thenReturn(Optional.of(target));
        when(apartmentRepository.findById(20L)).thenReturn(Optional.of(apartment2));

        ResidentSummaryResponse response = residentService.update("1", 2L,
                new ResidentBoardUpdateRequest(null, null, null, null, null, null, 20L));

        assertThat(response.apartmentId()).isEqualTo(20L);
        assertThat(response.apartmentNumber()).isEqualTo("A2");
    }

    @Test
    void update_unknownApartment_throwsNotFound() {
        when(residentRepository.findById(2L))
                .thenReturn(Optional.of(resident(2L, "Ola", "Nordmann", "ola@x.no", false, true, apartment1)));
        when(apartmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> residentService.update("1", 2L,
                new ResidentBoardUpdateRequest(null, null, null, null, null, null, 99L)))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));

        verify(residentRepository, never()).save(any());
    }

    @Test
    void update_unknownResident_throwsNotFound() {
        when(residentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> residentService.update("1", 2L,
                new ResidentBoardUpdateRequest(null, "X", null, null, null, null, null)))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    @Test
    void update_selfDemotion_throwsConflictAndDoesNotSave() {
        when(residentRepository.findById(2L))
                .thenReturn(Optional.of(resident(2L, "Board", "Member", "b@x.no", true, true, apartment1)));

        assertThatThrownBy(() -> residentService.update("2", 2L,
                new ResidentBoardUpdateRequest(null, null, null, null, false, null, null)))
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));

        verify(residentRepository, never()).save(any());
    }

    @Test
    void update_selfDeactivation_throwsConflict() {
        when(residentRepository.findById(2L))
                .thenReturn(Optional.of(resident(2L, "Board", "Member", "b@x.no", true, true, apartment1)));

        assertThatThrownBy(() -> residentService.update("2", 2L,
                new ResidentBoardUpdateRequest(null, null, null, null, null, false, null)))
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));
    }

    @Test
    void update_selfEditThatKeepsBoardRole_isAllowed() {
        Resident self = resident(2L, "Board", "Member", "b@x.no", true, true, apartment1);
        when(residentRepository.findById(2L)).thenReturn(Optional.of(self));

        ResidentSummaryResponse response = residentService.update("2", 2L,
                new ResidentBoardUpdateRequest(null, "Renamed", null, null, true, true, null));

        assertThat(response.lastName()).isEqualTo("Renamed");
        assertThat(response.manager()).isTrue();
        assertThat(response.active()).isTrue();
    }

    @Test
    void update_otherBoardMemberCanBeDemotedAndDeactivated() {
        Resident other = resident(3L, "Other", "Board", "o@x.no", true, true, apartment1);
        when(residentRepository.findById(3L)).thenReturn(Optional.of(other));

        ResidentSummaryResponse response = residentService.update("1", 3L,
                new ResidentBoardUpdateRequest(null, null, null, null, false, false, null));

        assertThat(response.manager()).isFalse();
        assertThat(response.active()).isFalse();
    }

    @Test
    void update_activeTrue_reactivatesResident() {
        Resident removed = resident(2L, "Ola", "Nordmann", "ola@x.no", false, false, apartment1);
        when(residentRepository.findById(2L)).thenReturn(Optional.of(removed));

        ResidentSummaryResponse response = residentService.update("1", 2L,
                new ResidentBoardUpdateRequest(null, null, null, null, null, true, null));

        assertThat(response.active()).isTrue();
    }

    @Test
    void update_duplicateEmail_throwsConflict() {
        when(residentRepository.findById(2L))
                .thenReturn(Optional.of(resident(2L, "Ola", "Nordmann", "ola@x.no", false, true, apartment1)));
        when(residentRepository.existsByEmail("taken@x.no")).thenReturn(true);

        assertThatThrownBy(() -> residentService.update("1", 2L,
                new ResidentBoardUpdateRequest(null, null, "taken@x.no", null, null, null, null)))
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));

        verify(residentRepository, never()).save(any());
    }

    @Test
    void update_blankName_throwsBadRequest() {
        when(residentRepository.findById(2L))
                .thenReturn(Optional.of(resident(2L, "Ola", "Nordmann", "ola@x.no", false, true, apartment1)));

        assertThatThrownBy(() -> residentService.update("1", 2L,
                new ResidentBoardUpdateRequest("  ", null, null, null, null, null, null)))
                .satisfies(t -> assertStatus(t, HttpStatus.BAD_REQUEST));
    }

    // ---- remove ----

    @Test
    void remove_deactivatesResidentInsteadOfDeleting() {
        Resident target = resident(2L, "Ola", "Nordmann", "ola@x.no", false, true, apartment1);
        when(residentRepository.findById(2L)).thenReturn(Optional.of(target));

        residentService.remove("1", 2L);

        assertThat(target.isActive()).isFalse();
        verify(residentRepository).save(target);
        verify(residentRepository, never()).delete(any());
        verify(residentRepository, never()).deleteById(any());
    }

    @Test
    void remove_self_throwsConflictWithoutTouchingRepository() {
        assertThatThrownBy(() -> residentService.remove("1", 1L))
                .satisfies(t -> assertStatus(t, HttpStatus.CONFLICT));

        verifyNoInteractions(residentRepository);
    }

    @Test
    void remove_unknownResident_throwsNotFound() {
        when(residentRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> residentService.remove("1", 2L))
                .satisfies(t -> assertStatus(t, HttpStatus.NOT_FOUND));
    }

    @Test
    void remove_alreadyInactiveResident_staysInactive() {
        Resident removed = resident(2L, "Ola", "Nordmann", "ola@x.no", false, false, apartment1);
        when(residentRepository.findById(2L)).thenReturn(Optional.of(removed));

        residentService.remove("1", 2L);

        assertThat(removed.isActive()).isFalse();
    }

    // ---- search ----

    @Test
    void search_trimsQueryAndMapsToSummaries() {
        Resident ola = resident(2L, "Ola", "Nordmann", "ola@x.no", true, true, apartment1);
        when(residentRepository.search("ola", true)).thenReturn(List.of(ola));

        List<ResidentSummaryResponse> result = residentService.search("  ola  ", true);

        assertThat(result).hasSize(1);
        ResidentSummaryResponse summary = result.get(0);
        assertThat(summary.id()).isEqualTo(2L);
        assertThat(summary.email()).isEqualTo("ola@x.no");
        assertThat(summary.manager()).isTrue();
        assertThat(summary.apartmentNumber()).isEqualTo("A1");
    }
}
