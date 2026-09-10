package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 🧮 MFI (Money Flow Index) 자금흐름지표 계산기
 *
 * MFI:
 * 가격과 거래량을 이용하여 자금 유입/유출 강도를 계산
 *
 * MFI Signal:
 * 최근 N개의 MFI 평균값
 */
@Component
public class MfiCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public MfiCalculator(IndexConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String calculate(
            DailyStockPrice targetDayData,
            List<DailyStockPrice> historicalDataCache,
            int currentSimulationIndex,
            Map<String, Object> resultMap
    ) {

        // 추후 사용자별 설정을 연결할 예정
        String userId = null;

        // MFI 계산 기간
        int period = configManager.getUserInt(
                userId,
                "MFI_PERIOD",
                14
        );

        // MFI Signal 계산 기간
        int signalPeriod = configManager.getUserInt(
                userId,
                "MFI_SIGNAL_PERIOD",
                9
        );

        /*
         * MFI 계산에는 전일 데이터 비교가 필요하므로
         * 최소 period일 이상의 과거 데이터가 필요합니다.
         */
        if (currentSimulationIndex < period) {

            targetDayData.setMfi(50.0);
            targetDayData.setMfiSignal(50.0);

            resultMap.put("mfi", 50.0);
            resultMap.put("mfiSignal", 50.0);

            return "⏳ [데이터 축적] MFI 계산을 위한 과거 데이터(N일)가 부족합니다.";
        }

        /*
         * ---------------------------------------------------------
         * 1. 현재 MFI 계산
         * ---------------------------------------------------------
         */
        double currentMfi = calculateMfiAtIndex(
                historicalDataCache,
                currentSimulationIndex,
                period
        );

        double roundedMfi =
                Math.round(currentMfi * 100.0) / 100.0;

        /*
         * ---------------------------------------------------------
         * 2. MFI Signal 계산
         * ---------------------------------------------------------
         *
         * 최근 signalPeriod개의 MFI 평균값을 Signal로 사용합니다.
         */

        double mfiSignal;

        /*
         * 최초 MFI 계산 가능 위치는 period 입니다.
         */
        int firstMfiIndex = period;

        int availableMfiCount =
                currentSimulationIndex - firstMfiIndex + 1;

        /*
         * 아직 9개 등의 Signal 기간이 충분히 쌓이지 않았으면
         * 현재 MFI를 Signal 값으로 사용합니다.
         */
        if (availableMfiCount < signalPeriod) {

            mfiSignal = roundedMfi;

        } else {

            double mfiSum = 0.0;

            int signalStartIndex =
                    currentSimulationIndex - signalPeriod + 1;

            for (int i = signalStartIndex;
                 i <= currentSimulationIndex;
                 i++) {

                double pastMfi = calculateMfiAtIndex(
                        historicalDataCache,
                        i,
                        period
                );

                mfiSum += pastMfi;
            }

            mfiSignal =
                    mfiSum / signalPeriod;
        }

        double roundedMfiSignal =
                Math.round(mfiSignal * 100.0) / 100.0;

        /*
         * ---------------------------------------------------------
         * 3. DailyStockPrice Entity 저장
         * ---------------------------------------------------------
         */

        targetDayData.setMfi(roundedMfi);
        targetDayData.setMfiSignal(roundedMfiSignal);

        /*
         * ---------------------------------------------------------
         * 4. resultMap 저장
         * ---------------------------------------------------------
         */

        resultMap.put("mfi", roundedMfi);
        resultMap.put("mfiSignal", roundedMfiSignal);

        return String.format(
                "📊 [MFI] MFI(%d일): %.2f | Signal(%d일): %.2f",
                period,
                roundedMfi,
                signalPeriod,
                roundedMfiSignal
        );
    }


    /**
     * 특정 인덱스 시점의 MFI를 계산합니다.
     *
     * Signal 계산 시 과거 MFI도 필요하기 때문에
     * MFI 계산 로직을 별도 메서드로 분리했습니다.
     */
    private double calculateMfiAtIndex(
            List<DailyStockPrice> historicalDataCache,
            int currentIndex,
            int period
    ) {

        /*
         * MFI는 이전 날짜와 비교해야 하므로
         * 최소 period 위치 이후에 계산 가능합니다.
         */
        if (currentIndex < period) {
            return 50.0;
        }

        double positiveMoneyFlow = 0.0;
        double negativeMoneyFlow = 0.0;

        /*
         * 최근 N일 동안의
         * Positive / Negative Money Flow 계산
         */
        for (int i = currentIndex - period + 1;
             i <= currentIndex;
             i++) {

            DailyStockPrice current =
                    historicalDataCache.get(i);

            DailyStockPrice previous =
                    historicalDataCache.get(i - 1);

            /*
             * Typical Price
             *
             * (고가 + 저가 + 종가) / 3
             */
            double currentTp =
                    (
                            current.getHighPrice()
                                    + current.getLowPrice()
                                    + current.getClosePrice()
                    ) / 3.0;

            double previousTp =
                    (
                            previous.getHighPrice()
                                    + previous.getLowPrice()
                                    + previous.getClosePrice()
                    ) / 3.0;

            /*
             * Raw Money Flow
             *
             * Typical Price × Volume
             */
            double rawMoneyFlow =
                    currentTp * current.getVolume();

            if (currentTp > previousTp) {

                positiveMoneyFlow += rawMoneyFlow;

            } else if (currentTp < previousTp) {

                negativeMoneyFlow += rawMoneyFlow;
            }
        }

        /*
         * Negative Money Flow가 0이면
         * MFI는 100으로 처리합니다.
         */
        if (negativeMoneyFlow == 0.0) {
            return 100.0;
        }

        double moneyRatio =
                positiveMoneyFlow / negativeMoneyFlow;

        return 100.0
                - (100.0 / (1.0 + moneyRatio));
    }
}









