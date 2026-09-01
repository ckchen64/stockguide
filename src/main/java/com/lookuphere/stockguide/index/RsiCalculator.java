package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 🧮 RSI (Relative Strength Index) 상대강도지수 계산기
 */
@Component
public class RsiCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public RsiCalculator(IndexConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String calculate(DailyStockPrice targetDayData,
                            List<DailyStockPrice> historicalDataCache,
                            int currentSimulationIndex,
                            Map<String, Object> resultMap) {

        // USER 설정 우선 탐색 (없을 경우 DB 공통 설정 -> 기본값 14 적용)
        String userId = null; // 추후 세션/파라미터에서 사용자 ID 전달받아 연결
        int period = configManager.getUserInt(userId, "RSI_PERIOD", 14);

        if (currentSimulationIndex < period) {
            targetDayData.setRsi(50.0);
            resultMap.put("rsi", 50.0);
            return "⏳ [데이터 축적] RSI 계산을 위한 과거 데이터(N일)가 부족합니다.";
        }

        double totalGain = 0.0;
        double totalLoss = 0.0;

        // 최근 N일간 상승분 및 하락분의 합산
        for (int i = currentSimulationIndex - period + 1; i <= currentSimulationIndex; i++) {
            DailyStockPrice current = historicalDataCache.get(i);
            DailyStockPrice previous = historicalDataCache.get(i - 1);

            double diff = current.getClosePrice() - previous.getClosePrice();
            if (diff > 0) {
                totalGain += diff;
            } else {
                totalLoss += Math.abs(diff);
            }
        }

        double avgGain = totalGain / period;
        double avgLoss = totalLoss / period;

        // RSI 계산 (Loss가 0일 경우 100 처리)
        double rsi;
        if (avgLoss == 0) {
            rsi = 100.0;
        } else {
            double rs = avgGain / avgLoss;
            rsi = 100.0 - (100.0 / (1.0 + rs));
        }

        double roundedRsi = Math.round(rsi * 100.0) / 100.0;

        // 엔티티 및 결과 맵 저장
        targetDayData.setRsi(roundedRsi);
        resultMap.put("rsi", roundedRsi);

        return String.format("📊 [RSI] 상대강도지수(%d일): %.2f", period, roundedRsi);
    }
}









