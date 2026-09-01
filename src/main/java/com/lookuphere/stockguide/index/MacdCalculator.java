package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 🧮 MACD (Moving Average Convergence Divergence) 계산기
 */
@Component
public class MacdCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public MacdCalculator(IndexConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String calculate(DailyStockPrice targetDayData,
                            List<DailyStockPrice> historicalDataCache,
                            int currentSimulationIndex,
                            Map<String, Object> resultMap) {

        // USER 설정 적용 (userId가 없거나 미설정 시 공통 DB 설정 -> 기본값 적용)
        String userId = null; // 추후 세션/인자에서 userId 전달받아 연결
        int fastPeriod = configManager.getUserInt(userId, "MACD_FAST_PERIOD", 12);
        int slowPeriod = configManager.getUserInt(userId, "MACD_SLOW_PERIOD", 26);
        int signalPeriod = configManager.getUserInt(userId, "MACD_SIGNAL_PERIOD", 9);

        if (currentSimulationIndex < slowPeriod - 1) {
            targetDayData.setMacd(0.0);
            targetDayData.setMacdSignal(0.0);
            targetDayData.setMacdHist(0.0);
            return "⏳ [데이터 축적] MACD 계산을 위한 최소 데이터(장기 이평 기간)가 부족합니다.";
        }

        // 1. 단기 EMA 및 장기 EMA 계산
        double fastEma = calculateEMA(historicalDataCache, currentSimulationIndex, fastPeriod);
        double slowEma = calculateEMA(historicalDataCache, currentSimulationIndex, slowPeriod);

        // 2. MACD Line 산출 (단기 EMA - 장기 EMA)
        double macdLine = fastEma - slowEma;
        macdLine = Math.round(macdLine * 100.0) / 100.0;

        // 3. MACD Signal Line 계산 (MACD Line의 N일 EMA)
        double signalLine = calculateMacdSignal(historicalDataCache, currentSimulationIndex, fastPeriod, slowPeriod, signalPeriod, macdLine);
        signalLine = Math.round(signalLine * 100.0) / 100.0;

        // 4. MACD Histogram 산출 (MACD Line - Signal Line)
        double macdHist = Math.round((macdLine - signalLine) * 100.0) / 100.0;

        // 엔티티에 결과 저장
        targetDayData.setMacd(macdLine);
        targetDayData.setMacdSignal(signalLine);
        targetDayData.setMacdHist(macdHist);

        resultMap.put("macd", macdLine);
        resultMap.put("macdSignal", signalLine);
        resultMap.put("macdHist", macdHist);

        return String.format("📊 [MACD] Line: %.2f | Signal: %.2f | Hist: %.2f", macdLine, signalLine, macdHist);
    }

    /**
     * 지수이동평균(EMA) 계산 헬퍼 메서드
     */
    private double calculateEMA(List<DailyStockPrice> dataList, int currentIndex, int period) {
        double multiplier = 2.0 / (period + 1);

        // 초기값: 첫 N일 단순이동평균(SMA)
        double ema = 0.0;
        int startIndex = currentIndex - period + 1;
        for (int i = startIndex; i <= currentIndex; i++) {
            ema += dataList.get(i).getClosePrice();
        }
        ema /= period;

        // 이후 데이터 지수 평활화 적용
        for (int i = startIndex + 1; i <= currentIndex; i++) {
            double close = dataList.get(i).getClosePrice();
            ema = (close - ema) * multiplier + ema;
        }

        return ema;
    }

    /**
     * MACD Signal Line (MACD 값들의 EMA) 계산 헬퍼 메서드
     */
    private double calculateMacdSignal(List<DailyStockPrice> dataList, int currentIndex, int fastPeriod, int slowPeriod, int signalPeriod, double currentMacd) {
        if (currentIndex < (slowPeriod - 1) + (signalPeriod - 1)) {
            return currentMacd; // Signal 계산에 필요한 과거 MACD 데이터 부족 시 현재 MACD 반환
        }

        double multiplier = 2.0 / (signalPeriod + 1);
        double signalEma = 0.0;

        // 과거 MACD 포인트 추적 연산
        int startIndex = currentIndex - signalPeriod + 1;
        for (int i = startIndex; i <= currentIndex; i++) {
            double fEma = calculateEMA(dataList, i, fastPeriod);
            double sEma = calculateEMA(dataList, i, slowPeriod);
            signalEma += (fEma - sEma);
        }
        signalEma /= signalPeriod;

        for (int i = startIndex + 1; i <= currentIndex; i++) {
            double fEma = calculateEMA(dataList, i, fastPeriod);
            double sEma = calculateEMA(dataList, i, slowPeriod);
            double mLine = fEma - sEma;
            signalEma = (mLine - signalEma) * multiplier + signalEma;
        }

        return signalEma;
    }
}









