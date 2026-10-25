#!/usr/bin/env python3
"""Drive grant desk through a paid draw, a short ceiling, a lapse release, and a clawback."""

import base64
import json
import sys
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor

PORTS = {
    "sponsor": 8280,
    "program": 8281,
    "grantee": 8282,
    "milestone": 8283,
    "award": 8284,
    "draw": 8285,
    "eligibility": 8286,
    "compliance": 8287,
    "voucher": 8288,
    "disbursement": 8289,
    "acknowledgement": 8290,
    "clawback": 8291,
    "audit": 8292,
}
MGMT = "http://127.0.0.1:15688"
AUTH = "platform:platform"
DECIDE_DLQ = "grant.compliance.decide.dlq"
MAX_INT = 2147483647
failures = []
RUN = uuid.uuid4().hex[:8]


def check(name, ok, detail=""):
    if ok:
        print(f"PASS  {name}", flush=True)
    else:
        print(f"FAIL  {name} {detail}", flush=True)
        failures.append(f"{name}: {detail}")


def call(method, url, body=None, auth=None, timeout=20):
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        request.add_header("content-type", "application/json")
    if auth:
        request.add_header("Authorization", "Basic " + base64.b64encode(auth.encode()).decode())
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode()
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as ex:
        raw = ex.read().decode()
        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = raw
        return ex.code, parsed
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError, OSError):
        return 0, None


def wait_ok(url, seconds=300, auth=None):
    deadline = time.time() + seconds
    while time.time() < deadline:
        if call("GET", url, auth=auth)[0] == 200:
            return True
        time.sleep(1)
    return False


def until(label, fn, seconds=120):
    deadline = time.time() + seconds
    last = None
    while time.time() < deadline:
        last = fn()
        if last is not None:
            return last
        time.sleep(0.4)
    check(label, False, f"timed out last={last!r}")
    return None


def depth(name):
    status, body = call("GET", f"{MGMT}/api/queues/%2F/{name}", auth=AUTH)
    if status != 200 or not isinstance(body, dict):
        return None
    return body.get("messages", 0)


def grew(name, before, amount=1):
    def ready():
        now = depth(name)
        if now is not None and now >= before + amount:
            return now
        return None

    found = until(f"dlq {name}", ready, 120)
    if found is not None:
        check(f"dlq {name}", True)
    return found


def base(name):
    return f"http://127.0.0.1:{PORTS[name]}"


def ready_dir(service, path):
    def seen():
        code, body = call("GET", f"{base(service)}{path}")
        if code == 200 and isinstance(body, dict):
            return body
        return None

    return until(f"{service} {path}", seen, 120) is not None


def load_draw(draw_id):
    code, body = call("GET", f"{base('draw')}/draws/{draw_id}")
    if code == 200 and isinstance(body, dict):
        return body
    return None


def wait_draw(draw_id, status, reason=None, seconds=180):
    def ready():
        row = load_draw(draw_id)
        if not row or row.get("status") != status:
            return None
        if reason is not None and row.get("reason") != reason:
            return None
        return row

    return until(f"{draw_id[:8]} {status}", ready, seconds)


def award_row(award_id):
    return call("GET", f"{base('award')}/awards/{award_id}")


def hold_row(draw_id):
    return call("GET", f"{base('award')}/holds/{draw_id}")


def audits(draw_id):
    code, body = call("GET", f"{base('audit')}/audits/{draw_id}")
    if code == 200 and isinstance(body, list):
        return body
    return None


def fact_count(draw_id, kind, status):
    def ready():
        rows = audits(draw_id)
        if rows is None:
            return None
        count = sum(1 for row in rows if row.get("kind") == kind and row.get("status") == status)
        if count > 0:
            return count
        return None

    return until(f"audit {kind} {status} {draw_id[:8]}", ready, 90)


