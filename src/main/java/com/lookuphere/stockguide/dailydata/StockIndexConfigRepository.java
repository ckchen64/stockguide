package com.lookuphere.stockguide.dailydata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockIndexConfigRepository extends JpaRepository<StockIndexConfig, String> {
    // findAll(), findById(), save() 등 핵심 CRUD 기능은
    // JpaRepository가 자동으로 상속해 주므로 별도의 쿼리 메서드 작성 없이 비워두셔도 작동합니다!
}