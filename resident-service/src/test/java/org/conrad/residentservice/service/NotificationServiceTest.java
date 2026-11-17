package org.conrad.residentservice.service;

import org.conrad.residentservice.model.Notification;
import org.conrad.residentservice.repository.NotificationRepository;
import org.conrad.residentservice.repository.ResidentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Long RESIDENT_ID = 2L;
    private static final Long REQUEST_ID = 3L;

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ResidentRepository residentRepository;

    @InjectMocks
    private NotificationService service;

    @Test
    void completedRequestCreatesNotification() {
        when(residentRepository.existsById(RESIDENT_ID)).thenReturn(true);
        when(notificationRepository.existsByResidentIdAndRequestId(RESIDENT_ID, REQUEST_ID)).thenReturn(false);

        service.notifyMaintenanceCompleted(RESIDENT_ID, REQUEST_ID);

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void sameEventTwiceOnlyNotifiesOnce() {
        when(residentRepository.existsById(RESIDENT_ID)).thenReturn(true);
        when(notificationRepository.existsByResidentIdAndRequestId(RESIDENT_ID, REQUEST_ID)).thenReturn(true);

        service.notifyMaintenanceCompleted(RESIDENT_ID, REQUEST_ID);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void unknownResidentIsSkippedWithoutError() {
        when(residentRepository.existsById(RESIDENT_ID)).thenReturn(false);

        service.notifyMaintenanceCompleted(RESIDENT_ID, REQUEST_ID);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markReadSetsReadFlag() {
        Notification n = Notification.builder().id(1L).residentId(RESIDENT_ID).requestId(REQUEST_ID)
                .message("m").build();
        when(notificationRepository.findByIdAndResidentId(1L, RESIDENT_ID)).thenReturn(Optional.of(n));

        assertThat(service.markRead(1L, RESIDENT_ID).read()).isTrue();
    }

    @Test
    void cannotMarkSomeoneElsesNotificationAsRead() {
        when(notificationRepository.findByIdAndResidentId(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(1L, 99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}