def open_sponsor(code, name):
    status, body = call("POST", f"{base('sponsor')}/sponsors", {"sponsorCode": code, "name": name})
    check(f"sponsor {code}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    sponsor_id = body["id"]
    if not ready_dir("draw", f"/directory/sponsors/{sponsor_id}"):
        return None
    return sponsor_id


def open_program(code, status_name):
    status, body = call("POST", f"{base('program')}/programs", {"programCode": code, "status": status_name})
    check(f"program {code}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    program_id = body["id"]
    if not ready_dir("milestone", f"/directory/programs/{program_id}"):
        return None
    if not ready_dir("draw", f"/directory/programs/{program_id}"):
        return None
    return program_id


def open_grantee(code, name):
    status, body = call("POST", f"{base('grantee')}/grantees", {"granteeCode": code, "name": name})
    check(f"grantee {code}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    grantee_id = body["id"]
    if not ready_dir("draw", f"/directory/grantees/{grantee_id}"):
        return None
    return grantee_id


def open_milestone(program_id, code, status_name):
    status, body = call("POST", f"{base('milestone')}/milestones", {
        "milestoneCode": code,
        "programId": program_id,
        "status": status_name,
    })
    check(f"milestone {code}", status == 201 and isinstance(body, dict), body)
    if status != 201 or not isinstance(body, dict):
        return None
    milestone_id = body["id"]
    if not ready_dir("draw", f"/directory/milestones/{milestone_id}"):
        return None
    return milestone_id


def open_award(sponsor_id, program_id, code, ceiling, unit):
    def ready():
        status, body = call("POST", f"{base('award')}/awards", {
            "awardCode": code,
            "sponsorId": sponsor_id,
            "programId": program_id,
            "ceilingCents": ceiling,
            "unitCents": unit,
        })
        if status == 201 and isinstance(body, dict):
            return body
        if status in (0, 404):
            return None
        check(f"award {code}", False, (status, body))
        return "stop"

    found = until(f"award {code}", ready, 120)
    if not isinstance(found, dict):
        return None
    check(f"award {code}", found.get("openCents") == ceiling and found.get("unitCents") == unit, found)
    award_id = found["id"]
    if not ready_dir("draw", f"/directory/awards/{award_id}"):
        return None
    return award_id


def desk(prefix, ceiling, unit, units, grantee_code=None, program_status="OPEN", milestone_status="OPEN"):
    code = grantee_code or f"{prefix}-G-{RUN}"
    sponsor_id = open_sponsor(f"{prefix}-S-{RUN}", prefix)
    program_id = open_program(f"{prefix}-P-{RUN}", program_status)
    grantee_id = open_grantee(code, prefix)
    if not sponsor_id or not program_id or not grantee_id:
        return None
    milestone_id = open_milestone(program_id, f"{prefix}-M-{RUN}", milestone_status)
    award_id = open_award(sponsor_id, program_id, f"{prefix}-A-{RUN}", ceiling, unit)
    if not milestone_id or not award_id:
        return None
    return {
        "sponsor": sponsor_id,
        "program": program_id,
        "grantee": grantee_id,
        "milestone": milestone_id,
        "award": award_id,
        "ceiling": ceiling,
        "unit": unit,
        "units": units,
        "code": code,
    }


def start_draw(world, request_id=None, sponsor_id=None, program_id=None, grantee_id=None, milestone_id=None,
               award_id=None, units=None):
    request_id = request_id or str(uuid.uuid4())
    status, body = call("POST", f"{base('draw')}/draws", {
        "requestId": request_id,
        "sponsorId": sponsor_id or world["sponsor"],
        "programId": program_id or world["program"],
        "granteeId": grantee_id or world["grantee"],
        "milestoneId": milestone_id or world["milestone"],
        "awardId": award_id or world["award"],
        "units": world["units"] if units is None else units,
    })
    return status, body, request_id


def acknowledge(draw_id, request_id=None):
    return call("POST", f"{base('draw')}/draws/{draw_id}/acknowledgements", {
        "requestId": request_id or str(uuid.uuid4()),
    })


def clawback(draw_id, request_id=None):
    return call("POST", f"{base('draw')}/draws/{draw_id}/clawbacks", {
        "requestId": request_id or str(uuid.uuid4()),
    })


def hold_returned(draw_id, award_id, ceiling, seconds=8):
    deadline = time.time() + seconds
    seen = None
    live = None
    opened = None
    while time.time() < deadline:
        row = load_draw(draw_id)
        live = hold_row(draw_id)
        opened = award_row(award_id)
        seen = None if row is None else row.get("status")
        hold_status = None if live[0] != 200 or not isinstance(live[1], dict) else live[1].get("status")
        open_cents = None if opened[0] != 200 or not isinstance(opened[1], dict) else opened[1].get("openCents")
        if seen != "RETURNED" or hold_status != "RETURNED" or open_cents != ceiling:
            return False, seen, live, opened
        time.sleep(0.5)
    return True, seen, live, opened


def main():
    for name in PORTS:
        check(f"health {name}", wait_ok(f"{base(name)}/actuator/health"))
    if failures:
        return 1

    check("blank sponsor", call("POST", f"{base('sponsor')}/sponsors", {"sponsorCode": " ", "name": "North"})[0] == 400)
    check("bad program", call("POST", f"{base('program')}/programs", {"programCode": f"BAD-{RUN}", "status": "open"})[0] == 400)
    check("blank grantee", call("POST", f"{base('grantee')}/grantees", {"granteeCode": " ", "name": "Ada"})[0] == 400)
    check("bad milestone", call("POST", f"{base('milestone')}/milestones", {
        "milestoneCode": f"BAD-{RUN}", "programId": str(uuid.uuid4()), "status": "open",
    })[0] == 400)
    check("zero ceiling", call("POST", f"{base('award')}/awards", {
        "awardCode": f"ZERO-{RUN}", "sponsorId": str(uuid.uuid4()), "programId": str(uuid.uuid4()),
        "ceilingCents": 0, "unitCents": 125,
    })[0] == 400)
    check("zero unit", call("POST", f"{base('award')}/awards", {
        "awardCode": f"UNIT-{RUN}", "sponsorId": str(uuid.uuid4()), "programId": str(uuid.uuid4()),
        "ceilingCents": 5000, "unitCents": 0,
    })[0] == 400)
    check("blank draw", call("POST", f"{base('draw')}/draws", {"requestId": " "})[0] == 400)
    check("negative units", call("POST", f"{base('draw')}/draws", {
        "requestId": str(uuid.uuid4()),
        "sponsorId": str(uuid.uuid4()),
        "programId": str(uuid.uuid4()),
        "granteeId": str(uuid.uuid4()),
        "milestoneId": str(uuid.uuid4()),
        "awardId": str(uuid.uuid4()),
        "units": -1,
    })[0] == 400)
    check("unknown draw", call("GET", f"{base('draw')}/draws/{uuid.uuid4()}")[0] == 404)
    check("unknown hold", hold_row(str(uuid.uuid4()))[0] == 404)
    check("unknown rating", call("GET", f"{base('eligibility')}/ratings/{uuid.uuid4()}")[0] == 404)
    check("unknown acknowledgement", call("GET", f"{base('acknowledgement')}/acknowledgements/{uuid.uuid4()}")[0] == 404)
    check("unknown clawback", call("GET", f"{base('clawback')}/clawbacks/{uuid.uuid4()}")[0] == 404)
    check("empty audit", call("GET", f"{base('audit')}/audits/{uuid.uuid4()}")[1] == [])
    missing = str(uuid.uuid4())
    check("missing sponsor", call("POST", f"{base('draw')}/draws", {
        "requestId": str(uuid.uuid4()),
        "sponsorId": missing,
        "programId": missing,
        "granteeId": missing,
        "milestoneId": missing,
        "awardId": missing,
        "units": 8,
    })[0] == 404)

    dup_sponsor = open_sponsor(f"DUP-S-{RUN}", "Dup")
    if dup_sponsor:
        check("duplicate sponsor", call("POST", f"{base('sponsor')}/sponsors", {
            "sponsorCode": f"DUP-S-{RUN}", "name": "Again",
        })[0] == 409)
    dup_program = open_program(f"DUP-P-{RUN}", "OPEN")
    if dup_program:
        check("duplicate program", call("POST", f"{base('program')}/programs", {
            "programCode": f"DUP-P-{RUN}", "status": "OPEN",
        })[0] == 409)
        check("missing program on milestone", call("POST", f"{base('milestone')}/milestones", {
            "milestoneCode": f"MISS-{RUN}", "programId": str(uuid.uuid4()), "status": "OPEN",
        })[0] == 404)
    dup_grantee = open_grantee(f"DUP-G-{RUN}", "Dup")
    if dup_grantee:
        check("duplicate grantee", call("POST", f"{base('grantee')}/grantees", {
            "granteeCode": f"DUP-G-{RUN}", "name": "Again",
        })[0] == 409)

    happy = desk("HPY", 5000, 125, 8)
    if happy:
        status, body, request_id = start_draw(happy)
        check("happy create", status == 201 and isinstance(body, dict) and body.get("status") == "SCORE", body)
        if status == 201 and isinstance(body, dict):
            draw_id = body["id"]
            replay = start_draw(happy, request_id=request_id)
            check("happy replay", replay[0] == 200 and isinstance(replay[1], dict) and replay[1].get("id") == draw_id, replay[1])
            paid = wait_draw(draw_id, "PAID")
            if paid:
                check("happy amount", paid.get("amountCents") == 1000 and paid.get("units") == 8 and paid.get("unitCents") == 125, paid)
                opened = award_row(happy["award"])
                hold = hold_row(draw_id)
                rating = call("GET", f"{base('eligibility')}/ratings/{draw_id}")
                decision = call("GET", f"{base('compliance')}/decisions/{draw_id}")
                voucher = call("GET", f"{base('voucher')}/vouchers/{draw_id}")
                wire = call("GET", f"{base('disbursement')}/disbursements/{draw_id}")
                check("happy ceiling", opened[0] == 200 and opened[1].get("openCents") == 4000, opened[1])
                check("happy hold", hold[0] == 200 and hold[1].get("status") == "PAID" and hold[1].get("amountCents") == 1000, hold[1])
                check("happy rating", rating[0] == 200 and rating[1].get("status") == "RATED" and rating[1].get("amountCents") == 1000, rating[1])
                check("happy decision", decision[0] == 200 and decision[1].get("status") == "ACCEPTED", decision[1])
                check("happy voucher", voucher[0] == 200 and voucher[1].get("status") == "ISSUED" and voucher[1].get("amountCents") == 1000, voucher[1])
                check("happy wire", wire[0] == 200 and wire[1].get("status") == "WIRED" and wire[1].get("amountCents") == 1000, wire[1])
                again = start_draw(happy, request_id=request_id)
                check("replay stays one draw", again[0] == 200 and again[1].get("id") == draw_id, again[1])
                ack_id = str(uuid.uuid4())
                check("happy ack", acknowledge(draw_id, ack_id)[0] == 202)
                acknowledged = wait_draw(draw_id, "ACKNOWLEDGED")
                if acknowledged:
                    issued = call("GET", f"{base('acknowledgement')}/acknowledgements/{draw_id}")
                    check("ack issued", issued[0] == 200 and issued[1].get("status") == "ISSUED", issued[1])
                    check("ack replay", acknowledge(draw_id, ack_id)[0] == 200)
                    check("second ack", acknowledge(draw_id)[0] == 409)
                    claw_id = str(uuid.uuid4())
                    check("claw after ack", clawback(draw_id, claw_id)[0] == 202)
                    check("claw replay", clawback(draw_id, claw_id)[0] == 200)
                    returned = wait_draw(draw_id, "RETURNED")
                    if returned:
                        steady, seen, live_hold, live_award = hold_returned(draw_id, happy["award"], 5000)
                        claw = call("GET", f"{base('clawback')}/clawbacks/{draw_id}")
                        check("claw stays", steady and seen == "RETURNED", {"status": seen, "hold": live_hold, "award": live_award})
                        check("claw row", claw[0] == 200 and claw[1].get("status") == "RETURNED" and claw[1].get("amountCents") == 1000, claw[1])
                        check("second claw", clawback(draw_id)[0] == 409)
                        paid_facts = fact_count(draw_id, "DrawFact", "PAID")
                        if paid_facts is not None:
                            check("one paid fact", paid_facts == 1, paid_facts)
                        still = award_row(happy["award"])
                        check("replay did not reserve again", still[0] == 200 and still[1].get("openCents") == 5000, still[1])

    bound = desk("BND", 10000, 125, 80)
    if bound:
        status, body, _request = start_draw(bound)
        check("bound create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            paid = wait_draw(body["id"], "PAID")
            if paid:
                opened = award_row(bound["award"])
                hold = hold_row(body["id"])
                check("bound amount", paid.get("amountCents") == 10000, paid)
                check("bound ceiling", opened[0] == 200 and opened[1].get("openCents") == 0, opened[1])
                check("bound hold", hold[0] == 200 and hold[1].get("status") == "PAID" and hold[1].get("amountCents") == 10000, hold[1])

    def cancelled_clean(prefix, units, unit, reason):
        world = desk(prefix, 5000, unit, units)
        if not world:
            return
        status, body, _request = start_draw(world)
        check(f"{prefix} create", status == 201 and isinstance(body, dict), body)
        if status != 201 or not isinstance(body, dict):
            return
        draw_id = body["id"]
        row = wait_draw(draw_id, "CANCELLED", reason)
        if row:
            opened = award_row(world["award"])
            check(f"{prefix} rating", call("GET", f"{base('eligibility')}/ratings/{draw_id}")[0] == 404)
            check(f"{prefix} hold", hold_row(draw_id)[0] == 404)
            check(f"{prefix} ceiling", opened[0] == 200 and opened[1].get("openCents") == 5000, opened[1])
            check(f"{prefix} ack", acknowledge(draw_id)[0] == 409)
            check(f"{prefix} claw", clawback(draw_id)[0] == 409)

    cancelled_clean("EMP", 0, 125, "EMPTY")
    cancelled_clean("OVR", 81, 125, "OVER")
    cancelled_clean("OVF", 2, MAX_INT, "GROSS")

    short = desk("SHT", 500, 125, 8)
    if short:
        status, body, _request = start_draw(short)
        check("short create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            draw_id = body["id"]
            row = wait_draw(draw_id, "CANCELLED", "SHORT")
            if row:
                rating = call("GET", f"{base('eligibility')}/ratings/{draw_id}")
                opened = award_row(short["award"])
                check("short rating", rating[0] == 200 and rating[1].get("status") == "RATED" and rating[1].get("amountCents") == 1000, rating[1])
                check("short hold", hold_row(draw_id)[0] == 404)
                check("short ceiling", opened[0] == 200 and opened[1].get("openCents") == 500, opened[1])
                check("short decision", call("GET", f"{base('compliance')}/decisions/{draw_id}")[0] == 404)

    closed = desk("SHU", 5000, 125, 8, program_status="CLOSED")
    if closed:
        status, body, _request = start_draw(closed)
        check("closed program", status == 409, body)
        check("closed hold", hold_row(body["id"])[0] == 404 if isinstance(body, dict) and body.get("id") else hold_row(str(uuid.uuid4()))[0] == 404)

    shut_milestone = desk("MLS", 5000, 125, 8, milestone_status="CLOSED")
    if shut_milestone:
        status, body, _request = start_draw(shut_milestone)
        check("closed milestone", status == 409, body)
        check("closed milestone hold", hold_row(str(uuid.uuid4()))[0] == 404)

    other = desk("OTH", 5000, 125, 8)
    if other and happy:
        status, body, _request = start_draw(
            happy,
            program_id=other["program"],
            milestone_id=other["milestone"],
        )
        check("mismatched program", status == 409, body)
        status, body, _request = start_draw(happy, sponsor_id=other["sponsor"])
        check("mismatched sponsor", status == 409, body)
        check("missing award", call("POST", f"{base('draw')}/draws", {
            "requestId": str(uuid.uuid4()),
            "sponsorId": happy["sponsor"],
            "programId": happy["program"],
            "granteeId": happy["grantee"],
            "milestoneId": happy["milestone"],
            "awardId": str(uuid.uuid4()),
            "units": 8,
        })[0] == 404)

    before = depth(DECIDE_DLQ)
    check("lapse depth", before is not None, before)
    lapsed = desk("ICE", 5000, 125, 8, grantee_code=f"G-lapse-{RUN}")
    if lapsed and before is not None:
        status, body, _request = start_draw(lapsed)
        check("lapse create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            draw_id = body["id"]

            def restored():
                row = load_draw(draw_id)
                decision = call("GET", f"{base('compliance')}/decisions/{draw_id}")
                claw = call("GET", f"{base('clawback')}/clawbacks/{draw_id}")
                hold = hold_row(draw_id)
                opened = award_row(lapsed["award"])
                if not row or decision[0] != 200 or claw[0] != 200 or hold[0] != 200 or opened[0] != 200:
                    return None
                if row.get("status") == "CANCELLED" and row.get("reason") == "LAPSE" \
                        and decision[1].get("status") == "REJECTED" \
                        and claw[1].get("status") == "RESTORED" \
                        and hold[1].get("status") == "RELEASED" \
                        and opened[1].get("openCents") == 5000:
                    return row
                return None

            row = until("lapse released", restored, 180)
            if row:
                grew(DECIDE_DLQ, before)
                rating = call("GET", f"{base('eligibility')}/ratings/{draw_id}")
                check("lapse rating", rating[0] == 200 and rating[1].get("status") == "RATED" and rating[1].get("amountCents") == 1000, rating[1])

    pair = desk("TWO", 1000, 125, 8)
    if pair:
        with ThreadPoolExecutor(max_workers=2) as pool:
            first = pool.submit(start_draw, pair)
            second = pool.submit(start_draw, pair)
            left = first.result()
            right = second.result()
        check("two creates", left[0] == 201 and right[0] == 201 and isinstance(left[1], dict) and isinstance(right[1], dict), (left[0], right[0]))
        if left[0] == 201 and right[0] == 201:
            ids = (left[1]["id"], right[1]["id"])

            def split():
                rows = [load_draw(item) for item in ids]
                if any(row is None for row in rows):
                    return None
                paid = [row for row in rows if row.get("status") == "PAID" and row.get("amountCents") == 1000]
                short_rows = [row for row in rows if row.get("status") == "CANCELLED" and row.get("reason") == "SHORT"]
                if len(paid) == 1 and len(short_rows) == 1:
                    return paid[0], short_rows[0]
                return None

            found = until("one paid draw", split, 180)
            if found:
                winner, loser = found
                live = hold_row(winner["id"])
                loser_hold = hold_row(loser["id"])
                opened = award_row(pair["award"])
                check("one hold", live[0] == 200 and live[1].get("status") == "PAID" and live[1].get("amountCents") == 1000, live[1])
                check("short skipped hold", loser_hold[0] == 404, loser_hold)
                check("one ceiling take", opened[0] == 200 and opened[1].get("openCents") == 0, opened[1])
                winner_paid = fact_count(winner["id"], "DrawFact", "PAID")
                loser_short = fact_count(loser["id"], "DrawFact", "CANCELLED")
                if winner_paid is not None:
                    check("winner paid once", winner_paid == 1, winner_paid)
                if loser_short is not None:
                    check("loser short once", loser_short == 1, loser_short)

    race = desk("RCE", 5000, 125, 8)
    if race:
        status, body, _request = start_draw(race)
        check("race create", status == 201 and isinstance(body, dict), body)
        if status == 201 and isinstance(body, dict):
            draw_id = body["id"]
            paid = wait_draw(draw_id, "PAID")
            if paid:
                with ThreadPoolExecutor(max_workers=2) as pool:
                    acked = pool.submit(acknowledge, draw_id)
                    clawed = pool.submit(clawback, draw_id)
                    ack_result = acked.result()
                    claw_result = clawed.result()
                check("race claw", claw_result[0] == 202, claw_result)
                check("race ack", ack_result[0] in (202, 409), ack_result)
                done = wait_draw(draw_id, "RETURNED")
                if done:
                    steady, seen, live_hold, live_award = hold_returned(draw_id, race["award"], 5000)
                    check("race stays returned", steady and seen == "RETURNED", {"status": seen, "hold": live_hold, "award": live_award})

    check("unknown award", award_row(str(uuid.uuid4()))[0] == 404)
    check("unknown voucher", call("GET", f"{base('voucher')}/vouchers/{uuid.uuid4()}")[0] == 404)

    if failures:
        print(f"{len(failures)} failed", flush=True)
        for failure in failures:
            print(f" - {failure}", flush=True)
        return 1
    print("grant desk smoke passed", flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
