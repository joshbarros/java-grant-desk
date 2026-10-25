package com.grantdesk.disbursement;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class DisbursementController {

    private final DisbursementRepository disbursements;

    public DisbursementController(DisbursementRepository disbursements) {
        this.disbursements = disbursements;
    }

    @GetMapping("/disbursements/{drawId}")
    public DisbursementEntity get(@PathVariable String drawId) {
        return disbursements.findById(drawId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "disbursement not found"));
    }
}
