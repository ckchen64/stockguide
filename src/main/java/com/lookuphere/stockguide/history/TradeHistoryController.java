package com.lookuphere.stockguide.history;

import lombok.RequiredArgsConstructor; // ⚠️ [필수 추가] 롬복 어노테이션을 쓰기 위해 반드시 import 해야 합니다.
import org.springframework.web.bind.annotation.CrossOrigin;// ⚠️ [필수 추가]
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController // "나는 리액트나 브라우저와 JSON 데이터로 대화하는 웹 창구야!"라고 선언합니다.
@RequestMapping("/api") // 이 컨트롤러 방으로 들어오는 모든 주소 앞에 "/api"를 붙이겠다는 뜻입니다.
@RequiredArgsConstructor // (SPR-1-V2) 주방장(service)을 자동으로 조립해서 소환해 주는 스프링 부트의 끈입니다.
@CrossOrigin(origins = "http://localhost:5173") // ⭕ (GUI-TS-CN-1-1) 핵심: "5173 리액트 기지에서 오는 신호는 무조건 허용하라!"

public class TradeHistoryController {
    // StockController.java 내부에 추가할 코드
    private final TradeHistoryService tradeHistoryService; // 영수증 전용 서비스 소환

    @GetMapping("/history")
    public List<TradeHistory> getTradeHistoryList() {
        System.out.println("📬 [문지기 로그] 분리된 TradeHistoryService를 호출하여 타임라인 리스트를 로드합니다.");
        // ⭕ 깔끔하게 분리 독립된 지점에서 데이터를 공급받습니다.
        return tradeHistoryService.getAllHistory();
    }
}
