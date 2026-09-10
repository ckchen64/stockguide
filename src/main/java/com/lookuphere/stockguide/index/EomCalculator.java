package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * EOM (Ease of Movement) 주가 이동 용이성 지표 계산기
 */
@Component
public class EomCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public EomCalculator(IndexConfigManager configManager) {
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
         * DB 설정값 우선 사용
         * 설정값이 없으면 기본 14일 사용
         *
         * 현재는 userId가 없으므로 공통 설정을 사용합니다.
         * 나중에 사용자별 설정 기능을 연결할 때 userId를 전달하도록 확장할 수 있습니다.
         */
        String userId = null;
        int period = configManager.getUserInt(userId, "EOM_PERIOD", 14);

        /*
         * 첫 번째 데이터는 전일 데이터가 없기 때문에
         * EOM 계산을 할 수 없습니다.
         */
        if (currentSimulationIndex <= 0
                || historicalDataCache == null
                || historicalDataCache.size() <= currentSimulationIndex) {

            targetDayData.setEom(0.0);
            resultMap.put("eom", 0.0);

            return "⏳ [데이터 축적] EOM 계산을 위한 이전 데이터가 부족합니다.";
        }

        /*
         * EOM 이동평균을 계산하려면
         * 현재 위치를 포함하여 최대 period개의 EOM 값을 계산합니다.
         */
        int startIndex = Math.max(1, currentSimulationIndex - period + 1);

        double eomSum = 0.0;
        int count = 0;

        for (int i = startIndex; i <= currentSimulationIndex; i++) {

            DailyStockPrice current = historicalDataCache.get(i);
            DailyStockPrice previous = historicalDataCache.get(i - 1);

            double high = current.getHighPrice();
            double low = current.getLowPrice();

            double previousHigh = previous.getHighPrice();
            double previousLow = previous.getLowPrice();

            double volume = current.getVolume();

            /*
             * 거래량이 0이면 정상적인 EOM 계산이 불가능하므로
             * 해당 시점은 0으로 처리합니다.
             */
            if (volume == 0) {
                eomSum += 0.0;
                count++;
                continue;
            }

            /*
             * 1. Distance Moved
             *
             * 오늘의 고가/저가 중간값과
             * 전일 고가/저가 중간값의 차이
             */
            double currentMidPoint = (high + low) / 2.0;
            double previousMidPoint = (previousHigh + previousLow) / 2.0;

            double distanceMoved = currentMidPoint - previousMidPoint;

            /*
             * 2. Box Ratio
             *
             * 기존 프로젝트에서 사용하던 계산 방식을 유지합니다.
             *
             * Box Ratio =
             * (Volume / 10000) / (High - Low)
             */
            double highLowDiff = high - low;

            /*
             * 고가와 저가가 같으면 0으로 나누는 문제가 발생하므로
             * 1로 보정합니다.
             */
            if (highLowDiff == 0) {
                highLowDiff = 1.0;
            }

            double boxRatio = (volume / 10000.0) / highLowDiff;

            /*
             * Box Ratio가 0이면 EOM을 0으로 처리합니다.
             */
            double dailyEom;

            if (boxRatio == 0) {
                dailyEom = 0.0;
            } else {
                dailyEom = distanceMoved / boxRatio;
            }

            eomSum += dailyEom;
            count++;
        }

        /*
         * 지정 기간 동안의 EOM 평균값 계산
         */
        double eom;

        if (count == 0) {
            eom = 0.0;
        } else {
            eom = eomSum / count;
        }

        /*
         * 소수점 둘째 자리까지 반올림
         */
        double roundedEom = Math.round(eom * 100.0) / 100.0;

        /*
         * 중요:
         *
         * 기존 코드에서는 resultMap에만 저장하고
         * DailyStockPrice의 eom 필드에는 넣지 않았습니다.
         *
         * 그래서 DB 저장 시 EOM 값이 반영되지 않는 문제가 있었습니다.
         */
        targetDayData.setEom(roundedEom);

        /*
         * 차트나 다른 로직에서 사용할 수 있도록
         * resultMap에도 함께 저장합니다.
         */
        resultMap.put("eom", roundedEom);

        return String.format(
                "📊 [EOM] 이동용이성 지수(%d일): %.2f",
                period,
                roundedEom
        );
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
// * 🧮 EOM(Ease of Movement) 주가 용이성 지표 계산기
// */
//@Component
//public class EomCalculator implements IndicatorCalculator {
//
//    private final IndexConfigManager configManager;
//
//    public EomCalculator(IndexConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    @Override
//    public String calculate(DailyStockPrice targetDayData,
//                            List<DailyStockPrice> historicalDataCache,
//                            int currentSimulationIndex,
//                            Map<String, Object> resultMap) {
//
//        // EOM 이평 기간 (기본값: 14)
//        int period = configManager.getInt("EOM_PERIOD", 14);
//
//        if (currentSimulationIndex < 1) {
//            resultMap.put("eom", 0.0);
//            return "⏳ [데이터 축적] EOM 계산을 위한 이전 데이터가 부족합니다.";
//        }
//
//        // 1단계: 당일 Distance Moved 및 Box Ratio 산출
//        DailyStockPrice today = targetDayData;
//        DailyStockPrice yesterday = historicalDataCache.get(currentSimulationIndex - 1);
//
//        double midToday = (today.getHighPrice() + today.getLowPrice()) / 2.0;
//        double midYesterday = (yesterday.getHighPrice() + yesterday.getLowPrice()) / 2.0;
//        double distanceMoved = midToday - midYesterday;
//
//        double highLowDiff = (today.getHighPrice() - today.getLowPrice());
//        if (highLowDiff == 0) highLowDiff = 1.0; // 0 분할 방지
//
//        // 단위 조정을 위한 스케일링 상수 (보통 10,000 ~ 1,000,000 사용)
//        double boxRatio = (today.getVolume() / 10000.0) / highLowDiff;
//        if (boxRatio == 0) boxRatio = 1.0;
//
//        double rawEom = distanceMoved / boxRatio;
//
//        // 2단계: EOM N일 단순 이동평균(SMA) 계산
//        double eomSum = rawEom;
//        int count = 1;
//
//        for (int i = currentSimulationIndex - 1; i >= Math.max(0, currentSimulationIndex - period + 1); i--) {
//            DailyStockPrice curr = historicalDataCache.get(i);
//            DailyStockPrice prev = historicalDataCache.get(i - 1);
//
//            double midC = (curr.getHighPrice() + curr.getLowPrice()) / 2.0;
//            double midP = (prev.getHighPrice() + prev.getLowPrice()) / 2.0;
//            double dm = midC - midP;
//            double hld = (curr.getHighPrice() - curr.getLowPrice());
//            if (hld == 0) hld = 1.0;
//            double br = (curr.getVolume() / 10000.0) / hld;
//            if (br == 0) br = 1.0;
//
//            eomSum += (dm / br);
//            count++;
//        }
//
//        double eomSma = eomSum / count;
//        double roundedEom = Math.round(eomSma * 100.0) / 100.0;
//
//        resultMap.put("eom", roundedEom);
//        // targetDayData.setEom(roundedEom); // DailyStockPrice 엔티티에 eom 필드 추가 시 주석 해제
//
//        return String.format("📊 [EOM] 이동용이성 수치: %.2f", roundedEom);
//    }
//}