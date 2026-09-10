package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 🧮 Sigma (Z-Score / 표준편차 이격 지표) 계산기
 *
 * Sigma:
 * 현재 종가가 최근 평균에서 표준편차 기준으로
 * 얼마나 떨어져 있는지를 계산합니다.
 *
 * Sigma Signal:
 * 최근 N개의 Sigma 평균값
 */
@Component
public class SigmaCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public SigmaCalculator(IndexConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String calculate(
            DailyStockPrice targetDayData,
            List<DailyStockPrice> historicalDataCache,
            int currentSimulationIndex,
            Map<String, Object> resultMap
    ) {

        /*
         * 추후 사용자별 설정 연결 예정
         */
        String userId = null;

        /*
         * Sigma 계산 기간
         * 기본값: 20일
         */
        int period = configManager.getUserInt(
                userId,
                "SIGMA_PERIOD",
                20
        );

        /*
         * Sigma Signal 계산 기간
         * 기본값: 9일
         */
        int signalPeriod = configManager.getUserInt(
                userId,
                "SIGMA_SIGNAL_PERIOD",
                9
        );

        /*
         * Sigma 계산에 필요한 데이터가 부족한 경우
         */
        if (currentSimulationIndex < period - 1) {

            targetDayData.setSigma(0.0);
            targetDayData.setSigmaSignal(0.0);

            resultMap.put("sigma", 0.0);
            resultMap.put("sigmaSignal", 0.0);

            return "⏳ [데이터 축적] Sigma 계산을 위한 과거 데이터(N일)가 부족합니다.";
        }

        /*
         * ---------------------------------------------------------
         * 1. 현재 Sigma 계산
         * ---------------------------------------------------------
         */
        double currentSigma = calculateSigmaAtIndex(
                historicalDataCache,
                currentSimulationIndex,
                period
        );

        double roundedSigma =
                Math.round(currentSigma * 100.0) / 100.0;

        /*
         * ---------------------------------------------------------
         * 2. Sigma Signal 계산
         * ---------------------------------------------------------
         *
         * 최근 signalPeriod개의 Sigma 평균값을
         * Sigma Signal로 사용합니다.
         */

        double sigmaSignal;

        /*
         * Sigma는 period - 1 위치부터 계산 가능합니다.
         */
        int firstSigmaIndex = period - 1;

        int availableSigmaCount =
                currentSimulationIndex - firstSigmaIndex + 1;

        /*
         * Signal 기간만큼 Sigma가 충분하지 않다면
         * 현재 Sigma를 Signal 값으로 사용합니다.
         */
        if (availableSigmaCount < signalPeriod) {

            sigmaSignal = roundedSigma;

        } else {

            double sigmaSum = 0.0;

            int signalStartIndex =
                    currentSimulationIndex - signalPeriod + 1;

            for (int i = signalStartIndex;
                 i <= currentSimulationIndex;
                 i++) {

                double pastSigma = calculateSigmaAtIndex(
                        historicalDataCache,
                        i,
                        period
                );

                sigmaSum += pastSigma;
            }

            sigmaSignal =
                    sigmaSum / signalPeriod;
        }

        /*
         * Sigma Signal 반올림
         */
        double roundedSigmaSignal =
                Math.round(sigmaSignal * 100.0) / 100.0;

        /*
         * ---------------------------------------------------------
         * 3. DailyStockPrice Entity 저장
         * ---------------------------------------------------------
         */
        targetDayData.setSigma(roundedSigma);
        targetDayData.setSigmaSignal(roundedSigmaSignal);

        /*
         * ---------------------------------------------------------
         * 4. resultMap 저장
         * ---------------------------------------------------------
         */
        resultMap.put("sigma", roundedSigma);
        resultMap.put("sigmaSignal", roundedSigmaSignal);

        return String.format(
                "📊 [Sigma] Sigma(%d일): %.2f | Signal(%d일): %.2f",
                period,
                roundedSigma,
                signalPeriod,
                roundedSigmaSignal
        );
    }


    /**
     * 특정 인덱스 시점의 Sigma를 계산합니다.
     *
     * Sigma Signal을 계산하려면 과거 Sigma 값도 필요하므로
     * 계산 로직을 별도 메서드로 분리했습니다.
     */
    private double calculateSigmaAtIndex(
            List<DailyStockPrice> historicalDataCache,
            int currentIndex,
            int period
    ) {

        /*
         * Sigma 계산에 필요한 데이터가 부족하면 0 반환
         */
        if (currentIndex < period - 1) {
            return 0.0;
        }

        /*
         * ---------------------------------------------------------
         * 1. 최근 N일 종가 평균 계산
         * ---------------------------------------------------------
         */

        double sum = 0.0;

        int startIndex =
                currentIndex - period + 1;

        for (int i = startIndex;
             i <= currentIndex;
             i++) {

            sum += historicalDataCache
                    .get(i)
                    .getClosePrice();
        }

        double mean =
                sum / period;

        /*
         * ---------------------------------------------------------
         * 2. 분산 계산
         * ---------------------------------------------------------
         */

        double varianceSum = 0.0;

        for (int i = startIndex;
             i <= currentIndex;
             i++) {

            double close =
                    historicalDataCache
                            .get(i)
                            .getClosePrice();

            double diff =
                    close - mean;

            varianceSum +=
                    diff * diff;
        }

        /*
         * 모표준편차 계산
         */
        double stdDev =
                Math.sqrt(
                        varianceSum / period
                );

        /*
         * 표준편차가 0이면 Sigma도 0
         */
        if (stdDev == 0.0) {
            return 0.0;
        }

        /*
         * ---------------------------------------------------------
         * 3. Sigma(Z-Score) 계산
         *
         * (현재 종가 - 평균) / 표준편차
         * ---------------------------------------------------------
         */

        double currentClose =
                historicalDataCache
                        .get(currentIndex)
                        .getClosePrice();

        return (currentClose - mean)
                / stdDev;
    }
}









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
// * 🧮 Sigma (Z-Score / 표준편차 이격 지표) 계산기
// */
//@Component
//public class SigmaCalculator implements IndicatorCalculator {
//
//    private final IndexConfigManager configManager;
//
//    public SigmaCalculator(IndexConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    @Override
//    public String calculate(DailyStockPrice targetDayData,
//                            List<DailyStockPrice> historicalDataCache,
//                            int currentSimulationIndex,
//                            Map<String, Object> resultMap) {
//
//        // USER 설정 우선 탐색 (없을 경우 DB 공통 설정 -> 기본값 20 적용)
//        String userId = null; // 추후 세션/파라미터에서 사용자 ID 전달받아 연결
//        int period = configManager.getUserInt(userId, "SIGMA_PERIOD", 20);
//
//        if (currentSimulationIndex < period - 1) {
//            targetDayData.setSigma(0.0);
//            resultMap.put("sigma", 0.0);
//            return "⏳ [데이터 축적] Sigma 계산을 위한 과거 데이터(N일)가 부족합니다.";
//        }
//
//        // 1. N일간의 종가 단순 이동평균(SMA) 계산
//        double sum = 0.0;
//        for (int i = currentSimulationIndex - period + 1; i <= currentSimulationIndex; i++) {
//            sum += historicalDataCache.get(i).getClosePrice();
//        }
//        double mean = sum / period;
//
//        // 2. 모표준편차(Standard Deviation) 계산
//        double varianceSum = 0.0;
//        for (int i = currentSimulationIndex - period + 1; i <= currentSimulationIndex; i++) {
//            double diff = historicalDataCache.get(i).getClosePrice() - mean;
//            varianceSum += diff * diff;
//        }
//        double stdDev = Math.sqrt(varianceSum / period);
//
//        // 3. Sigma (Z-Score = (당일 종가 - 평균) / 표준편차) 계산
//        double sigma = 0.0;
//        if (stdDev != 0) {
//            sigma = (targetDayData.getClosePrice() - mean) / stdDev;
//        }
//
//        double roundedSigma = Math.round(sigma * 100.0) / 100.0;
//
//        // 엔티티 및 결과 맵 저장
//        targetDayData.setSigma(roundedSigma);
//        resultMap.put("sigma", roundedSigma);
//
//        return String.format("📊 [Sigma] 표준편차 수치(%d일): %.2f", period, roundedSigma);
//    }
//}