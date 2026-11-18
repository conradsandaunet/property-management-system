package org.conrad.maintenanceservice.service;

import org.conrad.maintenanceservice.dto.CommentResponse;
import org.conrad.maintenanceservice.model.CommentAuthorType;
import org.conrad.maintenanceservice.model.MaintenanceComment;
import org.conrad.maintenanceservice.model.MaintenanceRequest;
import org.conrad.maintenanceservice.model.MaintenanceStatus;
import org.conrad.maintenanceservice.model.MaintenanceWorker;
import org.conrad.maintenanceservice.repository.MaintenanceCommentRepository;
import org.conrad.maintenanceservice.repository.MaintenanceRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CommentService {

    private final MaintenanceRequestRepository requestRepository;
    private final MaintenanceCommentRepository commentRepository;

    public CommentService(MaintenanceRequestRepository requestRepository, MaintenanceCommentRepository commentRepository) {
        this.requestRepository = requestRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public CommentResponse addAsWorker(Long requestId, Long workerId, String text) {
        MaintenanceRequest request = findAssignedTo(requestId, workerId);
        return save(request, CommentAuthorType.WORKER, workerId, request.getAssignedWorker().getName(), text);
    }

    @Transactional
    public CommentResponse addAsResident(Long requestId, Long residentId, String text) {
        MaintenanceRequest request = findOwnedBy(requestId, residentId);
        if (request.getStatus() == MaintenanceStatus.RESOLVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is already resolved");
        }

        return save(request, CommentAuthorType.RESIDENT, residentId, "Resident", text);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listForWorker(Long requestId, Long workerId) {
        return list(findAssignedTo(requestId, workerId));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listForResident(Long requestId, Long residentId) {
        return list(findOwnedBy(requestId, residentId));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listForBoard(Long requestId) {
        return list(requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found")));
    }

    private MaintenanceRequest findAssignedTo(Long requestId, Long workerId) {
        MaintenanceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Request not found"));
        MaintenanceWorker assignee = request.getAssignedWorker();
        if (assignee == null || !assignee.getId().equals(workerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Request is not assigned to you");
        }
        return request;
    }

    private MaintenanceRequest findOwnedBy(Long requestId, Long residentId) {
        return requestRepository.findByIdAndResidentId(requestId, residentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Request not found"));
    }

    private CommentResponse save(MaintenanceRequest request, CommentAuthorType type,
                                 Long authorId, String authorName, String text) {
        return CommentResponse.from(commentRepository.save(MaintenanceComment.builder()
                .requestId(request.getId())
                .authorType(type)
                .authorId(authorId)
                .authorName(authorName)
                .text(text.strip())
                .build()));
    }

    private List<CommentResponse> list(MaintenanceRequest request) {
        return commentRepository.findByRequestIdOrderByCreatedAtAsc(request.getId()).stream()
                .map(CommentResponse::from)
                .toList();
    }

}
