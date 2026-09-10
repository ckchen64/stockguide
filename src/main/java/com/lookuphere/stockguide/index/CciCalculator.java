package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * CCI (Commodity Channel Index) 계산기
 *
 * CCI:
 * Typical Price(TP)를 기준으로 현재 가격이
 * 최근 평균 가격에서 얼마나 떨어져 있는지를 계산합니다.
 *
 * CCI Signal:
 * 최근 N개의 CCI 값을 평균하여 계산합니다.
 */
@Component
public class CciCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public CciCalculator(IndexConfigManager configManager) {
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
         * 현재는 userId가 연결되어 있지 않기 때문에 null을 사용합니다.
         *
         * getUserInt()는
         * 사용자 설정 -> 공통 설정 -> 기본값
         * 순서로 값을 찾습니다.
         */
        String userId = null;

        /*
         * CCI 계산 기간
         * 기본값: 20일
         */
        int period = configManager.getUserInt(
                userId,
                "CCI_PERIOD",
                20
        );

        /*
         * CCI Signal 계산 기간
         * 기본값: 9일
         */
        int signalPeriod = configManager.getUserInt(
                userId,
                "CCI_SIGNAL_PERIOD",
                9
        );

        /*
         * CCI 자체를 계산하기 위한 데이터가 부족한 경우
         */
        if (currentSimulationIndex < period - 1) {

            targetDayData.setCci(0.0);
            targetDayData.setCciSignal(0.0);

            resultMap.put("cci", 0.0);
            resultMap.put("cciSignal", 0.0);

            return "⏳ [데이터 축적] CCI 계산을 위한 과거 데이터(N일)가 부족합니다.";
        }

        /*
         * ---------------------------------------------------------
         * 1. 현재 날짜의 CCI 계산
         * ---------------------------------------------------------
         */
        double currentCci = calculateCciAtIndex(
                historicalDataCache,
                currentSimulationIndex,
                period
        );

        /*
         * 소수점 둘째 자리까지 반올림
         */
        double roundedCci =
                Math.round(currentCci * 100.0) / 100.0;

        /*
         * ---------------------------------------------------------
         * 2. CCI Signal 계산
         * ---------------------------------------------------------
         *
         * CCI Signal은 최근 signalPeriod개의 CCI 평균값으로 계산합니다.
         *
         * 예:
         *
         * signalPeriod = 9
         *
         * 최근 9일의 CCI
         *
         * CCI1
         * CCI2
         * ...
         * CCI9
         *
         * 평균을 내서 CCI Signal로 사용합니다.
         */

        double cciSignal;

        /*
         * CCI Signal 계산에 필요한 과거 CCI 개수가 충분한지 확인합니다.
         *
         * CCI는 period-1 인덱스부터 계산 가능하고,
         * 그 이후 signalPeriod개의 CCI가 필요합니다.
         */
        int firstCciIndex = period - 1;

        int availableCciCount =
                currentSimulationIndex - firstCciIndex + 1;

        if (availableCciCount < signalPeriod) {

            /*
             * 아직 Signal 기간만큼 CCI가 쌓이지 않았다면
             * 현재 CCI를 Signal 값으로 사용합니다.
             *
             * 이렇게 하면 초반 값이 null이 되지 않습니다.
             */
            cciSignal = roundedCci;

        } else {

            double cciSum = 0.0;

            int signalStartIndex =
                    currentSimulationIndex - signalPeriod + 1;

            for (int i = signalStartIndex;
                 i <= currentSimulationIndex;
                 i++) {

                double pastCci = calculateCciAtIndex(
                        historicalDataCache,
                        i,
                        period
                );

                cciSum += pastCci;
            }

            cciSignal = cciSum / signalPeriod;
        }

        /*
         * 소수점 둘째 자리까지 반올림
         */
        double roundedCciSignal =
                Math.round(cciSignal * 100.0) / 100.0;

        /*
         * ---------------------------------------------------------
         * 3. DailyStockPrice Entity에 저장
         * ---------------------------------------------------------
         */

        targetDayData.setCci(roundedCci);
        targetDayData.setCciSignal(roundedCciSignal);

        /*
         * ---------------------------------------------------------
         * 4. resultMap에도 저장
         * ---------------------------------------------------------
         *
         * 차트나 다른 지표 / 이벤트 판단 로직에서 사용할 수 있습니다.
         */
        resultMap.put("cci", roundedCci);
        resultMap.put("cciSignal", roundedCciSignal);

        /*
         * 계산 결과 로그 문자열 반환
         */
        return String.format(
                "📊 [CCI] CCI(%d일): %.2f | Signal(%d일): %.2f",
                period,
                roundedCci,
                signalPeriod,
                roundedCciSignal
        );
    }


    /**
     * 특정 인덱스 시점의 CCI를 계산하는 공통 메서드
     *
     * @param historicalDataCache 전체 주가 데이터
     * @param currentIndex        CCI를 계산할 현재 위치
     * @param period              CCI 계산 기간
     * @return 계산된 CCI
     */
    private double calculateCciAtIndex(
            List<DailyStockPrice> historicalDataCache,
            int currentIndex,
            int period
    ) {

        /*
         * CCI 계산에 필요한 데이터가 부족하면
         * 안전하게 0을 반환합니다.
         */
        if (currentIndex < period - 1) {
            return 0.0;
        }

        /*
         * ---------------------------------------------------------
         * 1. 최근 N일 Typical Price 계산
         *
         * TP = (고가 + 저가 + 종가) / 3
         * ---------------------------------------------------------
         */

        double[] typicalPrices =
                new double[period];

        double tpSum = 0.0;

        int startIndex =
                currentIndex - period + 1;

        for (int i = 0; i < period; i++) {

            DailyStockPrice data =
                    historicalDataCache.get(startIndex + i);

            double typicalPrice =
                    (
                            data.getHighPrice()
                                    + data.getLowPrice()
                                    + data.getClosePrice()
                    ) / 3.0;

            typicalPrices[i] = typicalPrice;

            tpSum += typicalPrice;
        }

        /*
         * Typical Price 평균
         */
        double tpSma =
                tpSum / period;

        /*
         * 현재 날짜의 Typical Price
         */
        double currentTp =
                typicalPrices[period - 1];

        /*
         * ---------------------------------------------------------
         * 2. Mean Deviation 계산
         * ---------------------------------------------------------
         */

        double deviationSum = 0.0;

        for (double tp : typicalPrices) {

            deviationSum +=
                    Math.abs(tp - tpSma);
        }

        double meanDeviation =
                deviationSum / period;

        /*
         * ---------------------------------------------------------
         * 3. CCI 계산
         *
         * CCI =
         *
         * (현재 TP - TP 평균)
         * -----------------------------
         * 0.015 × Mean Deviation
         * ---------------------------------------------------------
         */

        if (meanDeviation == 0.0) {
            return 0.0;
        }

        return (currentTp - tpSma)
                / (0.015 * meanDeviation);
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
