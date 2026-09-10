package com.lookuphere.stockguide.dailydata;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-data")
@RequiredArgsConstructor
public class StockDataRestController {

    private final StockIndicatorEngineService engineService;


    /**
     * ============================================================
     * 1. 단일 캔들 입력
     * ============================================================
     *
     * 기존에 사용하던 API입니다.
     *
     * POST
     * /api/v1/stock-data/candle
     */
    @PostMapping("/candle")
    public ResponseEntity<DailyStockPrice> receiveRestCandle(
            @RequestBody RealtimeStockCandleDto candleDto
    ) {

        DailyStockPrice calculated =
                engineService.processIncrementalCandle(
                        candleDto.getStockCode(),
                        candleDto.toEntity()
                );

        return ResponseEntity.ok(calculated);
    }


    /**
     * ============================================================
     * 2. 여러 개의 캔들을 한 번에 입력
     * ============================================================
     *
     * 지표 계산 및 Signal 테스트를 위한 API입니다.
     *
     * 예:
     * 삼성전자 30일 데이터를 한 번에 입력
     *
     * POST
     * /api/v1/stock-data/candles
     */
    @PostMapping("/candles")
    public ResponseEntity<List<DailyStockPrice>> receiveRestCandles(
            @RequestBody List<RealtimeStockCandleDto> candleDtoList
    ) {

        /*
         * 계산이 끝난 결과를 담을 List
         */
        List<DailyStockPrice> resultList = new ArrayList<>();


        /*
         * 입력받은 데이터를
         * 앞에서부터 하나씩 순서대로 처리합니다.
         *
         * 날짜가
         *
         * 과거 → 최근
         *
         * 순서로 들어오는 것이 중요합니다.
         */
        for (RealtimeStockCandleDto candleDto : candleDtoList) {

            DailyStockPrice calculated =
                    engineService.processIncrementalCandle(
                            candleDto.getStockCode(),
                            candleDto.toEntity()
                    );

            resultList.add(calculated);
        }


        /*
         * 모든 계산 결과를 JSON 배열로 반환
         */
        return ResponseEntity.ok(resultList);
    }
}









//구코드
//package com.lookuphere.stockguide.dailydata;
//
//import com.lookuphere.stockguide.dailydata.DailyStockPrice;
//import com.lookuphere.stockguide.dailydata.StockIndicatorEngineService;
//import com.lookuphere.stockguide.dailydata.RealtimeStockCandleDto;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/stock-data")
//@RequiredArgsConstructor
//public class StockDataRestController {
//
//    private final StockIndicatorEngineService engineService;
//
//    /**
//     * REST API로 단일 캔들 데이터를 주입받아 보조지표 동시 연산 처리
//     */
//    @PostMapping("/candle")
//    public ResponseEntity<DailyStockPrice> receiveRestCandle(@RequestBody RealtimeStockCandleDto candleDto) {
//        DailyStockPrice calculated = engineService.processIncrementalCandle(
//                candleDto.getStockCode(),
//                candleDto.toEntity()
//        );
//        return ResponseEntity.ok(calculated);
//    }
//}