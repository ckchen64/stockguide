package com.lookuphere.stockguide.account;

import com.lookuphere.stockguide.dashboard.DashboardDto;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")// 🔓 리액트 브라우저의 접근 통행증 발급
public class TradeEngineController {
    // 🛠️ [체크 포인트 ①]: 주문 처리를 수행할 진짜 서비스 엔진 변수가 누락 없이 존재해야 합니다.
    private final TradeEngineService tradeEngineService;
    private final VirtualAccountRepository accountRepository;
    private final StockService stockService;

    // 🛠️ [체크 포인트 ②]: 생성자 매개변수의 명칭과 개수가 위 변수들과 1:1로 정확히 도킹되어야 합니다.
    public TradeEngineController(TradeEngineService tradeEngineService,
                                 VirtualAccountRepository accountRepository,
                                 StockService stockService) {
        this.tradeEngineService = tradeEngineService;
        this.accountRepository = accountRepository;
        this.stockService = stockService;
    }

    /**
     * 🚪 [구역 B 주문 패널 도킹 API]
     * 리액트의 axios.post('http://localhost:9100/api/trade') 요청을 정면 수신합니다.
     * * URL 파라미터 방식과 Form 데이터 방식을 모두 포용할 수 있도록 규격을 느슨하고 안전하게 확장합니다.
     */
    @PostMapping("/api/trade")
    public String executeTrade(
            @RequestParam(value = "type", required = false, defaultValue = "BUY") String type,
            @RequestParam(value = "price", required = false, defaultValue = "0") long price,
            @RequestParam(value = "quantity", required = false, defaultValue = "0") long quantity) {

        String stockCode = "005930"; // 기본 가상 종목 고정
        // 브라우저 검사용 디버깅 로그 출력 (인텔리제이 콘솔창에서 확인 가능)
        System.out.println("📥 [주문 수신 성공] 유형: " + type + " | 단가: " + price + " | 수량: " + quantity);
        // 예외 방어: 프론트에서 값이 누락되어 0으로 넘어왔을 때의 안전 장치
        if (price == 0 || quantity == 0) {
            return "⚠️ [체결 거부] 주문 단가 또는 수량이 0원/0주 입니다. 화면 입력값을 확인하세요.";
        }
        // 🛠️ [체크 포인트 ③]: 주입받은 tradeEngineService를 깨워 회계 처리를 넘깁니다.
        return tradeEngineService.processOrder(stockCode, type, price, quantity);
    }

    /**
     * 실시간 자산 평가 전광판 라우터 관제탑 API
     * 리액트의 axios.get('http://localhost:9100/api/dashboard') 요청을 실시간 수신합니다.
     */
    @GetMapping("/api/dashboard")
    public DashboardDto getLiveDashboard() {
        String stockCode = "005930";
        VirtualAccount account = accountRepository.findById(stockCode)
                .orElseGet(() -> new VirtualAccount(stockCode));

        // StockService에서 현재 전진한 실시간 당일 마감 주가를 수집
        long liveCurrentPrice = stockService.getCurrentClosePrice();

        return new DashboardDto(account.getBalanceMoney(), account.getStockQuantity(), account.getAveragePrice(), liveCurrentPrice);
    }
}