//구코드
//package com.lookuphere.stockguide.index;
//
////import org.springframework.stereotype.Component;
////
////import java.util.ArrayList;
////import java.util.List;
////import java.util.Map;
////
/////**
//// * 🧮 MACD 및 MACD 시그널 라인 연산 코어
//// * MACD는 단순히 현재 가격만 보는 SMA와 달리, 과거에 계산된 MACD 값들을 저장해두는 자체 창고(macdList)가 필요하고,
//// * 지수이동평균(EMA)을 구하는 수학적 보조 메서드(calculateEMA)를 함께 지니고 있어야 합니다.
//// */
////@Component
////public class MacdCalculator {
////
////    // 💡 MACD 역추적용 자체 전역 창고를 클래스 내부로 격리
////    private final List<Double> macdList = new ArrayList<>();
////
////    public String calculate(List<Integer> priceList, Map<String, Object> resultMap) {
////        int listSize = priceList.size();
////
////        // 1단계: 26일치 데이터가 쌓이기 전 방어 로직 및 resultMap 초기화
////        if (listSize < 26) {
////            resultMap.put("macd", 0);
////            resultMap.put("macdSignal", 0);
////            return "⏳ r-MACD 26일치 데이터 축적 중...";
////        }
////
////        // 2단계: EMA 12와 EMA 26을 구해 단기-장기 격차(MACD) 산출
////        double ema12 = calculateEMA(priceList, 26); // 기존 코드의 인자 순서 버그(26, 12)를 정상적인 표준 12, 26순으로 교정하여 연산 안정성을 높였습니다.
////        double ema26 = calculateEMA(priceList, 12);
////        double currentMacd = ema12 - ema26;
////        this.macdList.add(currentMacd);
////
////        // 3단계: 실시간 차트 바인딩용 "macd" 값 동기화 주입
////        resultMap.put("macd", (int) Math.round(currentMacd));
////
////        // 4단계: 시그널선(MACD의 9일 지수이동평균) 산출 및 리포트 반환
////        if (this.macdList.size() < 9) {
////            resultMap.put("macdSignal", 0);
////            return "⏳ 시그널선 확정 대기 중...";
////        }
////
////        double signal9 = calculateEMA(this.macdList, 9);
////        resultMap.put("macdSignal", (int) Math.round(signal9)); // 기존 StockService의 외곽 동기화 로직을 계산기 내부로 전격 통합
////
////        // 5단계: 입체적 리포트 멘트 생성
////        return currentMacd > signal9
////                ? String.format("📈 [r-MACD 상승] 추세 (MACD: %.1f | 시그널: %.1f)", currentMacd, signal9)
////                : String.format("📉 [r-MACD 하락] 관망 (MACD: %.1f | 시그널: %.1f)", currentMacd, signal9);
////    }
////
////    /**
////     * 지수이동평균(EMA) 공통 수학 내부 메서드
////     */
////    private double calculateEMA(List<? extends Number> dataList, int period) {
////        int size = dataList.size();
////        if (size < period) return dataList.get(size - 1).doubleValue();
////
////        double k = 2.0 / (period + 1.0);
////        double ema = dataList.get(size - period).doubleValue();
////
////        for (int i = size - period + 1; i < size; i++) {
////            ema = (dataList.get(i).doubleValue() * k) + (ema * (1.0 - k));
////        }
////        return Math.round(ema * 10.0) / 10.0;
////    }
////}
//
//import com.lookuphere.stockguide.dailydata.IndexConfigManager; // 💡 타 패키지의 설정 매니저 임포트
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
///**
// * 🧮 MACD 및 MACD 시그널 라인 연산 코어 (동적 파라미터 튜닝 버전)
// */
//@Component
//public class MacdCalculator {
//
//    private final IndexConfigManager configManager; // 🔄 동적 설정 매니저 주입
//
//    // 💡 MACD 역추적용 자체 전역 창고
//    private final List<Double> macdList = new ArrayList<>();
//
//    // 생성자 주입을 통해 컨테이너로부터 의존성을 공급받습니다.
//    public MacdCalculator(IndexConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    public String calculate(List<Integer> priceList, Map<String, Object> resultMap) {
//
//        // 🎛️ DB 파라미터 실시간 동적 스위치 매핑 (기본값: 단기 12 / 장기 26 / 시그널 9)
//        int fastPeriod = configManager.getInt("MACD_FAST", 12);
//        int slowPeriod = configManager.getInt("MACD_SLOW", 26);
//        int signalPeriod = configManager.getInt("MACD_SIGNAL", 9);
//
//        int listSize = priceList.size();
//
//        // 1단계: 장기 기준선(slowPeriod) 데이터가 쌓이기 전 방어 로직 및 resultMap 초기화
//        if (listSize < slowPeriod) {
//            resultMap.put("macd", 0);
//            resultMap.put("macdSignal", 0);
//            return String.format("⏳ r-MACD 데이터 축적 중... (%d/%d)", listSize, slowPeriod);
//        }
//
//        // 2단계: 단기 EMA와 장기 EMA를 구해 격차(MACD) 산출
//        // 🚨 [치명적 버그 교정]: 변수명과 할당 주기가 서로 엇갈려 역산되던 구조를 정상 표준 공식으로 바로잡았습니다.
//        double emaFast = calculateEMA(priceList, fastPeriod);
//        double emaSlow = calculateEMA(priceList, slowPeriod);
//        double currentMacd = emaFast - emaSlow;
//        this.macdList.add(currentMacd);
//
//        // 3단계: 실시간 차트 바인딩용 "macd" 값 동기화 주입
//        resultMap.put("macd", (int) Math.round(currentMacd));
//
//        // 4단계: 시그널선(MACD의 N일 지수이동평균) 산출 및 리포트 반환
//        if (this.macdList.size() < signalPeriod) {
//            resultMap.put("macdSignal", 0);
//            return "⏳ 시그널선 확정 대기 중...";
//        }
//
//        double signal = calculateEMA(this.macdList, signalPeriod);
//        resultMap.put("macdSignal", (int) Math.round(signal));
//
//        // 5단계: 입체적 리포트 멘트 생성
//        return currentMacd > signal
//                ? String.format("📈 [r-MACD 상승] 추세 (MACD: %.1f | 시그널: %.1f)", currentMacd, signal)
//                : String.format("📉 [r-MACD 하락] 관망 (MACD: %.1f | 시그널: %.1f)", currentMacd, signal);
//    }
//
//    /**
//     * 지수이동평균(EMA) 공통 수학 내부 메서드
//     */
//    private double calculateEMA(List<? extends Number> dataList, int period) {
//        int size = dataList.size();
//        if (size < period) return dataList.get(size - 1).doubleValue();
//
//        double k = 2.0 / (period + 1.0);
//        double ema = dataList.get(size - period).doubleValue();
//
//        for (int i = size - period + 1; i < size; i++) {
//            ema = (dataList.get(i).doubleValue() * k) + (ema * (1.0 - k));
//        }
//        return Math.round(ema * 10.0) / 10.0;
//    }
//}
//
