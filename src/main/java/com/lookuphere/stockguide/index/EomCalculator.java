package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EomCalculator {

    public String calculate(DailyStockPrice targetDayData, List<DailyStockPrice> historicalDataCache, int currentIndex, Map<String, Object> resultMap) {
        // 🎯 1. 0일차(첫 번째 데이터) 인덱스 바운드 방어
        if (currentIndex <= 0 || historicalDataCache == null || historicalDataCache.size() <= currentIndex) {
            resultMap.put("eom", 0.0);
            return "⏳ [데이터 축적] EOM 계산을 위한 이전 데이터가 부족합니다.";
        }

        // 이전 날짜 데이터 가져오기 (Index -1 방지 완료)
        DailyStockPrice prevDayData = historicalDataCache.get(currentIndex - 1);

        // 🎯 2. EOM 수치 계산
        double high = targetDayData.getHighPrice();
        double low = targetDayData.getLowPrice();
        double prevHigh = prevDayData.getHighPrice();
        double prevLow = prevDayData.getLowPrice();
        double volume = targetDayData.getVolume();

        // 거래량이 0일 경우 0으로 예외 처리
        if (volume == 0) {
            resultMap.put("eom", 0.0);
            return "📊 [EOM] 무빙용이성지수: 0.0000";
        }

        // Distance Moved = ((High + Low) / 2) - ((PrevHigh + PrevLow) / 2)
        double distanceMoved = ((high + low) / 2.0) - ((prevHigh + prevLow) / 2.0);

        // Box Ratio = (Volume / 10000) / (High - Low)  (단위 조정을 위해 10,000 나누기 활용)
        double highLowDiff = high - low;
        if (highLowDiff == 0) {
            highLowDiff = 1; // 0으로 나누기 방지
        }
        double boxRatio = (volume / 10000.0) / highLowDiff;

        // EOM 1일치 계산
        double eom = boxRatio == 0 ? 0 : distanceMoved / boxRatio;

        // 차트용 JSON 매핑
        resultMap.put("eom", eom);

        return String.format("📊 [EOM] 무빙용이성지수: %.4f", eom);
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