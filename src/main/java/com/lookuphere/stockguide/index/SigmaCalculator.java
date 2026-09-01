package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 🧮 Sigma (Z-Score / 표준편차 이격 지표) 계산기
 */
@Component
public class SigmaCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public SigmaCalculator(IndexConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String calculate(DailyStockPrice targetDayData,
                            List<DailyStockPrice> historicalDataCache,
                            int currentSimulationIndex,
                            Map<String, Object> resultMap) {

        // USER 설정 우선 탐색 (없을 경우 DB 공통 설정 -> 기본값 20 적용)
        String userId = null; // 추후 세션/파라미터에서 사용자 ID 전달받아 연결
        int period = configManager.getUserInt(userId, "SIGMA_PERIOD", 20);

        if (currentSimulationIndex < period - 1) {
            targetDayData.setSigma(0.0);
            resultMap.put("sigma", 0.0);
            return "⏳ [데이터 축적] Sigma 계산을 위한 과거 데이터(N일)가 부족합니다.";
        }

        // 1. N일간의 종가 단순 이동평균(SMA) 계산
        double sum = 0.0;
        for (int i = currentSimulationIndex - period + 1; i <= currentSimulationIndex; i++) {
            sum += historicalDataCache.get(i).getClosePrice();
        }
        double mean = sum / period;

        // 2. 모표준편차(Standard Deviation) 계산
        double varianceSum = 0.0;
        for (int i = currentSimulationIndex - period + 1; i <= currentSimulationIndex; i++) {
            double diff = historicalDataCache.get(i).getClosePrice() - mean;
            varianceSum += diff * diff;
        }
        double stdDev = Math.sqrt(varianceSum / period);

        // 3. Sigma (Z-Score = (당일 종가 - 평균) / 표준편차) 계산
        double sigma = 0.0;
        if (stdDev != 0) {
            sigma = (targetDayData.getClosePrice() - mean) / stdDev;
        }

        double roundedSigma = Math.round(sigma * 100.0) / 100.0;

        // 엔티티 및 결과 맵 저장
        targetDayData.setSigma(roundedSigma);
        resultMap.put("sigma", roundedSigma);

        return String.format("📊 [Sigma] 표준편차 수치(%d일): %.2f", period, roundedSigma);
    }
}









//구코드(error 있음)
//package com.lookuphere.stockguide.index;
//
//import com.lookuphere.stockguide.dailydata.DailyStockPrice;
//import com.lookuphere.stockguide.dailydata.IndexConfigManager; // 💡 타 패키지의 핵심 설정 매니저 임포트
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Component
//public class SigmaCalculator implements IndicatorCalculator {
//
//    private final IndexConfigManager configManager; // 🔄 동적 설정 매니저 주입
//
//    // 스프링 생성자 의존성 주입
//    public SigmaCalculator(IndexConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    @Override
//    public String getName() {
//        return "SIGMA";
//    }
//
//    @Override
//    public String calculate(DailyStockPrice targetData, List<Integer> prices, Map<String, Object> resultMap) {
//
//        // 🎛️ DB 파라미터 실시간 동적 매핑 (MySQL 테이블 연동, 기본값 20)
//        int period = configManager.getInt("SIGMA_PERIOD", 20);
//
//        // 💡 설정 기간에 맞춰 이동평균선 키를 동적으로 빌드합니다 (예: 20일이면 "sma20", 14일이면 "sma14")
//        String dynamicSmaKey = "sma" + period;
//
//        // 1. 주머니(resultMap)에서 동적 키값에 맞는 SMA 추출 (실패 시 기존 sma20을 방어선으로 탐색)
//        double sma = 0.0;
//        if (resultMap.containsKey(dynamicSmaKey) && resultMap.get(dynamicSmaKey) != null) {
//            sma = ((Number) resultMap.get(dynamicSmaKey)).doubleValue();
//        } else if (resultMap.containsKey("sma20") && resultMap.get("sma20") != null) {
//            sma = ((Number) resultMap.get("sma20")).doubleValue();
//        }
//
//        // 2. 파라미터로 넘어온 정수형 주가 리스트(prices)를 연산 처리를 위해 Double 리스트로 변환
//        List<Double> closingPrices = prices.stream()
//                .map(Integer::doubleValue)
//                .collect(Collectors.toList());
//
//        // 3. 🎯 내부 static 메서드에 동적 period 파라미터를 함께 토스하여 정밀 연산 가동!
//        double sigma = calculateSigmaInternal(closingPrices, sma, period);
//
//        // 4. 프론트엔드가 즉시 낚아챌 수 있도록 결과 주머니에 "sigma" 키값으로 저장
//        resultMap.put("sigma", sigma);
//
//        // 5. 실시간 리포팅 가독성을 위한 변동성 텍스트 브리핑 분기 가공 (String.format으로 깔끔하게 정제)
//        if (sigma >= 1.0) {
//            return String.format("📈 [SIGMA 폭발] 주가가 상단 과열! (SIGMA: %.2f)", sigma);
//        } else if (sigma <= -1.0) {
//            return String.format("📉 [SIGMA 위험] 주가가 하단 추락! (SIGMA: %.2f)", sigma);
//        } else {
//            return String.format("➖ [SIGMA 안정] 통계적 범위 내 (SIGMA: %.2f)", sigma);
//        }
//    }
//
//    /**
//     * 🛠️ 내부 전용 정적 계산 메서드 (인터페이스 의존성이 없으며, period 파라미터를 동적으로 흡수)
//     */
//    private static double calculateSigmaInternal(List<Double> closingPrices, double sma, int period) {
//        if (closingPrices == null || closingPrices.size() < period || sma == 0.0) {
//            return 0.0; // 데이터가 최소 지정 기간만큼 쌓이기 전에는 0으로 방어
//        }
//
//        // 최근 N(period)일 데이터만 하위 리스트로 추출
//        List<Double> recentPrices = closingPrices.subList(closingPrices.size() - period, closingPrices.size());
//
//        // 분산(Variance) 계산: (측정값 - 평균)의 제곱의 합
//        double sumOfSquares = 0.0;
//        for (double price : recentPrices) {
//            sumOfSquares += Math.pow(price - sma, 2);
//        }
//        double variance = sumOfSquares / period;
//
//        // 표준편차(Standard Deviation) 계산
//        double standardDeviation = Math.sqrt(variance);
//
//        if (standardDeviation == 0.0) {
//            return 0.0;
//        }
//
//        // 시그마 계산: (현재 종가 - 지정이평선) / 표준편차
//        double currentClose = closingPrices.get(closingPrices.size() - 1);
//        double sigma = (currentClose - sma) / standardDeviation;
//
//        // 소수점 둘째 자리까지 깔끔하게 반올림 정제
//        return Math.round(sigma * 100.0) / 100.0;
//    }
//}