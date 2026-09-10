package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 🧮 MACD (Moving Average Convergence Divergence) 계산기
 *
 * 기본 MACD
 * MACD_FAST_PERIOD = 12
 * MACD_SLOW_PERIOD = 26
 *
 * 계산:
 * EMA(12) - EMA(26)
 *
 *
 * 역 MACD
 * MACD_FAST_PERIOD = 26
 * MACD_SLOW_PERIOD = 12
 *
 * 계산:
 * EMA(26) - EMA(12)
 *
 * 즉, FAST / SLOW라는 이름과 관계없이
 * 사용자가 입력한 순서대로
 *
 * EMA(FAST_PERIOD) - EMA(SLOW_PERIOD)
 *
 * 를 계산합니다.
 */
@Component
public class MacdCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public MacdCalculator(IndexConfigManager configManager) {
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
         * ---------------------------------------------------------
         * 1. 설정값 읽기
         * ---------------------------------------------------------
         */

        String userId = null;

        int fastPeriod = configManager.getUserInt(
                userId,
                "MACD_FAST_PERIOD",
                12
        );

        int slowPeriod = configManager.getUserInt(
                userId,
                "MACD_SLOW_PERIOD",
                26
        );

        int signalPeriod = configManager.getUserInt(
                userId,
                "MACD_SIGNAL_PERIOD",
                9
        );


        /*
         * ---------------------------------------------------------
         * 2. 잘못된 설정값 방어
         * ---------------------------------------------------------
         *
         * period가 0 또는 음수가 들어오면
         * EMA 계산에서 오류가 발생할 수 있습니다.
         *
         * 따라서 기본값으로 복구합니다.
         */

        if (fastPeriod <= 0) {
            fastPeriod = 12;
        }

        if (slowPeriod <= 0) {
            slowPeriod = 26;
        }

        if (signalPeriod <= 0) {
            signalPeriod = 9;
        }


        /*
         * ---------------------------------------------------------
         * 3. 필요한 최소 데이터 기간 결정
         * ---------------------------------------------------------
         *
         * 매우 중요합니다.
         *
         * 일반 MACD
         * fast = 12
         * slow = 26
         *
         * → 26일 필요
         *
         * 역 MACD
         * fast = 26
         * slow = 12
         *
         * → 역시 26일 필요
         *
         * 따라서 slowPeriod만 검사하면 안 되고
         * 둘 중 큰 값을 사용해야 합니다.
         */

        int maxPeriod = Math.max(
                fastPeriod,
                slowPeriod
        );


        /*
         * ---------------------------------------------------------
         * 4. 데이터 부족 처리
         * ---------------------------------------------------------
         */

        if (currentSimulationIndex < maxPeriod - 1) {

            targetDayData.setMacd(0.0);
            targetDayData.setMacdSignal(0.0);
            targetDayData.setMacdHist(0.0);

            resultMap.put("macd", 0.0);
            resultMap.put("macdSignal", 0.0);
            resultMap.put("macdHist", 0.0);

            return String.format(
                    "⏳ [데이터 축적] MACD 계산을 위한 최소 데이터(%d일)가 부족합니다.",
                    maxPeriod
            );
        }


        /*
         * ---------------------------------------------------------
         * 5. FAST EMA 계산
         * ---------------------------------------------------------
         */

        double fastEma = calculateEMA(
                historicalDataCache,
                currentSimulationIndex,
                fastPeriod
        );


        /*
         * ---------------------------------------------------------
         * 6. SLOW EMA 계산
         * ---------------------------------------------------------
         */

        double slowEma = calculateEMA(
                historicalDataCache,
                currentSimulationIndex,
                slowPeriod
        );


        /*
         * ---------------------------------------------------------
         * 7. MACD Line 계산
         * ---------------------------------------------------------
         *
         * 사용자가 입력한 순서를 그대로 유지합니다.
         *
         * 12, 26
         * → EMA12 - EMA26
         *
         * 26, 12
         * → EMA26 - EMA12
         *
         * 따라서 역MACD도 별도 코드를 만들 필요가 없습니다.
         */

        double macdLine =
                fastEma - slowEma;

        double roundedMacd =
                round(macdLine);


