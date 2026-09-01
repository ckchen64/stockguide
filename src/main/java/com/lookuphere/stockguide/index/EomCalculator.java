package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 🧮 EOM(Ease of Movement) 주가 용이성 지표 계산기
 */
@Component
public class EomCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public EomCalculator(IndexConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String calculate(DailyStockPrice targetDayData,
                            List<DailyStockPrice> historicalDataCache,
                            int currentSimulationIndex,
                            Map<String, Object> resultMap) {

        // EOM 이평 기간 (기본값: 14)
        int period = configManager.getInt("EOM_PERIOD", 14);

        if (currentSimulationIndex < 1) {
            resultMap.put("eom", 0.0);
            return "⏳ [데이터 축적] EOM 계산을 위한 이전 데이터가 부족합니다.";
        }

        // 1단계: 당일 Distance Moved 및 Box Ratio 산출
        DailyStockPrice today = targetDayData;
        DailyStockPrice yesterday = historicalDataCache.get(currentSimulationIndex - 1);

        double midToday = (today.getHighPrice() + today.getLowPrice()) / 2.0;
        double midYesterday = (yesterday.getHighPrice() + yesterday.getLowPrice()) / 2.0;
        double distanceMoved = midToday - midYesterday;

        double highLowDiff = (today.getHighPrice() - today.getLowPrice());
        if (highLowDiff == 0) highLowDiff = 1.0; // 0 분할 방지

        // 단위 조정을 위한 스케일링 상수 (보통 10,000 ~ 1,000,000 사용)
        double boxRatio = (today.getVolume() / 10000.0) / highLowDiff;
        if (boxRatio == 0) boxRatio = 1.0;

        double rawEom = distanceMoved / boxRatio;

        // 2단계: EOM N일 단순 이동평균(SMA) 계산
        double eomSum = rawEom;
        int count = 1;

        for (int i = currentSimulationIndex - 1; i >= Math.max(0, currentSimulationIndex - period + 1); i--) {
            DailyStockPrice curr = historicalDataCache.get(i);
            DailyStockPrice prev = historicalDataCache.get(i - 1);

            double midC = (curr.getHighPrice() + curr.getLowPrice()) / 2.0;
            double midP = (prev.getHighPrice() + prev.getLowPrice()) / 2.0;
            double dm = midC - midP;
            double hld = (curr.getHighPrice() - curr.getLowPrice());
            if (hld == 0) hld = 1.0;
            double br = (curr.getVolume() / 10000.0) / hld;
            if (br == 0) br = 1.0;

            eomSum += (dm / br);
            count++;
        }

        double eomSma = eomSum / count;
        double roundedEom = Math.round(eomSma * 100.0) / 100.0;

        resultMap.put("eom", roundedEom);
        // targetDayData.setEom(roundedEom); // DailyStockPrice 엔티티에 eom 필드 추가 시 주석 해제

        return String.format("📊 [EOM] 이동용이성 수치: %.2f", roundedEom);
    }
}