package com.lookuphere.stockguide.history;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "trade_history")
public class TradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 종목 코드
    @Column(nullable = false, length = 10)
    private String stockCode;

    // 종목명
    @Column(nullable = false, length = 50)
    private String stockName;

    private LocalDate tradeDate;

    private String tradeType;

    private Integer price;

    private Integer quantity;

    private Integer totalAmount;

    // 누적/거래 수익률
    private Double profitRate;


    public TradeHistory() {
    }


    // =========================================================
    // Getters / Setters
    // =========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStockCode() {
        return stockCode;
    }

    public void setStockCode(String stockCode) {
        this.stockCode = stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public void setTradeDate(LocalDate tradeDate) {
        this.tradeDate = tradeDate;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Integer totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Double getProfitRate() {
        return profitRate;
    }

    public void setProfitRate(Double profitRate) {
        this.profitRate = profitRate;
    }
}





//package com.lookuphere.stockguide.history;
//import jakarta.persistence.*;
//import java.time.LocalDate;
//
//@Entity
//@Table(name = "trade_history")
//public class TradeHistory {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private LocalDate tradeDate;
//    private String tradeType;
//    private Integer price;
//    private Integer quantity;
//    private Integer totalAmount;
//
//    // 🛠️ [확장]: 타임라인 누적용 수익률 필드 추가
//    private Double profitRate;
//
//    public TradeHistory() {}
//
//    // Getters and Setters
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//    public LocalDate getTradeDate() { return tradeDate; }
//    public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
//    public String getTradeType() { return tradeType; }
//    public void setTradeType(String tradeType) { this.tradeType = tradeType; }
//    public Integer getPrice() { return price; }
//    public void setPrice(Integer price) { this.price = price; }
//    public Integer getQuantity() { return quantity; }
//    public void setQuantity(Integer quantity) { this.quantity = quantity; }
//    public Integer getTotalAmount() { return totalAmount; }
//    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
//
//    // 🛠️ 추가된 게터/세터
//    public Double getProfitRate() { return profitRate; }
//    public void setProfitRate(Double profitRate) { this.profitRate = profitRate; }
//}