        /*
         * ---------------------------------------------------------
         * 8. MACD Signal 계산
         * ---------------------------------------------------------
         */

        double signalLine =
                calculateMacdSignal(
                        historicalDataCache,
                        currentSimulationIndex,
                        fastPeriod,
                        slowPeriod,
                        signalPeriod
                );

        double roundedSignal =
                round(signalLine);


        /*
         * ---------------------------------------------------------
         * 9. MACD Histogram
         * ---------------------------------------------------------
         */

        double macdHist =
                roundedMacd - roundedSignal;

        double roundedHist =
                round(macdHist);


        /*
         * ---------------------------------------------------------
         * 10. Entity 저장
         * ---------------------------------------------------------
         */

        targetDayData.setMacd(roundedMacd);
        targetDayData.setMacdSignal(roundedSignal);
        targetDayData.setMacdHist(roundedHist);


        /*
         * ---------------------------------------------------------
         * 11. ResultMap 저장
         * ---------------------------------------------------------
         */

        resultMap.put(
                "macd",
                roundedMacd
        );

        resultMap.put(
                "macdSignal",
                roundedSignal
        );

        resultMap.put(
                "macdHist",
                roundedHist
        );


        /*
         * ---------------------------------------------------------
         * 12. 일반 MACD / 역MACD 구분
         * ---------------------------------------------------------
         */

        String macdType;

        if (fastPeriod < slowPeriod) {

            macdType = "일반 MACD";

        } else if (fastPeriod > slowPeriod) {

            macdType = "역 MACD";

        } else {

            macdType = "동일기간 MACD";
        }


