package com.grantdesk.voucher;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<VoucherEntity, String> {

}
