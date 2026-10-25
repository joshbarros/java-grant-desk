package com.grantdesk.clawback;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ClawbackController {

    private final ClawbackRepository clawbacks;

    public ClawbackController(ClawbackRepository clawbacks) {
        this.clawbacks = clawbacks;
    }

    @GetMapping("/clawbacks/{drawId}")
    public ClawbackEntity get(@PathVariable String drawId) {
        return clawbacks.findById(drawId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "clawback not found"));
    }
}
