package org.conrad.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Answer for a route whose circuit breaker is open or whose service didn't respond in time. The
 * gateway forwards here instead of leaving the browser waiting or showing a raw proxy error.
 */
@RestController
class FallbackController {

    @RequestMapping("/fallback/{service}")
    ResponseEntity<Map<String, String>> unavailable(@PathVariable String service) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "10")
                .body(Map.of(
                        "error", "Service unavailable",
                        "service", service,
                        "message", "The " + service + " service isn't responding right now. Try again in a moment."));
    }
}
