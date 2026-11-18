package org.conrad.maintenanceservice.service;

import org.conrad.maintenanceservice.dto.CommentResponse;
import org.conrad.maintenanceservice.model.CommentAuthorType;
import org.conrad.maintenanceservice.model.MaintenanceComment;
import org.conrad.maintenanceservice.model.MaintenanceRequest;
import org.conrad.maintenanceservice.model.MaintenanceStatus;
import org.conrad.maintenanceservice.model.MaintenanceWorker;
import org.conrad.maintenanceservice.repository.MaintenanceCommentRepository;
import org.conrad.maintenanceservice.repository.MaintenanceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
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
class CommentServiceTest {

    private static final Long REQUEST_ID = 10L;
    private static final Long WORKER_ID = 1L;
    private static final Long RESIDENT_ID = 2L;

    @Mock
    private MaintenanceRequestRepository requestRepository;
    @Mock
    private MaintenanceCommentRepository commentRepository;

    @InjectMocks
    private CommentService service;

    private MaintenanceRequest request;

    @BeforeEach
    void setUp() {
        MaintenanceWorker worker = new MaintenanceWorker();
        worker.setId(WORKER_ID);
        worker.setName("Per Rørlegger");

        request = new MaintenanceRequest();
        request.setId(REQUEST_ID);
        request.setResidentId(RESIDENT_ID);
        request.setStatus(MaintenanceStatus.IN_PROGRESS);
        request.setAssignedWorker(worker);
    }

    @Test
    void assignedWorkerCanComment() {
        when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(commentRepository.save(any(MaintenanceComment.class))).thenAnswer(i -> i.getArgument(0));

        CommentResponse result = service.addAsWorker(REQUEST_ID, WORKER_ID, "  Replaced the valve  ");

        assertThat(result.authorType()).isEqualTo(CommentAuthorType.WORKER);
        assertThat(result.authorName()).isEqualTo("Per Rørlegger");
        assertThat(result.text()).isEqualTo("Replaced the valve");
    }

    @Test
    void workerCanCommentAfterRequestIsResolved() {
        request.setStatus(MaintenanceStatus.RESOLVED);
        when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
        when(commentRepository.save(any(MaintenanceComment.class))).thenAnswer(i -> i.getArgument(0));

        assertThat(service.addAsWorker(REQUEST_ID, WORKER_ID, "Done").text()).isEqualTo("Done");
    }

    @Test
    void otherWorkerCannotComment() {
        when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.addAsWorker(REQUEST_ID, 99L, "Hi"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void ownerCanCommentOnOpenRequest() {
        when(requestRepository.findByIdAndResidentId(REQUEST_ID, RESIDENT_ID)).thenReturn(Optional.of(request));
        when(commentRepository.save(any(MaintenanceComment.class))).thenAnswer(i -> i.getArgument(0));

        CommentResponse result = service.addAsResident(REQUEST_ID, RESIDENT_ID, "It is leaking again");

        assertThat(result.authorType()).isEqualTo(CommentAuthorType.RESIDENT);
    }

    @Test
    void residentCannotCommentOnResolvedRequest() {
        request.setStatus(MaintenanceStatus.RESOLVED);
        when(requestRepository.findByIdAndResidentId(REQUEST_ID, RESIDENT_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.addAsResident(REQUEST_ID, RESIDENT_ID, "Hi"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void residentCannotSeeSomeoneElsesRequest() {
        when(requestRepository.findByIdAndResidentId(REQUEST_ID, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addAsResident(REQUEST_ID, 99L, "Hi"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
