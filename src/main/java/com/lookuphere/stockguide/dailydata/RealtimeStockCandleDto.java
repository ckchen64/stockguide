package com.lookuphere.stockguide.dailydata;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class RealtimeStockCandleDto {

    private String stockCode;
    private String stockName;
    private LocalDate tradeDate;
    private int openPrice;
    private int highPrice;
    private int lowPrice;
    private int closePrice;
    private long volume;

    /**
     * DTO를 DailyStockPrice 엔티티로 변환하는 헬퍼 메서드
     */
    public DailyStockPrice toEntity() {
        DailyStockPrice entity = new DailyStockPrice();
        entity.setStockCode(this.stockCode);
        entity.setStockName(this.stockName);
        entity.setTradeDate(this.tradeDate);
        entity.setOpenPrice(this.openPrice);
        entity.setHighPrice(this.highPrice);
        entity.setLowPrice(this.lowPrice);
        entity.setClosePrice(this.closePrice);
        entity.setVolume(this.volume);
        return entity;
    }
}