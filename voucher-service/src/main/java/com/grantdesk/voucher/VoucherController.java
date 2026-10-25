package com.grantdesk.voucher;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class VoucherController {

    private final VoucherRepository vouchers;

    public VoucherController(VoucherRepository vouchers) {
        this.vouchers = vouchers;
    }

    @GetMapping("/vouchers/{drawId}")
    public VoucherEntity get(@PathVariable String drawId) {
        return vouchers.findById(drawId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "voucher not found"));
    }
}
