package com.grantdesk.draw;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grantdesk.common.AckCommand;
import com.grantdesk.common.Channels;
import com.grantdesk.common.DrawFact;
import com.grantdesk.common.ReturnCommand;
import com.grantdesk.common.ScoreCommand;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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
public class DrawController {

    private static final Set<String> RETURNABLE = Set.of("PAID", "ACKNOWLEDGED");

    private final DrawRepository draws;
    private final DrawActionRepository actions;
    private final SponsorDirectoryRepository sponsors;
    private final ProgramDirectoryRepository programs;
    private final GranteeDirectoryRepository grantees;
    private final MilestoneDirectoryRepository milestones;
    private final AwardDirectoryRepository awards;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;
    private final EntityManager em;

    public DrawController(DrawRepository draws, DrawActionRepository actions, SponsorDirectoryRepository sponsors,
            ProgramDirectoryRepository programs, GranteeDirectoryRepository grantees, MilestoneDirectoryRepository milestones,
            AwardDirectoryRepository awards, OutboxRepository outbox, ObjectMapper mapper, EntityManager em) {
        this.draws = draws;
        this.actions = actions;
        this.sponsors = sponsors;
        this.programs = programs;
        this.grantees = grantees;
        this.milestones = milestones;
        this.awards = awards;
        this.outbox = outbox;
        this.mapper = mapper;
        this.em = em;
    }

    @PostMapping("/draws")
    @Transactional
    public ResponseEntity<DrawEntity> create(@RequestBody CreateDraw request) {
        if (request == null || blank(request.requestId()) || blank(request.sponsorId()) || blank(request.programId())
                || blank(request.granteeId()) || blank(request.milestoneId()) || blank(request.awardId())
                || request.units() == null || request.units() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "draw fields are required");
        }
        DrawEntity existing = draws.findByRequestId(request.requestId().trim()).orElse(null);
        if (existing != null) {
            return ResponseEntity.ok(existing);
        }
        SponsorDirectory sponsor = sponsors.findById(request.sponsorId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "sponsor not found"));
        ProgramDirectory program = programs.findById(request.programId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "program not found"));
        GranteeDirectory grantee = grantees.findById(request.granteeId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "grantee not found"));
        MilestoneDirectory milestone = milestones.findById(request.milestoneId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "milestone not found"));
        AwardDirectory award = awards.findById(request.awardId().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "award not found"));
        if (!"OPEN".equals(program.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "program is closed");
        }
        if (!"OPEN".equals(milestone.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "milestone is closed");
        }
        if (!program.getId().equals(milestone.getProgramId()) || !program.getId().equals(award.getProgramId())
                || !sponsor.getId().equals(award.getSponsorId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "draw does not belong to that program and sponsor");
        }
        DrawEntity row = new DrawEntity();
        row.setId(UUID.randomUUID().toString());
        row.setRequestId(request.requestId().trim());
        row.setSponsorId(sponsor.getId());
        row.setProgramId(program.getId());
        row.setGranteeId(grantee.getId());
        row.setMilestoneId(milestone.getId());
        row.setAwardId(award.getId());
        row.setGranteeCode(grantee.getGranteeCode());
        row.setUnits(request.units());
        row.setUnitCents(award.getUnitCents());
        row.setAmountCents(0);
        row.setStatus("SCORE");
        row.setReason("");
        draws.save(row);
        OutboxWriter.kafka(outbox, mapper, Channels.DRAWS, row.getId(), new DrawFact(row.getId(), row.getStatus(), ""));
        OutboxWriter.rabbit(outbox, mapper, Channels.RATE, row.getId(),
                new ScoreCommand(row.getId(), row.getUnits(), row.getUnitCents()));
        return ResponseEntity.status(HttpStatus.CREATED).body(row);
    }

    @GetMapping("/draws/{id}")
    public DrawEntity get(@PathVariable String id) {
        return draws.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "draw not found"));
    }

    @PostMapping("/draws/{id}/acknowledgements")
    @Transactional
    public ResponseEntity<DrawEntity> acknowledge(@PathVariable String id, @RequestBody ActionRequest request) {
        DrawEntity prior = replay(request);
        if (prior != null) {
            return ResponseEntity.ok(prior);
        }
        DrawEntity row = lock(id);
        if (!"PAID".equals(row.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "draw is closed");
        }
        remember(request.requestId().trim(), row.getId(), "ACK");
        OutboxWriter.rabbit(outbox, mapper, Channels.ACK, row.getId(), new AckCommand(row.getId()));
        return ResponseEntity.accepted().body(row);
    }

    @PostMapping("/draws/{id}/clawbacks")
    @Transactional
    public ResponseEntity<DrawEntity> clawback(@PathVariable String id, @RequestBody ActionRequest request) {
        DrawEntity prior = replay(request);
        if (prior != null) {
            return ResponseEntity.ok(prior);
        }
        DrawEntity row = lock(id);
        if (!RETURNABLE.contains(row.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "draw is closed");
        }
        remember(request.requestId().trim(), row.getId(), "RETURN");
        OutboxWriter.rabbit(outbox, mapper, Channels.RETURN, row.getId(),
                new ReturnCommand(row.getId(), row.getAwardId(), row.getAmountCents()));
        return ResponseEntity.accepted().body(row);
    }

    @GetMapping("/directory/sponsors/{id}")
    public SponsorDirectory sponsor(@PathVariable String id) {
        return sponsors.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "sponsor not found"));
    }

    @GetMapping("/directory/programs/{id}")
    public ProgramDirectory program(@PathVariable String id) {
        return programs.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "program not found"));
    }

    @GetMapping("/directory/grantees/{id}")
    public GranteeDirectory grantee(@PathVariable String id) {
        return grantees.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "grantee not found"));
    }

    @GetMapping("/directory/milestones/{id}")
    public MilestoneDirectory milestone(@PathVariable String id) {
        return milestones.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "milestone not found"));
    }

    @GetMapping("/directory/awards/{id}")
    public AwardDirectory award(@PathVariable String id) {
        return awards.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "award not found"));
    }

    private DrawEntity replay(ActionRequest request) {
        if (request == null || blank(request.requestId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request id is required");
        }
        DrawAction prior = actions.findById(request.requestId().trim()).orElse(null);
        if (prior == null) {
            return null;
        }
        return draws.findById(prior.getDrawId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "draw not found"));
    }

    private void remember(String requestId, String drawId, String kind) {
        DrawAction action = new DrawAction();
        action.setId(requestId);
        action.setDrawId(drawId);
        action.setKind(kind);
        actions.save(action);
    }

    private DrawEntity lock(String id) {
        DrawEntity row = em.find(DrawEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (row == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "draw not found");
        }
        return row;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public record CreateDraw(String requestId, String sponsorId, String programId, String granteeId, String milestoneId,
            String awardId, Integer units) {
    }

    public record ActionRequest(String requestId) {
    }
}
