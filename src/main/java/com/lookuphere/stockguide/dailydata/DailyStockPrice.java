package com.lookuphere.stockguide.dailydata;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(
        name = "daily_stock_price",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_stock_date", columnNames = {"stockCode", "tradeDate"})
        }
)
public class DailyStockPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String stockCode;

    @Column(nullable = false, length = 50)
    private String stockName; // 💡 새로 추가된 종목명 필드

    @Column(nullable = false)
    private LocalDate tradeDate;

    private int openPrice;
    private int highPrice;
    private int lowPrice;
    private int closePrice;
    private long volume;

    // 📊 보조지표 필드
    private Double sma05;
    private Double sma20;
    private Double sma60;

    private Double rsi;
    private Double mfi;
    private Double obv;

    private Double macd;
    private Double macdSignal;
    private Double macdHist;

    private Double sigma;
    private Double adx;
    private Double diPlus;
    private Double diMinus;
    private Double cci;
    private Double cciSignal;
    private Double eom;
}









//구코드
//package com.lookuphere.stockguide.dailydata;
//
//import jakarta.persistence.*;
//import lombok.Data;
//import java.time.LocalDate;
//
//@Entity
//@Data // 🎯 Lombok의 @Data가 자동으로 모든 필드의 Getter와 Setter(setMfi 등)를 생성해 줍니다!
//@Table(name = "daily_stock_price")
//public class DailyStockPrice {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String stockCode;
//    private LocalDate tradeDate;
//    private int openPrice;
//    private int highPrice;
//    private int lowPrice;
//    private int closePrice;
//    private long volume;
//
//    // =========================================================================
//    // 📊 [지표 저장용 그릇 배치] 엔진이 연산한 결과를 테이블에 매핑하기 위한 필드들입니다.
//    // =========================================================================
//    private Double sma05;
//    private Double sma20;
//    private Double sma60;
//
//    private Double rsi;
//    private Double mfi; // 🎯 여기에 mfi 그릇이 추가되면서 target.setMfi() 에러가 해결됩니다!
//    private Double obv;
//
//    // MACD 관련
//    private Double macd;
//    private Double macdSignal;
//    private Double macdHist;
//
//    // 기타 고급 지표
//    private Double sigma;
//    private Double adx;
//    private Double diPlus;
//    private Double diMinus;
//    private Double cci;
//    private Double cciSignal;
//    private Double eom;
//}