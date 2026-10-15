package org.conrad.residentservice.service;

import org.conrad.residentservice.dto.MeResponse;
import org.conrad.residentservice.model.Apartment;
import org.conrad.residentservice.model.Resident;
import org.conrad.residentservice.repository.ResidentRepository;
import org.conrad.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private ResidentRepository residentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Apartment apartment;

    @BeforeEach
    void setUp() {
        apartment = Apartment.builder().id(10L).apartmentNumber("A1").floor(3).buildingName("Main").build();
    }

    private Resident resident(boolean active) {
        return Resident.builder()
                .id(1L)
                .firstName("Kari")
                .lastName("Aas")
                .email("kari@x.no")
                .phone("111")
                .passwordHash("hash")
                .manager(true)
                .active(active)
                .apartment(apartment)
                .build();
    }

    @Test
    void login_validCredentials_returnsToken() {
        when(residentRepository.findByEmail("kari@x.no")).thenReturn(Optional.of(resident(true)));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(jwtService.issueToken("1", "kari@x.no", true, 10L)).thenReturn("jwt-token");

        assertThat(authService.login("kari@x.no", "pw")).contains("jwt-token");
    }

    @Test
    void login_wrongPassword_returnsEmptyAndIssuesNoToken() {
        when(residentRepository.findByEmail("kari@x.no")).thenReturn(Optional.of(resident(true)));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThat(authService.login("kari@x.no", "wrong")).isEmpty();

        verifyNoInteractions(jwtService);
    }

    @Test
    void login_unknownEmail_returnsEmpty() {
        when(residentRepository.findByEmail("nobody@x.no")).thenReturn(Optional.empty());

        assertThat(authService.login("nobody@x.no", "pw")).isEmpty();

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void login_inactiveResident_returnsEmptyWithoutCheckingPassword() {
        when(residentRepository.findByEmail("kari@x.no")).thenReturn(Optional.of(resident(false)));

        assertThat(authService.login("kari@x.no", "pw")).isEmpty();

        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    void getMe_returnsResidentAndApartmentDetails() {
        when(residentRepository.findById(1L)).thenReturn(Optional.of(resident(true)));

        MeResponse me = authService.getMe(1L);

        assertThat(me.id()).isEqualTo(1L);
        assertThat(me.firstName()).isEqualTo("Kari");
        assertThat(me.lastName()).isEqualTo("Aas");
        assertThat(me.email()).isEqualTo("kari@x.no");
        assertThat(me.phone()).isEqualTo("111");
        assertThat(me.manager()).isTrue();
        assertThat(me.active()).isTrue();
        assertThat(me.apartmentId()).isEqualTo(10L);
        assertThat(me.apartmentNumber()).isEqualTo("A1");
        assertThat(me.floor()).isEqualTo(3);
        assertThat(me.buildingName()).isEqualTo("Main");
    }

    @Test
    void getMe_inactiveResident_stillReturnsDetailsWithActiveFalse() {
        when(residentRepository.findById(1L)).thenReturn(Optional.of(resident(false)));

        assertThat(authService.getMe(1L).active()).isFalse();
    }

    @Test
    void getMe_unknownResident_throwsNotFound() {
        when(residentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe(1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(HttpStatus.NOT_FOUND.value()));
    }
}
