package com.lookuphere.stockguide.dailydata;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.StockIndicatorEngineService;
import com.lookuphere.stockguide.dailydata.RealtimeStockCandleDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock-data")
@RequiredArgsConstructor
public class StockDataRestController {

    private final StockIndicatorEngineService engineService;

    /**
     * REST API로 단일 캔들 데이터를 주입받아 보조지표 동시 연산 처리
     */
    @PostMapping("/candle")
    public ResponseEntity<DailyStockPrice> receiveRestCandle(@RequestBody RealtimeStockCandleDto candleDto) {
        DailyStockPrice calculated = engineService.processIncrementalCandle(
                candleDto.getStockCode(),
                candleDto.toEntity()
        );
        return ResponseEntity.ok(calculated);
    }
}