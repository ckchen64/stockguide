//package com.lookuphere.stockguide.account;
//
//import com.lookuphere.stockguide.history.TradeHistory; // 기존 사용하시던 매매이력 엔티티 명칭 확인
//import com.lookuphere.stockguide.history.TradeHistoryRepository; // 기존 레포지토리 명칭 확인
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//public class TradeEngineService {
//
//    private final VirtualAccountRepository accountRepository;
//    private final TradeHistoryRepository tradeHistoryRepository;
//    private final StockService stockService; // 📅 가상 날짜 공유를 위해 주입
//
//    public TradeEngineService(VirtualAccountRepository accountRepository,
//                              TradeHistoryRepository tradeHistoryRepository,
//                              StockService stockService) {
//        this.accountRepository = accountRepository;
//        this.tradeHistoryRepository = tradeHistoryRepository;
//        this.stockService = stockService;
//    }
//
//    @Transactional
//    public String processOrder(String stockCode, String type, long price, long quantity) {
//        // 1. 해당 종목의 가상 계좌를 불러옵니다. 없으면 예수금 1천만 원 원본 계좌 개설
//        VirtualAccount account = accountRepository.findById(stockCode)
//                .orElseGet(() -> accountRepository.save(new VirtualAccount(stockCode)));
//
//        long totalAmount = price * quantity;
//
//        // 📅 시뮬레이터 구역[A]가 현재 달리고 있는 그 가상 날짜를 영수증 날짜로 강제 지정!
//        String currentSimDate = stockService.getCurrentSimulatedDate();
//        if (currentSimDate == null || currentSimDate.isEmpty()) {
//            currentSimDate = "시뮬레이션 가동 전";
//        }
//
//        // 🛠️ [교정 포인트] 모든 방에서 공통으로 접근할 수 있도록 변수를 위에 미리 만들어둡니다.
//        long buyAvgPrice = account.getAveragePrice();
//        long realizedProfit = 0L;
//        double profitRate = 0.0;
//        String profitSign = "";
//
//        // 🟢 [매수 프로세스: BUY]
//        if ("BUY".equalsIgnoreCase(type)) {
//            // [안전장치 1]: 가진 돈보다 더 많이 사려고 할 때 원천 컷!
//            if (account.getBalanceMoney() < totalAmount) {
//                return String.format("⚠️ [체결 거부] 가상 예수금이 부족합니다. (필요: %d원 | 보유: %d원)",
//                        totalAmount, account.getBalanceMoney());
//            }
//
//            // 회계 정산: 돈 깎고, 평단가 새로 계산하고, 주식을 더합니다.
//            long currentQty = account.getStockQuantity();
//            long currentAvgPrice = account.getAveragePrice();
//
//            long nextQty = currentQty + quantity;
//            long nextAvgPrice = ((currentQty * currentAvgPrice) + totalAmount) / nextQty;
//
//            account.setBalanceMoney(account.getBalanceMoney() - totalAmount);
//            account.setStockQuantity(nextQty);
//            account.setAveragePrice(nextAvgPrice);
//
//            // 🔵 [매도 프로세스: SELL]
//        } else if ("SELL".equalsIgnoreCase(type)) {
//            // [안전장치]: 가지고 있는 주식보다 더 많이 팔려고 할 때 원천 컷!
//            if (account.getStockQuantity() < quantity) {
//                return String.format("⚠️ [체결 거부] 보유 주식 수량이 부족합니다. (요청: %d주 | 보유: %d주)",
//                        quantity, account.getStockQuantity());
//            }
//
//            // 🧮 매수 평단가 기준 수익률 계산
//            realizedProfit = (price - buyAvgPrice) * quantity;
//            if (buyAvgPrice > 0) {
//                profitRate = ((double)(price - buyAvgPrice) / (double)buyAvgPrice) * 100.0;
//            }
//            profitSign = realizedProfit > 0 ? "+" : "";
//
//            // 회계 정산: 주식 빼고, 판 돈만큼 예수금을 입금해 줍니다.
//            account.setBalanceMoney(account.getBalanceMoney() + totalAmount);
//            account.setStockQuantity(account.getStockQuantity() - quantity);
//
//            if (account.getStockQuantity() == 0) {
//                account.setAveragePrice(0L); // 주식을 전량 매도했다면 평단가 초기화
//            }
//        } else {
//            return "❌ [오류] 올바르지 않은 매매 유형입니다.";
//        }
//
//        // 2. 가상 자산 변화 상태 저장
//        accountRepository.save(account);
//
//        // 3. 정적인 기존 trade_history 테이블에 정산 영수증 최종 발행 및 적재
//        TradeHistory history = new TradeHistory();
//        // 🛠️ 변환 장치 탑재: "2025-05-01" 글자를 자바 LocalDate 객체로 완벽히 번역해서 주입합니다!
//        if (currentSimDate != null && !currentSimDate.equals("시뮬레이션 가동 전")) {
//            history.setTradeDate(java.time.LocalDate.parse(currentSimDate));
//        } else {
//            history.setTradeDate(java.time.LocalDate.now()); // 가동 전 예외 상황 시 현재 날짜 방어 코드
//        } // ⭐ 중요: 동기화된 가상 날짜 기록
//        history.setTradeType(type.toUpperCase());
//        history.setPrice((int) price);
//        history.setQuantity((int) quantity);
//        history.setTotalAmount((int) totalAmount);
//
//        // 🛠️ [핵심 교정]: 매도(SELL)일 때만 타임라인 엔티티에 수익률을 영구 주입합니다.
//        if ("SELL".equalsIgnoreCase(type)) {
//            history.setProfitRate(profitRate);
//        } else {
//            history.setProfitRate(null); // 매수 시에는 공란 처리
//        }
//
//        tradeHistoryRepository.save(history);
//
//        // 4. [매도 전용] 실현손익과 수익률이 포함된 특제 영수증 반환
//        String reportMessage = String.format("✅ 주문 체결 완료!\n" +
//                        "------------------------\n" +
//                        "▶ 거래 날짜: %s\n" +
//                        "▶ 거래 유형: %s | 수량: %d주\n" +
//                        "▶ 체결 금액: %,d원\n",
//                currentSimDate, type.toUpperCase(), quantity, totalAmount);
//
//        if ("SELL".equalsIgnoreCase(type)) {
//            reportMessage += String.format("📊 [🔥 매도 정산 결과]\n" +
//                            "• 실현 손익: %s%,d원\n" +
//                            "• 최종 수익률: %s%.2f%%\n",
//                    profitSign, realizedProfit, profitSign, profitRate);
//        }
//
//        reportMessage += String.format("------------------------\n" +
//                        "💰 가상 계좌 현재 잔고 현황\n" +
//                        "• 남은 예수금: %,d원\n" +
//                        "• 보유 주식수: %d주\n" +
//                        "• 보유 평단가: %,d원",
//                account.getBalanceMoney(), account.getStockQuantity(), account.getAveragePrice());
//
//        return reportMessage;
//    }
//}