        return String.format(
                "📊 [%s] EMA(%d)-EMA(%d) | Line: %.2f | Signal(%d): %.2f | Hist: %.2f",
                macdType,
                fastPeriod,
                slowPeriod,
                roundedMacd,
                signalPeriod,
                roundedSignal,
                roundedHist
        );
    }


    /**
     * ============================================================
     * EMA 계산
     * ============================================================
     *
     * 표준적인 EMA 계산 방식입니다.
     *
     * 1. 최초 period개의 종가 → SMA
     * 2. 그 이후부터 EMA 공식 적용
     *
     * EMA =
     *
     * (현재가격 - 이전EMA) × multiplier + 이전EMA
     *
     * multiplier =
     *
     * 2 / (period + 1)
     */
    private double calculateEMA(
            List<DailyStockPrice> dataList,
            int currentIndex,
            int period
    ) {

        /*
         * 잘못된 period 방어
         */
        if (period <= 0) {
            return 0.0;
        }


        /*
         * 아직 period만큼 데이터가 없다면
         * EMA 계산 불가능
         */
        if (currentIndex < period - 1) {
            return 0.0;
        }


        /*
         * ---------------------------------------------------------
         * 1. 최초 period 데이터 SMA 계산
         * ---------------------------------------------------------
         */

        double sma = 0.0;

        for (int i = 0;
             i < period;
             i++) {

            sma += dataList
                    .get(i)
                    .getClosePrice();
        }

        sma /= period;


        /*
         * 최초 EMA
         */
        double ema = sma;


        /*
         * EMA multiplier
         */
        double multiplier =
                2.0 / (period + 1.0);


        /*
         * ---------------------------------------------------------
         * 2. 최초 period 이후부터 현재까지 EMA 계산
         * ---------------------------------------------------------
         */

        for (int i = period;
             i <= currentIndex;
             i++) {

            double close =
                    dataList
                            .get(i)
                            .getClosePrice();

            ema =
                    (close - ema)
                            * multiplier
                            + ema;
        }


        return ema;
    }


    /**
     * ============================================================
     * MACD Signal Line 계산
     * ============================================================
     *
     * MACD Signal =
     *
     * MACD 값들의 signalPeriod EMA
     */
    private double calculateMacdSignal(
            List<DailyStockPrice> dataList,
            int currentIndex,
            int fastPeriod,
            int slowPeriod,
            int signalPeriod
    ) {

        /*
         * FAST와 SLOW 중 긴 기간을 기준으로
         * 최초 MACD 계산 가능 위치 결정
         */
        int maxPeriod =
                Math.max(
                        fastPeriod,
                        slowPeriod
                );


        /*
         * 최초 MACD 계산 가능한 index
         *
         * 예:
         *
         * maxPeriod = 26
         *
         * index 25부터 계산 가능
         */
        int firstMacdIndex =
                maxPeriod - 1;


        /*
         * 현재까지 계산 가능한 MACD 개수
         */
        int availableMacdCount =
                currentIndex
                        - firstMacdIndex
                        + 1;


        /*
         * 현재 MACD
         */
        double currentMacd =
                calculateMacdAtIndex(
                        dataList,
                        currentIndex,
                        fastPeriod,
                        slowPeriod
                );


        /*
         * Signal 기간보다 MACD 데이터가 적으면
         * 현재 MACD를 Signal로 사용
         */
        if (availableMacdCount < signalPeriod) {

            return currentMacd;
        }


        /*
         * ---------------------------------------------------------
         * 1. 최초 signalPeriod개 MACD의 SMA 계산
         * ---------------------------------------------------------
         */

        double signalSum = 0.0;

        int firstSignalEndIndex =
                firstMacdIndex
                        + signalPeriod
                        - 1;


        for (int i = firstMacdIndex;
             i <= firstSignalEndIndex;
             i++) {

            double macd =
                    calculateMacdAtIndex(
                            dataList,
                            i,
                            fastPeriod,
                            slowPeriod
                    );

            signalSum += macd;
        }


        double signalEma =
                signalSum / signalPeriod;


        /*
         * ---------------------------------------------------------
         * 2. 그 이후 MACD 값을 이용해서 Signal EMA 계산
         * ---------------------------------------------------------
         */

        double multiplier =
                2.0 / (signalPeriod + 1.0);


        for (int i = firstSignalEndIndex + 1;
             i <= currentIndex;
             i++) {

            double macd =
                    calculateMacdAtIndex(
                            dataList,
                            i,
                            fastPeriod,
                            slowPeriod
                    );


            signalEma =
                    (macd - signalEma)
                            * multiplier
                            + signalEma;
        }


        return signalEma;
    }


    /**
     * ============================================================
     * 특정 날짜의 MACD 계산
     * ============================================================
     */
    private double calculateMacdAtIndex(
            List<DailyStockPrice> dataList,
            int index,
            int fastPeriod,
            int slowPeriod
    ) {

        /*
         * 두 기간 중 큰 기간만큼 데이터가 있어야 합니다.
         */
        int maxPeriod =
                Math.max(
                        fastPeriod,
                        slowPeriod
                );

        if (index < maxPeriod - 1) {
            return 0.0;
        }


        /*
         * 첫 번째 EMA
         */
        double fastEma =
                calculateEMA(
                        dataList,
                        index,
                        fastPeriod
                );


        /*
         * 두 번째 EMA
         */
        double slowEma =
                calculateEMA(
                        dataList,
                        index,
                        slowPeriod
                );


        /*
         * 입력 순서 그대로 계산
         *
         * fast=12, slow=26
         * → EMA12 - EMA26
         *
         * fast=26, slow=12
         * → EMA26 - EMA12
         */
        return fastEma - slowEma;
    }


    /**
     * 소수점 둘째 자리 반올림
     */
    private double round(double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
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
// * 🧮 MACD (Moving Average Convergence Divergence) 계산기
// */
//@Component
//public class MacdCalculator implements IndicatorCalculator {
//
//    private final IndexConfigManager configManager;
//
//    public MacdCalculator(IndexConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    @Override
//    public String calculate(DailyStockPrice targetDayData,
//                            List<DailyStockPrice> historicalDataCache,
//                            int currentSimulationIndex,
//                            Map<String, Object> resultMap) {
//
//        // USER 설정 적용 (userId가 없거나 미설정 시 공통 DB 설정 -> 기본값 적용)
//        String userId = null; // 추후 세션/인자에서 userId 전달받아 연결
//        int fastPeriod = configManager.getUserInt(userId, "MACD_FAST_PERIOD", 12);
//        int slowPeriod = configManager.getUserInt(userId, "MACD_SLOW_PERIOD", 26);
//        int signalPeriod = configManager.getUserInt(userId, "MACD_SIGNAL_PERIOD", 9);
//
//        if (currentSimulationIndex < slowPeriod - 1) {
//            targetDayData.setMacd(0.0);
//            targetDayData.setMacdSignal(0.0);
//            targetDayData.setMacdHist(0.0);
//            return "⏳ [데이터 축적] MACD 계산을 위한 최소 데이터(장기 이평 기간)가 부족합니다.";
//        }
//
//        // 1. 단기 EMA 및 장기 EMA 계산
//        double fastEma = calculateEMA(historicalDataCache, currentSimulationIndex, fastPeriod);
//        double slowEma = calculateEMA(historicalDataCache, currentSimulationIndex, slowPeriod);
//
//        // 2. MACD Line 산출 (단기 EMA - 장기 EMA)
//        double macdLine = fastEma - slowEma;
//        macdLine = Math.round(macdLine * 100.0) / 100.0;
//
//        // 3. MACD Signal Line 계산 (MACD Line의 N일 EMA)
//        double signalLine = calculateMacdSignal(historicalDataCache, currentSimulationIndex, fastPeriod, slowPeriod, signalPeriod, macdLine);
//        signalLine = Math.round(signalLine * 100.0) / 100.0;
//
//        // 4. MACD Histogram 산출 (MACD Line - Signal Line)
//        double macdHist = Math.round((macdLine - signalLine) * 100.0) / 100.0;
//
//        // 엔티티에 결과 저장
//        targetDayData.setMacd(macdLine);
//        targetDayData.setMacdSignal(signalLine);
//        targetDayData.setMacdHist(macdHist);
//
//        resultMap.put("macd", macdLine);
//        resultMap.put("macdSignal", signalLine);
//        resultMap.put("macdHist", macdHist);
//
//        return String.format("📊 [MACD] Line: %.2f | Signal: %.2f | Hist: %.2f", macdLine, signalLine, macdHist);
//    }
//
//    /**
//     * 지수이동평균(EMA) 계산 헬퍼 메서드
//     */
//    private double calculateEMA(List<DailyStockPrice> dataList, int currentIndex, int period) {
//        double multiplier = 2.0 / (period + 1);
//
//        // 초기값: 첫 N일 단순이동평균(SMA)
//        double ema = 0.0;
//        int startIndex = currentIndex - period + 1;
//        for (int i = startIndex; i <= currentIndex; i++) {
//            ema += dataList.get(i).getClosePrice();
//        }
//        ema /= period;
//
//        // 이후 데이터 지수 평활화 적용
//        for (int i = startIndex + 1; i <= currentIndex; i++) {
//            double close = dataList.get(i).getClosePrice();
//            ema = (close - ema) * multiplier + ema;
//        }
//
//        return ema;
//    }
//
//    /**
//     * MACD Signal Line (MACD 값들의 EMA) 계산 헬퍼 메서드
//     */
//    private double calculateMacdSignal(List<DailyStockPrice> dataList, int currentIndex, int fastPeriod, int slowPeriod, int signalPeriod, double currentMacd) {
//        if (currentIndex < (slowPeriod - 1) + (signalPeriod - 1)) {
//            return currentMacd; // Signal 계산에 필요한 과거 MACD 데이터 부족 시 현재 MACD 반환
//        }
//
//        double multiplier = 2.0 / (signalPeriod + 1);
//        double signalEma = 0.0;
//
//        // 과거 MACD 포인트 추적 연산
//        int startIndex = currentIndex - signalPeriod + 1;
//        for (int i = startIndex; i <= currentIndex; i++) {
//            double fEma = calculateEMA(dataList, i, fastPeriod);
//            double sEma = calculateEMA(dataList, i, slowPeriod);
//            signalEma += (fEma - sEma);
//        }
//        signalEma /= signalPeriod;
//
//        for (int i = startIndex + 1; i <= currentIndex; i++) {
//            double fEma = calculateEMA(dataList, i, fastPeriod);
//            double sEma = calculateEMA(dataList, i, slowPeriod);
//            double mLine = fEma - sEma;
//            signalEma = (mLine - signalEma) * multiplier + signalEma;
//        }
//
//        return signalEma;
//    }
//}

