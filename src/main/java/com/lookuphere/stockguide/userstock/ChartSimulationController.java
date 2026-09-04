package com.lookuphere.stockguide.userstock;

import com.lookuphere.stockguide.dashboard.CandleChartDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChartSimulationController {

    private final SimpMessagingTemplate messagingTemplate;

    // 클라이언트에서 /app/chart/advance 로 요청을 보냈을 때 수신
    @MessageMapping("/chart/advance")
    public void handleAdvance(CandleChartDto candleData) {
        // /topic/chart/005930 경로를 구독 중인 모든 클라이언트에게 브로드캐스트
        messagingTemplate.convertAndSend("/topic/chart/" + candleData.getStockCode(), candleData);
    }

    // 서버 스케줄러나 백엔드 이벤트를 통해 직접 송신할 때 사용하는 메서드 예시
    public void sendRealtimeCandle(String stockCode, CandleChartDto candleData) {
        messagingTemplate.convertAndSend("/topic/chart/" + stockCode, candleData);
    }
}