package com.grantdesk.acknowledgement;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AcknowledgementController {

    private final AckRepository acknowledgements;

    public AcknowledgementController(AckRepository acknowledgements) {
        this.acknowledgements = acknowledgements;
    }

    @GetMapping("/acknowledgements/{drawId}")
    public AckEntity get(@PathVariable String drawId) {
        return acknowledgements.findById(drawId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "acknowledgement not found"));
    }
}
