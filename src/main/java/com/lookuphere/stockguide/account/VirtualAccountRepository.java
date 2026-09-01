package com.lookuphere.stockguide.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VirtualAccountRepository extends JpaRepository<VirtualAccount, String> {
    // 종목 코드로 계좌 조회 기능 자동 상속
}