//구코드
//package com.lookuphere.stockguide.index;
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
// * 🧮 Money Flow Index (MFI) 연산 코어 및 상태 관리자 (동적 파라미터 버전)
// */
//@Component
//public class MfiCalculator {
//
//    private final IndexConfigManager configManager; // 🔄 동적 설정 매니저 주입
//
//    // 💡 개별 계산 상태 창고 격리
//    private final List<Double> mfiHistoryList = new ArrayList<>();
//    private double prevAvgPosMF = 0.0;
//    private double prevAvgNegMF = 0.0;
//
//    // 스프링 생성자 의존성 주입
//    public MfiCalculator(IndexConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    public String calculate(DailyStockPrice targetDayData, List<DailyStockPrice> historicalDataCache, int currentSimulationIndex, Map<String, Object> resultMap) {
//
//        // 🎛️ DB 파라미터 실시간 동적 매핑 (MySQL 테이블에 등록해 둔 키와 연동, 기본값 14)
//        int period = configManager.getInt("MFI_PERIOD", 14);
//
//        // 1단계: MFI 지정된 변수 기간치 데이터가 축적되기 전에는 중앙값 50 적재 후 얼리 리턴
//        if (currentSimulationIndex < period) {
//            this.mfiHistoryList.add(50.0);
//            resultMap.put("mfi", 50);
//            return String.format("⏳ [MFI 계산중] 데이터 축적 중. (현재: %d/%d일치)", currentSimulationIndex + 1, period);
//        }
//
//        // 2단계: 당일 전형적 가격 (Typical Price = (고가 + 저가 + 종가) / 3) 및 자금 흐름 산출
//        double todayTP = (targetDayData.getHighPrice() + targetDayData.getLowPrice() + targetDayData.getClosePrice()) / 3.0;
//        double todayMF = todayTP * targetDayData.getVolume();
//
//        // 전일 전형적 가격 산출
//        DailyStockPrice yesterdayData = historicalDataCache.get(currentSimulationIndex - 1);
//        double yesterdayTP = (yesterdayData.getHighPrice() + yesterdayData.getLowPrice() + yesterdayData.getClosePrice()) / 3.0;
//
//        double todayPosMF = 0.0;
//        double todayNegMF = 0.0;
//
//        if (todayTP > yesterdayTP) {
//            todayPosMF = todayMF; // 가격 상승 시 양의 자금 흐름
//        } else if (todayTP < yesterdayTP) {
//            todayNegMF = todayMF; // 가격 하락 시 음의 자금 흐름
//        }
//
//        // 3단계: 최초 지정 기간(period) 누적 및 웰레스 와일더(Welles Wilder) 방식 평활화 연산
//        if (this.prevAvgPosMF == 0.0 && this.prevAvgNegMF == 0.0) {
//            double sumPos = 0.0;
//            double sumNeg = 0.0;
//
//            // 🚨 하드코딩된 13 대신 (period - 1)을 동적으로 역산하여 루프 구동
//            for (int i = currentSimulationIndex - (period - 1); i <= currentSimulationIndex; i++) {
//                DailyStockPrice currentItem = historicalDataCache.get(i);
//                DailyStockPrice prevItem = historicalDataCache.get(i - 1);
//
//                double currTP = (currentItem.getHighPrice() + currentItem.getLowPrice() + currentItem.getClosePrice()) / 3.0;
//                double prevTP = (prevItem.getHighPrice() + prevItem.getLowPrice() + prevItem.getClosePrice()) / 3.0;
//                double itemMF = currTP * currentItem.getVolume();
//
//                if (currTP > prevTP) sumPos += itemMF;
//                else if (currTP < prevTP) sumNeg += itemMF;
//            }
//            this.prevAvgPosMF = sumPos / (double) period;
//            this.prevAvgNegMF = sumNeg / (double) period;
//        } else {
//            // 🚨 평활화 가중치 분모 분자 공식도 변수(period) 기반으로 전격 전환
//            this.prevAvgPosMF = ((this.prevAvgPosMF * (period - 1)) + todayPosMF) / period;
//            this.prevAvgNegMF = ((this.prevAvgNegMF * (period - 1)) + todayNegMF) / period;
//        }
//
//        // 4단계: 자금 흐름 정체기 가드 코드
//        if (this.prevAvgPosMF == 0.0 && this.prevAvgNegMF == 0.0) {
//            this.mfiHistoryList.add(50.0);
//            resultMap.put("mfi", 50);
//            return "➖ [MFI정체] 계산 유보. (MFI: 50.0%)";
//        }
//
//        // 5단계: MFI 최종 백분율 수치 환산 및 차트 데이터 맵 동기화 주입
//        double mr = this.prevAvgPosMF / (this.prevAvgNegMF == 0.0 ? 1.0 : this.prevAvgNegMF); // 분모 0 방지
//        double mfi = 100.0 - (100.0 / (1.0 + mr));
//        double roundedMfi = Math.round(mfi * 10.0) / 10.0;
//
//        this.mfiHistoryList.add(roundedMfi);
//        resultMap.put("mfi", (int) Math.round(roundedMfi));
//
//        // 6단계: 거래량 기반 투자 심리 리포트 멘트 빌드
//        if (roundedMfi >= 80.0) {
//            return String.format("🛑 [MFI 과열] 매도 준비 (MFI: %.1f%%)", roundedMfi);
//        } else if (roundedMfi <= 20.0) {
//            return String.format("💎 [MFI 매집] 반등 포착 (MFI: %.1f%%)", roundedMfi);
//        } else if (roundedMfi > 50.0) {
//            return String.format("📈 [MFI유입] 매수세 증가 (MFI: %.1f%%)", roundedMfi);
//        } else {
//            return String.format("📉 [MFI유출] 매도세 점증 (MFI: %.1f%%)", roundedMfi);
//        }
//    }
//}