//구코드
//package com.lookuphere.stockguide.index;
//
//import com.lookuphere.stockguide.dailydata.DailyStockPrice;
//import com.lookuphere.stockguide.dailydata.IndexConfigManager;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * 🧮 RSI(상대강도지수) 연산 코어 (동적 파라미터 & Stateless 버전)
// */
//@Component
//public class RsiCalculator implements IndicatorCalculator {
//
//    private final IndexConfigManager configManager;
//
//    public RsiCalculator(IndexConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    @Override
//    public String calculate(DailyStockPrice targetDayData,
//                            List<DailyStockPrice> historicalDataCache,
//                            int currentSimulationIndex,
//                            Map<String, Object> resultMap) {
//
//        int period = configManager.getInt("RSI_PERIOD", 14);
//
//        // 1단계: 지정된 period선 완성 전 얼리 리턴
//        if (currentSimulationIndex < period) {
//            resultMap.put("rsi", 50);
//            return String.format("⏳ [심리 계측 중] RSI 데이터 축적 중입니다. (현재: %d/%d일치)", currentSimulationIndex + 1, period);
//        }
//
//        // 2단계: historicalDataCache를 기반으로 Welles Wilder 평활화 즉시 계산 (싱글톤 상태 오염 방지)
//        double prevAvgU = 0.0;
//        double prevAvgD = 0.0;
//
//        // 최초 (period)일간의 단순 평균 구하기
//        double sumU = 0.0;
//        double sumD = 0.0;
//        int firstStartIndex = currentSimulationIndex - period + 1; // 연산 필요 시점 기준
//
//        // historicalDataCache 전체 흐름 기반 순차 평활 연산
//        for (int i = 1; i <= currentSimulationIndex; i++) {
//            int diff = historicalDataCache.get(i).getClosePrice() - historicalDataCache.get(i - 1).getClosePrice();
//            double u = (diff > 0) ? diff : 0.0;
//            double d = (diff < 0) ? Math.abs(diff) : 0.0;
//
//            if (i < period) {
//                sumU += u;
//                sumD += d;
//            } else if (i == period) {
//                sumU += u;
//                sumD += d;
//                prevAvgU = sumU / (double) period;
//                prevAvgD = sumD / (double) period;
//            } else {
//                prevAvgU = ((prevAvgU * (period - 1)) + u) / (double) period;
//                prevAvgD = ((prevAvgD * (period - 1)) + d) / (double) period;
//            }
//        }
//
//        if (prevAvgU == 0.0 && prevAvgD == 0.0) {
//            resultMap.put("rsi", 50);
//            return "➖ [RSI 안정기] 계산을 유보합니다. (RSI: 50.0%)";
//        }
//
//        // 3단계: RSI 최종 수치 환산 및 결과 맵 주입
//        double rs = prevAvgU / prevAvgD;
//        double rsi = 100.0 - (100.0 / (1.0 + rs));
//        double roundedRsi = Math.round(rsi * 10.0) / 10.0;
//
//        resultMap.put("rsi", (int) Math.round(roundedRsi));
//        targetDayData.setRsi(roundedRsi); // 엔티티 동기화
//
//        // 4단계: 리포트 멘트 반환
//        if (roundedRsi >= 70.0) {
//            return String.format("🛑 [RSI과매수] 즉시 매도 (RSI: %.1f%%)", roundedRsi);
//        } else if (roundedRsi <= 30.0) {
//            return String.format("💎 [RSI과매도] 매수 타이밍 (RSI: %.1f%%)", roundedRsi);
//        } else if (roundedRsi > 50.0) {
//            return String.format("📈 [RSI우상향] 매수세 (RSI: %.1f%%)", roundedRsi);
//        } else {
//            return String.format("📉 [RSI우하향] 매도세 (RSI: %.1f%%)", roundedRsi);
//        }
//    }
//}
//
//
//
//
//
//
//
//
//
////package com.lookuphere.stockguide.index;
//////
//////import com.lookuphere.stockguide.dailydata.DailyStockPrice; // 프로젝트 패키지 구조에 맞게 임포트 확인
//////import org.springframework.stereotype.Component;
//////
//////import java.util.ArrayList;
//////import java.util.List;
//////import java.util.Map;
//////
///////**
////// * 🧮 RSI(상대강도지수) 연산 코어 및 상태 관리자
////// * RSI는 웰레스 와일더(Welles Wilder) 평활화 방식을 사용하기 때문에,
////// * 이전 날짜의 상승 평균치와 하락 평균치를 기억하는 변수(prevAvgU, prevAvgD)와
////// * 전체 흐름을 추적하는 창고(rsiHistoryList)가 필요합니다.
////// */
//////@Component
//////public class RsiCalculator {
//////
//////    // 💡 StockService에 있던 RSI 상태 저장 창고들을 내부로 격리
//////    private final List<Double> rsiHistoryList = new ArrayList<>();
//////    private double prevAvgU = 0.0;
//////    private double prevAvgD = 0.0;
//////
//////    public String calculate(DailyStockPrice targetDayData, List<DailyStockPrice> historicalDataCache, int currentSimulationIndex, Map<String, Object> resultMap) {
//////        // 1단계: 14일선이 완성되기 전(0~13 인덱스)에는 중앙인 50 복사 적재 후 얼리 리턴
//////        if (currentSimulationIndex < 14) {
//////            this.rsiHistoryList.add(50.0);
//////            resultMap.put("rsi", 50); // 실시간 동기화용 기본값 세팅
//////            return "⏳ [심리 계측 중] RSI 15일치 데이터 축적 중입니다. (현재: " + (currentSimulationIndex + 1) + "일치)";
//////        }
//////
//////        double todayU = 0.0;
//////        double todayD = 0.0;
//////
//////        int todayPrice = targetDayData.getClosePrice();
//////        int yesterdayPrice = historicalDataCache.get(currentSimulationIndex - 1).getClosePrice();
//////        int priceDiff = todayPrice - yesterdayPrice;
//////
//////        if (priceDiff > 0) todayU = priceDiff;
//////        else if (priceDiff < 0) todayD = Math.abs(priceDiff);
//////
//////        // 2단계: 최초 14일 이동평균 계산 및 웰레스 와일더 누적 연산
//////        if (this.prevAvgU == 0.0 && this.prevAvgD == 0.0) {
//////            double sumU = 0.0;
//////            double sumD = 0.0;
//////            for (int i = currentSimulationIndex - 13; i <= currentSimulationIndex; i++) {
//////                int diff = historicalDataCache.get(i).getClosePrice() - historicalDataCache.get(i - 1).getClosePrice();
//////                if (diff > 0) sumU += diff;
//////                else if (diff < 0) sumD += Math.abs(diff);
//////            }
//////            this.prevAvgU = sumU / 14.0;
//////            this.prevAvgD = sumD / 14.0;
//////        } else {
//////            this.prevAvgU = ((this.prevAvgU * 13.0) + todayU) / 14.0;
//////            this.prevAvgD = ((this.prevAvgD * 13.0) + todayD) / 14.0;
//////        }
//////
//////        if (this.prevAvgU == 0.0 && this.prevAvgD == 0.0) {
//////            this.rsiHistoryList.add(50.0);
//////            resultMap.put("rsi", 50);
//////            return "➖ [RSI 안정기] 계산을 유보합니다. (RSI: 50.0%)";
//////        }
//////
//////        // 3단계: RSI 최종 수치 환산 및 차트 데이터 맵 동기화 주입
//////        double rs = this.prevAvgU / this.prevAvgD;
//////        double rsi = 100.0 - (100.0 / (1.0 + rs));
//////        double roundedRsi = Math.round(rsi * 10.0) / 10.0;
//////
//////        this.rsiHistoryList.add(roundedRsi);
//////        resultMap.put("rsi", (int) Math.round(roundedRsi)); // 실시간 차트 값 연동 연산 통합
//////
//////        // 4단계: 입체적 투자 심리 리포트 멘트 반환
//////        if (roundedRsi >= 70.0) {
//////            return String.format("🛑 [RSI과매수] 즉시 매도 (RSI: %.1f%%)", roundedRsi);
//////        } else if (roundedRsi <= 30.0) {
//////            return String.format("💎 [RSI과매도] 매수 타이밍 (RSI: %.1f%%)", roundedRsi);
//////        } else if (roundedRsi > 50.0) {
//////            return String.format("📈 [RSI우상향] 매수세 (RSI: %.1f%%)", roundedRsi);
//////        } else {
//////            return String.format("📉 [RSI우하향] 매도세 (RSI: %.1f%%)", roundedRsi);
//////        }
//////    }
//////}
////
////
////
////
////
////
////
////import com.lookuphere.stockguide.dailydata.DailyStockPrice;
////import com.lookuphere.stockguide.dailydata.IndexConfigManager; // 💡 타 패키지의 핵심 설정 매니저 임포트
////import org.springframework.stereotype.Component;
////
////import java.util.ArrayList;
////import java.util.List;
////import java.util.Map;
////
/////**
//// * 🧮 RSI(상대강도지수) 연산 코어 및 상태 관리자 (동적 파라미터 버전)
//// */
////@Component
////public class RsiCalculator {
////
////    private final IndexConfigManager configManager; // 🔄 동적 설정 매니저 주입
////
////    // 💡 개별 계산 상태 창고 격리
////    private final List<Double> rsiHistoryList = new ArrayList<>();
////    private double prevAvgU = 0.0;
////    private double prevAvgD = 0.0;
////
////    // 스프링 생성자 의존성 주입
////    public RsiCalculator(IndexConfigManager configManager) {
////        this.configManager = configManager;
////    }
////
////    public String calculate(DailyStockPrice targetDayData, List<DailyStockPrice> historicalDataCache, int currentSimulationIndex, Map<String, Object> resultMap) {
////
////        // 🎛️ DB 파라미터 실시간 동적 매핑 (MySQL 테이블에 등록해 둔 키와 연동, 기본값 14)
////        int period = configManager.getInt("RSI_PERIOD", 14);
////
////        // 1단계: 지정된 period선이 완성되기 전에는 중앙인 50 복사 적재 후 얼리 리턴
////        if (currentSimulationIndex < period) {
////            this.rsiHistoryList.add(50.0);
////            resultMap.put("rsi", 50); // 실시간 동기화용 기본값 세팅
////            return String.format("⏳ [심리 계측 중] RSI 데이터 축적 중입니다. (현재: %d/%d일치)", currentSimulationIndex + 1, period);
////        }
////
////        double todayU = 0.0;
////        double todayD = 0.0;
////
////        int todayPrice = targetDayData.getClosePrice();
////        int yesterdayPrice = historicalDataCache.get(currentSimulationIndex - 1).getClosePrice();
////        int priceDiff = todayPrice - yesterdayPrice;
////
////        if (priceDiff > 0) todayU = priceDiff;
////        else if (priceDiff < 0) todayD = Math.abs(priceDiff);
////
////        // 2단계: 최초 지정 기간(period) 이동평균 계산 및 웰레스 와일더 누적 연산
////        if (this.prevAvgU == 0.0 && this.prevAvgD == 0.0) {
////            double sumU = 0.0;
////            double sumD = 0.0;
////
////            // 🚨 하드코딩된 13 대신 (period - 1)을 동적으로 역산하여 최초 누적 루프 구동
////            for (int i = currentSimulationIndex - (period - 1); i <= currentSimulationIndex; i++) {
////                int diff = historicalDataCache.get(i).getClosePrice() - historicalDataCache.get(i - 1).getClosePrice();
////                if (diff > 0) sumU += diff;
////                else if (diff < 0) sumD += Math.abs(diff);
////            }
////            this.prevAvgU = sumU / (double) period;
////            this.prevAvgD = sumD / (double) period;
////        } else {
////            // 🚨 지수 가중 비율 분모 분자도 고정값 대신 변수(period) 기반으로 제어
////            this.prevAvgU = ((this.prevAvgU * (period - 1)) + todayU) / period;
////            this.prevAvgD = ((this.prevAvgD * (period - 1)) + todayD) / period;
////        }
////
////        if (this.prevAvgU == 0.0 && this.prevAvgD == 0.0) {
////            this.rsiHistoryList.add(50.0);
////            resultMap.put("rsi", 50);
////            return "➖ [RSI 안정기] 계산을 유보합니다. (RSI: 50.0%)";
////        }
////
////        // 3단계: RSI 최종 수치 환산 및 차트 데이터 맵 동기화 주입
////        double rs = this.prevAvgU / this.prevAvgD;
////        double rsi = 100.0 - (100.0 / (1.0 + rs));
////        double roundedRsi = Math.round(rsi * 10.0) / 10.0;
////
////        this.rsiHistoryList.add(roundedRsi);
////        resultMap.put("rsi", (int) Math.round(roundedRsi));
////
////        // 4단계: 입체적 투자 심리 리포트 멘트 반환
////        if (roundedRsi >= 70.0) {
////            return String.format("🛑 [RSI과매수] 즉시 매도 (RSI: %.1f%%)", roundedRsi);
////        } else if (roundedRsi <= 30.0) {
////            return String.format("💎 [RSI과매도] 매수 타이밍 (RSI: %.1f%%)", roundedRsi);
////        } else if (roundedRsi > 50.0) {
////            return String.format("📈 [RSI우상향] 매수세 (RSI: %.1f%%)", roundedRsi);
////        } else {
////            return String.format("📉 [RSI우하향] 매도세 (RSI: %.1f%%)", roundedRsi);
////        }
////    }
////}