package com.lookuphere.stockguide.history; // ⚠️ 보내주신 그림의 패키지 경로 일치 확인

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeHistoryService {

    private final TradeHistoryRepository tradeHistoryRepository;

    // 🧾 [기능 1]: 영수증 새로 발행해서 금고에 넣기
    @Transactional
    public void logTradeHistory(LocalDate simulatedDate, String type, int price, int quantity) {
        TradeHistory history = new TradeHistory();
        history.setTradeDate(simulatedDate);
        history.setTradeType(type);
        history.setPrice(price);
        history.setQuantity(quantity);
        history.setTotalAmount(price * quantity);

        tradeHistoryRepository.save(history);
        System.out.println("🧾 [영수증 서비스] 가상 날짜 " + simulatedDate + "의 매매 이력이 안전하게 영구 기록되었습니다.");
    }

    // 📜 [기능 2]: 전체 매매 이력 최근 거래순으로 조회하기
    public List<TradeHistory> getAllHistory() {
        return tradeHistoryRepository.findAllByOrderByIdDesc();
    }
}