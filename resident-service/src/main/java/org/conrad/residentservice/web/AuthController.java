package org.conrad.residentservice.web;

import org.conrad.residentservice.dto.LoginRequest;
import org.conrad.residentservice.dto.LoginResponse;
import org.conrad.residentservice.dto.MeResponse;
import org.conrad.residentservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password())
                .map(token -> ResponseEntity.ok(new LoginResponse(token)))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @GetMapping("/me")
    public MeResponse me() {
        return authService.getMe(CurrentUser.requireResidentId());
    }
}
