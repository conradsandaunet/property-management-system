package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.NotificationResponse;
import org.conrad.residentservice.service.NotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/my")
    public List<NotificationResponse> myNotifications() {
        return notificationService.listFor(CurrentUser.requireResidentId());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id, CurrentUser.requireResidentId());
    }
}