package org.conrad.maintenanceservice.web;

import jakarta.validation.Valid;
import org.conrad.maintenanceservice.dto.CommentCreateRequest;
import org.conrad.maintenanceservice.dto.CommentResponse;
import org.conrad.maintenanceservice.service.CommentService;
import org.conrad.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/maintenance/{requestId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> add(@PathVariable Long requestId, @Valid @RequestBody
                                               CommentCreateRequest body, Authentication auth) {
        CommentResponse created = hasRole(auth, "ROLE_WORKER")
                ? commentService.addAsWorker(requestId, workerId(auth), body.text())
                : commentService.addAsResident(requestId, Long.valueOf(auth.getName()), body.text());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable Long requestId, Authentication auth) {
        if (hasRole(auth, "ROLE_WORKER")) {
            return commentService.listForWorker(requestId, workerId(auth));
        }
        if (hasRole(auth, "ROLE_BOARD")) {
            return commentService.listForBoard(requestId);
        }
        return commentService.listForResident(requestId, Long.valueOf(auth.getName()));
    }

    private static boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    private static Long workerId(Authentication auth) {
        return Long.valueOf(auth.getName().substring(JwtService.WORKER_SUBJECT_PREFIX.length()));
    }

}
