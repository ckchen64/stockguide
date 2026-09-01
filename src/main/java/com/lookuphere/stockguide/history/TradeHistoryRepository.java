package com.lookuphere.stockguide.history;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

//Jakarta Persistance API(application programming interface): save(),findAll(), findById(),delete()
public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {
    // 가장 최근에 거래한 내역이 맨 위로 오도록 역순(ID 내림차순) 정렬하여 가져오는 트럭입니다.
    List<TradeHistory> findAllByOrderByIdDesc();
}
