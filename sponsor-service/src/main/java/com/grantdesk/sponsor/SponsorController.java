package com.grantdesk.sponsor;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.Channels;
import com.grantdesk.common.SponsorOpened;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class SponsorController {

    private final SponsorRepository sponsors;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public SponsorController(SponsorRepository sponsors, OutboxRepository outbox, ObjectMapper mapper) {
        this.sponsors = sponsors;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/sponsors")
    @Transactional
    public ResponseEntity<SponsorEntity> create(@RequestBody CreateSponsor request) {
        if (request == null || blank(request.sponsorCode()) || blank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sponsor fields are required");
        }
        String code = request.sponsorCode().trim();
        if (sponsors.existsBySponsorCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "sponsor already exists");
        }
        SponsorEntity row = new SponsorEntity();
        row.setId(UUID.randomUUID().toString());
        row.setSponsorCode(code);
        row.setName(request.name().trim());
        sponsors.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.SPONSORS, row.getId(),
                new SponsorOpened(row.getId(), row.getSponsorCode(), row.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/sponsors/{id}")
    public SponsorEntity get(@PathVariable String id) {
        return sponsors.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "sponsor not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateSponsor(String sponsorCode, String name) {
    }
}
