package com.grantdesk.compliance;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ComplianceController {

    private final DecisionRepository decisions;

    public ComplianceController(DecisionRepository decisions) {
        this.decisions = decisions;
    }

    @GetMapping("/decisions/{drawId}")
    public DecisionEntity get(@PathVariable String drawId) {
        return decisions.findById(drawId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "decision not found"));
    }
}
