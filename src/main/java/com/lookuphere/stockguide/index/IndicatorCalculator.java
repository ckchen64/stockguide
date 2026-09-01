package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import java.util.List;
import java.util.Map;

public interface IndicatorCalculator {
    String calculate(
            DailyStockPrice targetDayData,
            List<DailyStockPrice> historicalDataCache,
            int currentSimulationIndex,
            Map<String, Object> resultMap
    );
}









//package com.lookuphere.stockguide.index;
//
//import com.lookuphere.stockguide.dailydata.DailyStockPrice;
//
//import java.util.List;
//import java.util.Map;
//
//public interface IndicatorCalculator {
//    String getName();
//    String calculate(DailyStockPrice targetData, List<Integer> prices, Map<String, Object> resultMap);
//}
