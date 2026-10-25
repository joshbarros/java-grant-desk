package com.grantdesk.award;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.AwardOpened;
import com.grantdesk.common.Channels;
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
public class AwardController {

    private final AwardRepository awards;
    private final HoldRepository holds;
    private final SponsorViewRepository sponsors;
    private final ProgramViewRepository programs;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public AwardController(AwardRepository awards, HoldRepository holds, SponsorViewRepository sponsors,
            ProgramViewRepository programs, OutboxRepository outbox, ObjectMapper mapper) {
        this.awards = awards;
        this.holds = holds;
        this.sponsors = sponsors;
        this.programs = programs;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/awards")
    @Transactional
    public ResponseEntity<AwardEntity> create(@RequestBody CreateAward request) {
        if (request == null || blank(request.awardCode()) || blank(request.sponsorId()) || blank(request.programId())
                || request.ceilingCents() == null || request.ceilingCents() <= 0
                || request.unitCents() == null || request.unitCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "award fields are required");
        }
        if (!sponsors.existsById(request.sponsorId().trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "sponsor not found");
        }
        if (!programs.existsById(request.programId().trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "program not found");
        }
        String code = request.awardCode().trim();
        if (awards.existsByAwardCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "award already exists");
        }
        AwardEntity row = new AwardEntity();
        row.setId(UUID.randomUUID().toString());
        row.setSponsorId(request.sponsorId().trim());
        row.setProgramId(request.programId().trim());
        row.setAwardCode(code);
        row.setCeilingCents(request.ceilingCents());
        row.setUnitCents(request.unitCents());
        row.setOpenCents(request.ceilingCents());
        row.setStatus("OPEN");
        awards.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.AWARDS, row.getId(),
                new AwardOpened(row.getId(), row.getSponsorId(), row.getProgramId(), row.getAwardCode(),
                        row.getCeilingCents(), row.getUnitCents()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/awards/{id}")
    public AwardEntity get(@PathVariable String id) {
        return awards.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "award not found"));
    }

    @GetMapping("/holds/{drawId}")
    public HoldEntity hold(@PathVariable String drawId) {
        return holds.findById(drawId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "hold not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateAward(String awardCode, String sponsorId, String programId, Integer ceilingCents, Integer unitCents) {
    }
}
