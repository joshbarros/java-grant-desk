package com.grantdesk.grantee;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.Channels;
import com.grantdesk.common.GranteeOpened;
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
public class GranteeController {

    private final GranteeRepository grantees;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public GranteeController(GranteeRepository grantees, OutboxRepository outbox, ObjectMapper mapper) {
        this.grantees = grantees;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    @PostMapping("/grantees")
    @Transactional
    public ResponseEntity<GranteeEntity> create(@RequestBody CreateGrantee request) {
        if (request == null || blank(request.granteeCode()) || blank(request.name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "grantee fields are required");
        }
        String code = request.granteeCode().trim();
        if (grantees.existsByGranteeCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "grantee already exists");
        }
        GranteeEntity row = new GranteeEntity();
        row.setId(UUID.randomUUID().toString());
        row.setGranteeCode(code);
        row.setName(request.name().trim());
        grantees.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.GRANTEES, row.getId(),
                new GranteeOpened(row.getId(), row.getGranteeCode(), row.getName()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/grantees/{id}")
    public GranteeEntity get(@PathVariable String id) {
        return grantees.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "grantee not found"));
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateGrantee(String granteeCode, String name) {
    }
}
