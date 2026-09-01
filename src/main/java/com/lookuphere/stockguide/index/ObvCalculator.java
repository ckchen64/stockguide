package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 🧮 OBV (On-Balance Volume) 거래량 집계 지표 계산기
 */
@Component
public class ObvCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public ObvCalculator(IndexConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String calculate(DailyStockPrice targetDayData,
                            List<DailyStockPrice> historicalDataCache,
                            int currentSimulationIndex,
                            Map<String, Object> resultMap) {

        // USER 설정 우선 탐색 (없을 경우 DB 공통 설정 -> 기본값 0 적용)
        String userId = null; // 추후 세션/파라미터에서 사용자 ID 전달받아 연결
        int period = configManager.getUserInt(userId, "OBV_PERIOD", 0);

        if (currentSimulationIndex == 0) {
            double initialObv = (double) targetDayData.getVolume();
            targetDayData.setObv(initialObv);
            resultMap.put("obv", initialObv);
            return "⏳ [시작점] OBV 초기값(1일차 거래량) 설정 완료";
        }

        // 이전 날짜의 OBV 수치 가져오기
        DailyStockPrice previous = historicalDataCache.get(currentSimulationIndex - 1);
        double previousObv = previous.getObv() != null ? previous.getObv() : 0.0;

        int currentClose = targetDayData.getClosePrice();
        int previousClose = previous.getClosePrice();
        long currentVolume = targetDayData.getVolume();

        // 종가 비교를 통한 OBV 누적 산출
        double currentObv;
        if (currentClose > previousClose) {
            currentObv = previousObv + currentVolume;
        } else if (currentClose < previousClose) {
            currentObv = previousObv - currentVolume;
        } else {
            currentObv = previousObv;
        }

        // 특정 Period 설정 시 지정된 기간 범위 내의 OBV 변동폭으로 제한 계산
        if (period > 0 && currentSimulationIndex >= period) {
            DailyStockPrice periodStart = historicalDataCache.get(currentSimulationIndex - period);
            double baseObv = periodStart.getObv() != null ? periodStart.getObv() : 0.0;
            currentObv = currentObv - baseObv;
        }

        double roundedObv = Math.round(currentObv * 100.0) / 100.0;

        // 엔티티 및 결과 맵 저장
        targetDayData.setObv(roundedObv);
        resultMap.put("obv", roundedObv);

        return String.format("📊 [OBV] 거래량 누적 지표: %.0f", roundedObv);
    }
}









//구코드
//package com.lookuphere.stockguide.index;
//
//
//import com.lookuphere.stockguide.dailydata.DailyStockPrice;
//import com.lookuphere.stockguide.dailydata.IndexConfigManager; // 💡 타 패키지의 핵심 설정 매니저 임포트
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * 🧮 OBV(On Balance Volume) 에너지 연산 코어 및 상태 관리자 (동적 파라미터 버전)
// */
//@Component
//public class ObvCalculator {
//
//    private final IndexConfigManager configManager; // 🔄 동적 설정 매니저 주입
//
//    // 💡 개별 시뮬레이션 상태 창고 격리
//    private final List<Long> obvHistoryList = new ArrayList<>();
//    private long currentObvValue = 0L;
//
//    // 스프링 생성자 의존성 주입
//    public ObvCalculator(IndexConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    public String calculate(DailyStockPrice targetDayData, List<DailyStockPrice> historicalDataCache, int currentSimulationIndex, Map<String, Object> resultMap) {
//
//        // 🎛️ DB 파라미터 실시간 동적 매핑 (MySQL 테이블 연동, 기본값 5)
//        int period = configManager.getInt("OBV_PERIOD", 5);
//
//        // 1단계: 인덱스가 0일 때는 비교 대상(어제)이 없으므로 안전하게 거래량 반영 없이 중립 리턴
//        if (currentSimulationIndex < 1) {
//            this.obvHistoryList.add(this.currentObvValue);
//            resultMap.put("obv", this.currentObvValue); // 실시간 동기화용 데이터 주입
//            return String.format("➖ [OBV 에너지]: %,d", this.currentObvValue);
//        }
//
//        long todayPrice = targetDayData.getClosePrice();
//        long yesterdayPrice = historicalDataCache.get(currentSimulationIndex - 1).getClosePrice();
//        long todayVolume = targetDayData.getVolume();
//
//        // 2단계: 주가 등락에 따른 거래량 누적 계산
//        if (todayPrice > yesterdayPrice) {
//            this.currentObvValue += todayVolume;
//        } else if (todayPrice < yesterdayPrice) {
//            this.currentObvValue -= todayVolume;
//        }
//
//        this.obvHistoryList.add(this.currentObvValue);
//        resultMap.put("obv", this.currentObvValue); // 기존 StockService 외곽 동기화 로직 전격 통합
//
//        int obvSize = this.obvHistoryList.size();
//
//        // 3단계: 초기 데이터(지정 기간 미만) 축적기 방어선
//        if (obvSize < period) {
//            return String.format("⏳ [OBV 에너지 축적 중] (현재: %d/%d일치)", obvSize, period);
//        }
//
//        // 4단계: 최근 N(period)일간의 최고 OBV 탐색 연산 (하드코딩 5 대신 동적 period 반영)
//        long prevObvMax = this.obvHistoryList.get(obvSize - period);
//        for (int i = obvSize - period; i < obvSize - 1; i++) {
//            if (this.obvHistoryList.get(i) > prevObvMax) {
//                prevObvMax = this.obvHistoryList.get(i);
//            }
//        }
//
//        // 5단계: 수급 분석 결과에 따른 시그널 리포트 반환
//        if (this.currentObvValue > prevObvMax && todayPrice <= yesterdayPrice) {
//            return String.format("🔥 [OBV] 강력반등 징후 (OBV: %,d)", this.currentObvValue);
//        } else if (this.currentObvValue > prevObvMax) {
//            return String.format("📈 [OBV] 건강한 상승 (OBV: %,d)", this.currentObvValue);
//        } else {
//            return String.format("📉 [OBV] 거래량 정체 (OBV: %,d)", this.currentObvValue);
//        }
//    }
//}