package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 🧮 CCI (Commodity Channel Index) 계산기
 */
@Component
public class CciCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public CciCalculator(IndexConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String calculate(DailyStockPrice targetDayData,
                            List<DailyStockPrice> historicalDataCache,
                            int currentSimulationIndex,
                            Map<String, Object> resultMap) {

        // USER 설정 우선 탐색 (없을 경우 DB 공통 설정 -> 기본값 20 적용)
        String userId = null; // 추후 세션/인자에서 userId 전달받아 연결
        int period = configManager.getUserInt(userId, "CCI_PERIOD", 20);

        if (currentSimulationIndex < period - 1) {
            targetDayData.setCci(0.0);
            targetDayData.setCciSignal(0.0);
            resultMap.put("cci", 0.0);
            resultMap.put("cciSignal", 0.0);
            return "⏳ [데이터 축적] CCI 계산을 위한 과거 데이터(N일)가 부족합니다.";
        }

        // 1. N일간의 Typical Price (TP = (고가 + 저가 + 종가) / 3) 및 평균(SMA) 계산
        double[] tpArray = new double[period];
        double tpSum = 0.0;

        for (int i = 0; i < period; i++) {
            DailyStockPrice data = historicalDataCache.get(currentSimulationIndex - (period - 1) + i);
            double tp = (data.getHighPrice() + data.getLowPrice() + data.getClosePrice()) / 3.0;
            tpArray[i] = tp;
            tpSum += tp;
        }

        double tpSma = tpSum / period;
        double currentTp = tpArray[period - 1];

        // 2. Mean Deviation (평균 편차) 계산
        double devSum = 0.0;
        for (double tp : tpArray) {
            devSum += Math.abs(tp - tpSma);
        }
        double meanDeviation = devSum / period;

        // 3. CCI 계산 (Lambert 상수 0.015 적용)
        double cci = 0.0;
        if (meanDeviation != 0) {
            cci = (currentTp - tpSma) / (0.015 * meanDeviation);
        }

        double roundedCci = Math.round(cci * 100.0) / 100.0;

        // 엔티티 및 결과 맵 저장
        targetDayData.setCci(roundedCci);
        resultMap.put("cci", roundedCci);

        return String.format("📊 [CCI] 이격지수(%d일): %.2f", period, roundedCci);
    }
}









//쿠코드
//package com.lookuphere.stockguide.index;

//import com.lookuphere.stockguide.dailydata.DailyStockPrice;
//import com.lookuphere.stockguide.dailydata.IndexConfigManager; // 💡 타 패키지의 설정 매니저 임포트 완료
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//@Component
//public class CciCalculator {
//
//    private final IndexConfigManager configManager; // 🔄 동적 설정 매니저 주입
//
//    private final List<Double> tpList = new ArrayList<>();       // 전형적 가격(Typical Price) 창고
//    private final List<Double> cciHistoryList = new ArrayList<>(); // CCI 결과 역사 창고 (Signal 계산용)
//
//    private static final double CONSTANT = 0.015;
//
//    // 생성자 주입을 통해 스프링 컨테이너로부터 설정 매니저를 공급받습니다.
//    public CciCalculator(IndexConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    public String calculate(DailyStockPrice targetDayData, int currentIndex, Map<String, Object> resultMap) {
//
//        // 🎛️ DB 파라미터 실시간 제어 스위치 작동 (MySQL 스크립트에 등록한 표준 기본값 적용)
//        int period = configManager.getInt("CCI_PERIOD", 20);
//        int signalPeriod = configManager.getInt("CCI_SIGNAL", 9);
//
//        // 1. 당일 전형적 가격 (Typical Price = (고가 + 저가 + 종가) / 3) 산출 및 저장
//        double todayTP = (targetDayData.getHighPrice() + targetDayData.getLowPrice() + targetDayData.getClosePrice()) / 3.0;
//        tpList.add(todayTP);
//
//        // 2. CCI 데이터 축적 전 방어선 가드 (하드코딩 상수 대신 동적 변수 period 적용)
//        if (currentIndex < period - 1) {
//            resultMap.put("cci", 0.0);
//            resultMap.put("cciSignal", 0.0);
//            return String.format("⏳ [CCI 계측 중]... (%d/%d)", currentIndex + 1, period);
//        }
//
//        // 3. 지정된 period일간의 TP 단순 이동평균(SMA_TP) 산출
//        double tpSum = 0.0;
//        int tpSize = tpList.size();
//        for (int i = tpSize - period; i < tpSize; i++) {
//            tpSum += tpList.get(i);
//        }
//        double smaTP = tpSum / period;
//
//        // 4. 평균오차 (Mean Deviation) 산출
//        double meanDeviationSum = 0.0;
//        for (int i = tpSize - period; i < tpSize; i++) {
//            meanDeviationSum += Math.abs(tpList.get(i) - smaTP);
//        }
//        double meanDeviation = meanDeviationSum / period;
//
//        // 5. CCI 최종 수치 계산 (분모 0방지 가드 포함)
//        double cci = 0.0;
//        if (meanDeviation != 0.0) {
//            cci = (todayTP - smaTP) / (CONSTANT * meanDeviation);
//        }
//        double roundedCci = Math.round(cci * 10.0) / 10.0;
//        cciHistoryList.add(roundedCci);
//        resultMap.put("cci", roundedCci);
//
//        // 6. CCI Signal (CCI의 지정된 signalPeriod일 단순이동평균) 계산
//        double cciSignal = 0.0;
//        int cciSize = cciHistoryList.size();
//
//        if (cciSize >= signalPeriod) {
//            double cciSignalSum = 0.0;
//            for (int i = cciSize - signalPeriod; i < cciSize; i++) {
//                cciSignalSum += cciHistoryList.get(i);
//            }
//            cciSignal = cciSignalSum / signalPeriod;
//        } else {
//            // 설정된 시그널 기일이 차기 전에는 현재 CCI 수치로 방어
//            cciSignal = roundedCci;
//        }
//        double roundedCciSignal = Math.round(cciSignal * 10.0) / 10.0;
//        resultMap.put("cciSignal", roundedCciSignal);
//
//        // 7. 실시간 알파 투자 전략 리포트 멘트 반환
//        if (roundedCci >= 100.0) {
//            return String.format("🛑 [CCI 과매수] 고점경계 (CCI: %.1f | 시그널: %.1f)", roundedCci, roundedCciSignal);
//        } else if (roundedCci <= -100.0) {
//            return String.format("💎 [CCI 과매도] 저점매수 (CCI: %.1f | 시그널: %.1f)", roundedCci, roundedCciSignal);
//        } else if (roundedCci > roundedCciSignal) {
//            return String.format("📈 [CCI 골든크로스] 추세가속 (CCI: %.1f)", roundedCci);
//        } else {
//            return String.format("📉 [CCI 데드크로스] 침체장세 (CCI: %.1f)", roundedCci);
//        }
//    }
//}
