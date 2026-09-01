package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 🧮 SMA (Simple Moving Average) 단순 이동평균 계산기
 */
@Component
public class SmaCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public SmaCalculator(IndexConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String calculate(DailyStockPrice targetDayData,
                            List<DailyStockPrice> historicalDataCache,
                            int currentSimulationIndex,
                            Map<String, Object> resultMap) {

        String userId = null; // 추후 세션/인자에서 userId 전달받아 연결

        // USER 설정 우선 탐색 (없을 경우 DB 공통 설정 -> 기본값 적용)
        int shortPeriod = configManager.getUserInt(userId, "SMA_SHORT_WINDOW", 5);
        int midPeriod = configManager.getUserInt(userId, "SMA_MEDIUM_WINDOW", 20);
        int longPeriod = configManager.getUserInt(userId, "SMA_LONG_WINDOW", 60);

        // 각각 5일, 20일, 60일 이동평균 산출
        double sma05 = calculateSmaForPeriod(historicalDataCache, currentSimulationIndex, shortPeriod);
        double sma20 = calculateSmaForPeriod(historicalDataCache, currentSimulationIndex, midPeriod);
        double sma60 = calculateSmaForPeriod(historicalDataCache, currentSimulationIndex, longPeriod);

        // 엔티티 및 결과 맵 저장
        targetDayData.setSma05(sma05);
        targetDayData.setSma20(sma20);
        targetDayData.setSma60(sma60);

        resultMap.put("sma05", sma05);
        resultMap.put("sma20", sma20);
        resultMap.put("sma60", sma60);

        return String.format("📊 [SMA] 5일: %.2f | 20일: %.2f | 60일: %.2f", sma05, sma20, sma60);
    }

    /**
     * 특정 기간(Period)에 대한 단순 이동평균 계산 헬퍼
     */
    private double calculateSmaForPeriod(List<DailyStockPrice> dataList, int currentIndex, int period) {
        if (currentIndex < period - 1) {
            return (double) dataList.get(currentIndex).getClosePrice(); // 과거 데이터 부족 시 당일 종가 반환
        }

        double sum = 0.0;
        for (int i = currentIndex - period + 1; i <= currentIndex; i++) {
            sum += dataList.get(i).getClosePrice();
        }

        double sma = sum / period;
        return Math.round(sma * 100.0) / 100.0;
    }
}









//구코드
//package com.lookuphere.stockguide.index;
//
//import com.lookuphere.stockguide.dailydata.IndexConfigManager; // 💡 타 패키지의 핵심 설정 매니저 임포트
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * 🧮 이동평균선(SMA 단기 / 중기 / 장기) 연산 코어 (동적 파라미터 버전)
// */
//@Component
//public class SmaCalculator {
//
//    private final IndexConfigManager configManager; // 🔄 동적 설정 매니저 주입
//
//    // 스프링 생성자 의존성 주입
//    public SmaCalculator(IndexConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    public String calculate(List<Integer> priceList, Map<String, Object> resultMap) {
//        int listSize = priceList.size();
//        if (listSize < 2) return ""; // 데이터 부족 시 빈 리포트
//
//        // 🎛️ DB 파라미터 실시간 동적 매핑 (기본값: 단기 5 / 중기 20 / 장기 60)
//        int shortPeriod = configManager.getInt("SMA_SHORT", 5);
//        int midPeriod = configManager.getInt("SMA_MID", 20);
//        int longPeriod = configManager.getInt("SMA_LONG", 60);
//
//        // 💡 프론트 차트 및 타 지표 연동용 동적 키 생성 (%02d 적용으로 sma05, sma20, sma60 형태 유지)
//        String shortKey = String.format("sma%02d", shortPeriod);
//        String midKey = String.format("sma%02d", midPeriod);
//        String longKey = String.format("sma%02d", longPeriod);
//
//        int todayPrice = priceList.get(listSize - 1);
//
//        // 1단계: 단기평균선(Short Period) 연산 및 매핑
//        double maShort;
//        if (listSize >= shortPeriod) {
//            int sumShort = 0;
//            for (int i = listSize - 1; i >= listSize - shortPeriod; i--) {
//                sumShort += priceList.get(i);
//            }
//            maShort = sumShort / (double) shortPeriod;
//            resultMap.put(shortKey, (int) Math.round(maShort));
//        } else {
//            maShort = todayPrice;
//            resultMap.put(shortKey, todayPrice);
//        }
//
//        // 2단계: 중기평균선(Mid Period) 연산 및 매핑
//        double maMid;
//        if (listSize >= midPeriod) {
//            int sumMid = 0;
//            for (int i = listSize - 1; i >= listSize - midPeriod; i--) {
//                sumMid += priceList.get(i);
//            }
//            maMid = sumMid / (double) midPeriod;
//            resultMap.put(midKey, (int) Math.round(maMid));
//        } else {
//            maMid = todayPrice;
//            resultMap.put(midKey, todayPrice);
//        }
//
//        // 3단계: 장기평균선(Long Period) 연산 및 리포트 최종 바인딩
//        if (listSize < longPeriod) {
//            resultMap.put(longKey, null); // 🎯 프론트 차트 숨김용 투명 가드 처리
//
//            // 장기선 정렬 전에도 단기/중기 크로스 멘트가 유연하게 나가도록 변수 바인딩
//            if (maShort > maMid) {
//                return String.format("⏳ [SMA 단기골든] %d일선/%d일선 가동 중! (%d일선 축적률: %d/%d)",
//                        shortPeriod, midPeriod, longPeriod, listSize, longPeriod);
//            } else {
//                return String.format("⏳ [SMA 단기데드] 추세 관망 중 (%d일선 축적률: %d/%d)",
//                        longPeriod, listSize, longPeriod);
//            }
//        }
//
//        int sumLong = 0;
//        for (int i = listSize - 1; i >= listSize - longPeriod; i--) {
//            sumLong += priceList.get(i);
//        }
//        double maLong = sumLong / (double) longPeriod;
//        resultMap.put(longKey, (int) Math.round(maLong));
//
//        // 🎯 설정된 동적 일수를 반영한 입체적인 정배열/역배열 추세 분석 리포트 빌드
//        if (maShort > maMid && maMid > maLong) {
//            return String.format("🔥 [SMA 완벽골든] 강력한 상승! (%d일: %.1f원 > %d일: %.1f원 > %d일: %.1f원)",
//                    shortPeriod, maShort, midPeriod, maMid, longPeriod, maLong);
//        } else if (maShort > maMid) {
//            return String.format("📈 [SMA 단기골든] 상승 흐름 (%d일: %.1f원 > %d일: %.1f원 | %d일선: %.1f원)",
//                    shortPeriod, maShort, midPeriod, maMid, longPeriod, maLong);
//        } else if (maShort < maMid && maMid < maLong) {
//            return String.format("💀 [SMA 역배열] 절대 관망! (%d일: %.1f원 < %d일: %.1f원 < %d일: %.1f원)",
//                    shortPeriod, maShort, midPeriod, maMid, longPeriod, maLong);
//        } else {
//            return String.format("➖ [SMA 관망] 추세 탐색 중 (%d일: %.1f원 | %d일: %.1f원 | %d일: %.1f원)",
//                    shortPeriod, maShort, midPeriod, maMid, longPeriod, maLong);
//        }
//    }
//}