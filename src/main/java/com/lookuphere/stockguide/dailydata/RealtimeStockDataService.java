package com.lookuphere.stockguide.dailydata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.StockIndicatorEngineService;
import com.lookuphere.stockguide.dailydata.RealtimeStockCandleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealtimeStockDataService {

    private final StockIndicatorEngineService engineService;
    private final ObjectMapper objectMapper;

    /**
     * WebSocket 메시지 수신 시 호출되는 처리 핸들러 (JSON 형태 예시)
     */
    public void processRealtimeMessage(String rawJsonMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawJsonMessage);

            // 증권사 spec에 맞게 필드명 매핑 (예시)
            String stockCode = root.path("code").asText();
            String stockName = root.path("name").asText();
            int currentPrice = root.path("price").asInt();
            int open = root.path("open").asInt();
            int high = root.path("high").asInt();
            int low = root.path("low").asInt();
            long volume = root.path("volume").asLong();

            RealtimeStockCandleDto candleDto = RealtimeStockCandleDto.builder()
                    .stockCode(stockCode)
                    .stockName(stockName)
                    .tradeDate(LocalDate.now()) // 당일 거래일
                    .openPrice(open)
                    .highPrice(high)
                    .lowPrice(low)
                    .closePrice(currentPrice)
                    .volume(volume)
                    .build();

            // 💡 실시간 수신 캔들을 엔진 서비스로 전달하여 실시간 보조지표 연산 및 DB 저장
            DailyStockPrice processed = engineService.processIncrementalCandle(stockCode, candleDto.toEntity());
            log.info("⚡ [실시간 지표 연산 완료] 종목: {}({}) | 종가: {} | RSI: {}",
                    processed.getStockName(), stockCode, processed.getClosePrice(), processed.getRsi());

        } catch (Exception e) {
            log.error("❌ 실시간 체결 데이터 파싱 실패: {}", e.getMessage(), e);
        }
    }
}