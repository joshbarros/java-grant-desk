package com.grantdesk.milestone;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.Channels;
import com.grantdesk.common.MilestoneOpened;
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
public class MilestoneController {

    private static final Set<String> STATUSES = Set.of("OPEN", "CLOSED");

    private final MilestoneRepository milestones;
    private final ProgramViewRepository programs;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public MilestoneController(MilestoneRepository milestones, ProgramViewRepository programs, OutboxRepository outbox,
            ObjectMapper mapper) {
        this.milestones = milestones;
        this.programs = programs;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/milestones")
    @Transactional
    public ResponseEntity<MilestoneEntity> create(@RequestBody CreateMilestone request) {
        if (request == null || blank(request.milestoneCode()) || blank(request.programId()) || blank(request.status())
                || !STATUSES.contains(request.status().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "milestone fields are required");
        }
        if (!programs.existsById(request.programId().trim())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "program not found");
        }
        String code = request.milestoneCode().trim();
        if (milestones.existsByMilestoneCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "milestone already exists");
        }
        MilestoneEntity row = new MilestoneEntity();
        row.setId(UUID.randomUUID().toString());
        row.setProgramId(request.programId().trim());
        row.setMilestoneCode(code);
        row.setStatus(request.status().trim());
        milestones.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.MILESTONES, row.getId(),
                new MilestoneOpened(row.getId(), row.getProgramId(), row.getMilestoneCode(), row.getStatus()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/milestones/{id}")
    public MilestoneEntity get(@PathVariable String id) {
        return milestones.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "milestone not found"));
    }

    @GetMapping("/directory/programs/{id}")
    public ProgramView program(@PathVariable String id) {
        return programs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "program not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateMilestone(String milestoneCode, String programId, String status) {
    }
}
