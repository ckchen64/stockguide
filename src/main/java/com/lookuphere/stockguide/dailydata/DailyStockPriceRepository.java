//package com.lookuphere.stockguide.dailydata;
//
//import org.springframework.data.jpa.repository.JpaRepository;
//import java.util.List;
//
//public interface DailyStockPriceRepository extends JpaRepository<DailyStockPrice, Long> {
//
//    // 🎯 이 메서드 선언문이 누락되어 StockService에서 에러가 났던 것입니다. 여기에 쏙 넣어주세요!
//    List<DailyStockPrice> findByStockCodeOrderByTradeDateAsc(String stockCode);
//}

package com.lookuphere.stockguide.dailydata;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyStockPriceRepository extends JpaRepository<DailyStockPrice, Long> {

    List<DailyStockPrice> findByStockCodeOrderByTradeDateAsc(String stockCode);

    // 🎯 [추가]: 최근 주가 데이터를 날짜 역순으로 페이징 조회하는 쿼리 메서드
    @Query("SELECT d FROM DailyStockPrice d WHERE d.stockCode = :stockCode ORDER BY d.tradeDate DESC")
    List<DailyStockPrice> findRecentPrices(@Param("stockCode") String stockCode, Pageable pageable);
    // 🔥 [여기만 한 줄 더 추가해 주세요!]: 조건 검색용 메서드 국한 추가
    Optional<DailyStockPrice> findByStockCodeAndTradeDate(String stockCode, LocalDate tradeDate);
}