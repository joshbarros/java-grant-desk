package com.grantdesk.program;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.Channels;
import com.grantdesk.common.ProgramOpened;
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
public class ProgramController {

    private static final Set<String> STATUSES = Set.of("OPEN", "CLOSED");

    private final ProgramRepository programs;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public ProgramController(ProgramRepository programs, OutboxRepository outbox, ObjectMapper mapper) {
        this.programs = programs;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/programs")
    @Transactional
    public ResponseEntity<ProgramEntity> create(@RequestBody CreateProgram request) {
        if (request == null || blank(request.programCode()) || blank(request.status())
                || !STATUSES.contains(request.status().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "program fields are required");
        }
        String code = request.programCode().trim();
        if (programs.existsByProgramCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "program already exists");
        }
        ProgramEntity row = new ProgramEntity();
        row.setId(UUID.randomUUID().toString());
        row.setProgramCode(code);
        row.setStatus(request.status().trim());
        programs.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.PROGRAMS, row.getId(),
                new ProgramOpened(row.getId(), row.getProgramCode(), row.getStatus()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/programs/{id}")
    public ProgramEntity get(@PathVariable String id) {
        return programs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "program not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateProgram(String programCode, String status) {
    }
}