package com.lookuphere.stockguide.account;

import com.lookuphere.stockguide.history.TradeHistory;
import com.lookuphere.stockguide.history.TradeHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeEngineService {

    private final VirtualAccountRepository accountRepository;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final StockService stockService;

    public TradeEngineService(VirtualAccountRepository accountRepository,
                              TradeHistoryRepository tradeHistoryRepository,
                              StockService stockService) {
        this.accountRepository = accountRepository;
        this.tradeHistoryRepository = tradeHistoryRepository;
        this.stockService = stockService;
    }

    @Transactional
    public String processOrder(String stockCode, String type, long price, long quantity) {
        // 1. 해당 종목의 가상 계좌를 불러옵니다. 없으면 계좌 개설
        VirtualAccount account = accountRepository.findById(stockCode)
                .orElseGet(() -> accountRepository.save(new VirtualAccount(stockCode)));

        long totalAmount = price * quantity;

        // 📅 동기화된 가상 날짜 가져오기
        String currentSimDate = stockService.getCurrentSimulatedDate();
        if (currentSimDate == null || currentSimDate.isEmpty()) {
            currentSimDate = "시뮬레이션 가동 전";
        }

        // 영수증 출력을 위한 변수 사전 선언
        long originalAvgPrice = account.getAveragePrice(); // 변동 전 오리지널 평단가 확보
        long realizedProfit = 0L;
        double profitRate = 0.0;
        String profitSign = "";

        // 🟢 [매수 프로세스: BUY]
        if ("BUY".equalsIgnoreCase(type)) {
            if (account.getBalanceMoney() < totalAmount) {
                return String.format("⚠️ [체결 거부] 가상 예수금이 부족합니다. (필요: %,d원 | 보유: %,d원)",
                        totalAmount, account.getBalanceMoney());
            }

            long currentQty = account.getStockQuantity();
            long currentAvgPrice = account.getAveragePrice();

            long nextQty = currentQty + quantity;
            long nextAvgPrice = ((currentQty * currentAvgPrice) + totalAmount) / nextQty;

            account.setBalanceMoney(account.getBalanceMoney() - totalAmount);
            account.setStockQuantity(nextQty);
            account.setAveragePrice(nextAvgPrice);

            // 🔵 [매도 프로세스: SELL]
        } else if ("SELL".equalsIgnoreCase(type)) {
            if (account.getStockQuantity() < quantity) {
                return String.format("⚠️ [체결 거부] 보유 주식 수량이 부족합니다. (요청: %d주 | 보유: %d주)",
                        quantity, account.getStockQuantity());
            }

            // 🧮 확보해둔 오리지널 매수 평단가 기준 수익률 계산 (안전 보장)
            realizedProfit = (price - originalAvgPrice) * quantity;
            if (originalAvgPrice > 0) {
                profitRate = ((double)(price - originalAvgPrice) / (double)originalAvgPrice) * 100.0;
            }
            profitSign = realizedProfit > 0 ? "+" : "";

            account.setBalanceMoney(account.getBalanceMoney() + totalAmount);
            account.setStockQuantity(account.getStockQuantity() - quantity);

            // 주식을 전량 매도했다면 평단가 초기화
            if (account.getStockQuantity() == 0) {
                account.setAveragePrice(0L);
            }
        } else {
            return "❌ [오류] 올바르지 않은 매매 유형입니다.";
        }

        // 2. 가상 자산 변화 상태 저장
        accountRepository.save(account);

        // 3. 정산 영수증 최종 발행 및 적재
        TradeHistory history = new TradeHistory();
        if (currentSimDate != null && !currentSimDate.equals("시뮬레이션 가동 전")) {
            history.setTradeDate(java.time.LocalDate.parse(currentSimDate));
        } else {
            history.setTradeDate(java.time.LocalDate.now());
        }

        history.setTradeType(type.toUpperCase());

        // ⭐ [중요]: 오버플로우 방지를 위해 빌드 시 롱타입 그대로 수용하도록 유도
        // 만약 엔티티가 int라면 엔티티 파일의 필드 타입을 long으로 바꾸는 것을 적극 추천합니다.
        history.setPrice((int) price);
        history.setQuantity((int) quantity);
        history.setTotalAmount((int) totalAmount);

        if ("SELL".equalsIgnoreCase(type)) {
            history.setProfitRate(profitRate);
        } else {
            history.setProfitRate(null);
        }

        tradeHistoryRepository.save(history);

        // 4. 특제 영수증 텍스트 반환 (가독성을 위해 원화 표시 컴마 %,d 대거 적용)
        String reportMessage = String.format("✅ 주문 체결 완료!\n" +
                        "------------------------\n" +
                        "▶ 거래 날짜: %s\n" +
                        "▶ 거래 유형: %s | 수량: %d주\n" +
                        "▶ 체결 금액: %,d원\n",
                currentSimDate, type.toUpperCase(), quantity, totalAmount);

        if ("SELL".equalsIgnoreCase(type)) {
            reportMessage += String.format("📊 [🔥 매도 정산 결과]\n" +
                            "• 매수 평단: %,d원\n" +
                            "• 실현 손익: %s%,d원\n" +
                            "• 최종 수익률: %s%.2f%%\n",
                    originalAvgPrice, profitSign, realizedProfit, profitSign, profitRate);
        }

        reportMessage += String.format("------------------------\n" +
                        "💰 가상 계좌 현재 잔고 현황\n" +
                        "• 남은 예수금: %,d원\n" +
                        "• 보유 주식수: %d주\n" +
                        "• 보유 평단가: %,d원",
                account.getBalanceMoney(), account.getStockQuantity(), account.getAveragePrice());

        return reportMessage;
    }
}
