package com.lookuphere.stockguide.index;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.IndexConfigManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 🧮 ADX (Average Directional Index) 평균 방향성 지수 계산기
 */
@Component
public class AdxCalculator implements IndicatorCalculator {

    private final IndexConfigManager configManager;

    public AdxCalculator(IndexConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public String calculate(DailyStockPrice targetDayData,
                            List<DailyStockPrice> historicalDataCache,
                            int currentSimulationIndex,
                            Map<String, Object> resultMap) {

        // USER 설정 우선 탐색 (없을 경우 DB 공통 설정 -> 기본값 14 적용)
        String userId = null; // 추후 세션/인자에서 userId 전달받아 연결
        int period = configManager.getUserInt(userId, "ADX_PERIOD", 14);

        if (currentSimulationIndex < period) {
            targetDayData.setAdx(0.0);
            targetDayData.setDiPlus(0.0);
            targetDayData.setDiMinus(0.0);
            resultMap.put("adx", 0.0);
            resultMap.put("diPlus", 0.0);
            resultMap.put("diMinus", 0.0);
            return "⏳ [데이터 축적] ADX 계산을 위한 과거 데이터(N일)가 부족합니다.";
        }

        double smoothTr = 0.0;
        double smoothDmPlus = 0.0;
        double smoothDmMinus = 0.0;

        // 1. 최근 N일간의 TR, +DM, -DM 산출 및 합산 (Wilder's Smoothing 적용)
        for (int i = currentSimulationIndex - period + 1; i <= currentSimulationIndex; i++) {
            DailyStockPrice curr = historicalDataCache.get(i);
            DailyStockPrice prev = historicalDataCache.get(i - 1);

            double highDiff = curr.getHighPrice() - prev.getHighPrice();
            double lowDiff = prev.getLowPrice() - curr.getLowPrice();

            double dmPlus = (highDiff > lowDiff && highDiff > 0) ? highDiff : 0.0;
            double dmMinus = (lowDiff > highDiff && lowDiff > 0) ? lowDiff : 0.0;

            // True Range = Max(고가-저가, |고가-전일종가|, |저가-전일종가|)
            double tr1 = curr.getHighPrice() - curr.getLowPrice();
            double tr2 = Math.abs(curr.getHighPrice() - prev.getClosePrice());
            double tr3 = Math.abs(curr.getLowPrice() - prev.getClosePrice());
            double tr = Math.max(tr1, Math.max(tr2, tr3));

            smoothTr += tr;
            smoothDmPlus += dmPlus;
            smoothDmMinus += dmMinus;
        }

        // 2. +DI, -DI 산출
        double diPlus = (smoothTr == 0) ? 0.0 : (smoothDmPlus / smoothTr) * 100.0;
        double diMinus = (smoothTr == 0) ? 0.0 : (smoothDmMinus / smoothTr) * 100.0;

        // 3. DX (Directional Movement Index) 계산
        double diSum = diPlus + diMinus;
        double dx = (diSum == 0) ? 0.0 : (Math.abs(diPlus - diMinus) / diSum) * 100.0;

        // 4. ADX (DX의 N일 이동평균) 산출
        double adxSum = dx;
        int adxCount = 1;

        for (int i = currentSimulationIndex - 1; i >= Math.max(period, currentSimulationIndex - period + 1); i--) {
            double prevTr = 0.0, prevDmP = 0.0, prevDmM = 0.0;
            for (int j = i - period + 1; j <= i; j++) {
                DailyStockPrice c = historicalDataCache.get(j);
                DailyStockPrice p = historicalDataCache.get(j - 1);
                double hDiff = c.getHighPrice() - p.getHighPrice();
                double lDiff = p.getLowPrice() - c.getLowPrice();
                prevDmP += (hDiff > lDiff && hDiff > 0) ? hDiff : 0.0;
                prevDmM += (lDiff > hDiff && lDiff > 0) ? lDiff : 0.0;
                double t1 = c.getHighPrice() - c.getLowPrice();
                double t2 = Math.abs(c.getHighPrice() - p.getClosePrice());
                double t3 = Math.abs(c.getLowPrice() - p.getClosePrice());
                prevTr += Math.max(t1, Math.max(t2, t3));
            }
            double dPlus = (prevTr == 0) ? 0.0 : (prevDmP / prevTr) * 100.0;
            double dMinus = (prevTr == 0) ? 0.0 : (prevDmM / prevTr) * 100.0;
            double dSum = dPlus + dMinus;
            double pastDx = (dSum == 0) ? 0.0 : (Math.abs(dPlus - dMinus) / dSum) * 100.0;

            adxSum += pastDx;
            adxCount++;
        }

        double adx = adxSum / adxCount;

        // 반올림 처리
        double roundedAdx = Math.round(adx * 100.0) / 100.0;
        double roundedDiPlus = Math.round(diPlus * 100.0) / 100.0;
        double roundedDiMinus = Math.round(diMinus * 100.0) / 100.0;

        // 엔티티 및 결과 맵 저장
        targetDayData.setAdx(roundedAdx);
        targetDayData.setDiPlus(roundedDiPlus);
        targetDayData.setDiMinus(roundedDiMinus);

        resultMap.put("adx", roundedAdx);
        resultMap.put("diPlus", roundedDiPlus);
        resultMap.put("diMinus", roundedDiMinus);

        return String.format("📊 [ADX] 강도(%d일): %.2f | +DI: %.2f | -DI: %.2f", period, roundedAdx, roundedDiPlus, roundedDiMinus);
    }
}









//구코드
//package com.lookuphere.stockguide.index;
//
//import com.lookuphere.stockguide.dailydata.IndexConfigManager;
//import com.lookuphere.stockguide.dailydata.DailyStockPrice;
//
//import org.springframework.stereotype.Component;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//@Component
//public class AdxCalculator {
//    private final IndexConfigManager configManager;
//
//    // 웰스 와일더 평활화 누적을 위한 내부 창고
//    private final List<Double> trList = new ArrayList<>();
//    private final List<Double> dmPlusList = new ArrayList<>();
//    private final List<Double> dmMinusList = new ArrayList<>();
//    private final List<Double> dxList = new ArrayList<>();
//    private final List<Double> adxHistoryList = new ArrayList<>();
//
//    private double prevAvgTR = 0.0;
//    private double prevAvgDmPlus = 0.0;
//    private double prevAvgDmMinus = 0.0;
//    private double prevAvgDX = 0.0;
//
//    // 생성자를 통해 실시간 동적 설정 매니저를 주입받습니다.
//    public AdxCalculator(IndexConfigManager configManager) {
//        this.configManager = configManager;
//    }
//
//    public String calculate(DailyStockPrice targetDayData, List<DailyStockPrice> historicalCache, int currentIndex, Map<String, Object> resultMap) {
//
//        // 💥 [실시간 동적 반영] DB/캐시에서 관리자가 설정한 ADX 기간을 실시간으로 호출 (기본값 14)
//        int period = configManager.getInt("ADX_PERIOD", 14);
//
//        // 1. 데이터 축적 전 예외 방어선 가드 코드
//        if (currentIndex < 1) {
//            resultMap.put("adx", 0.0);
//            resultMap.put("diPlus", 0.0);
//            resultMap.put("diMinus", 0.0);
//            return "⏳ [DMI 추세 측정 중] ...";
//        }
//
//        DailyStockPrice current = targetDayData;
//        DailyStockPrice prev = historicalCache.get(currentIndex - 1);
//
//        // 2. TR (True Range) 실시간 산출
//        double hl = current.getHighPrice() - current.getLowPrice();
//        double hpc = Math.abs(current.getHighPrice() - prev.getClosePrice());
//        double lpc = Math.abs(current.getLowPrice() - prev.getClosePrice());
//        double todayTR = Math.max(hl, Math.max(hpc, lpc));
//        trList.add(todayTR);
//
//        // 3. Directional Movement (+DM, -DM) 실시간 산출
//        double upMove = current.getHighPrice() - prev.getHighPrice();
//        double downMove = prev.getLowPrice() - current.getLowPrice();
//
//        double todayDmPlus = (upMove > downMove && upMove > 0) ? upMove : 0.0;
//        double todayDmMinus = (downMove > upMove && downMove > 0) ? downMove : 0.0;
//        dmPlusList.add(todayDmPlus);
//        dmMinusList.add(todayDmMinus);
//
//        // 4. 동적 파라미터(period)치 데이터 축적 전 방어선 2단계
//        if (currentIndex < period) {
//            resultMap.put("adx", 0.0);
//            resultMap.put("diPlus", 0.0);
//            resultMap.put("diMinus", 0.0);
//            return String.format("⏳ [DMI 측정 중] ... (%d/%d)", currentIndex + 1, period);
//        }
//
//        // 5. 웰스 와일더 평활화 초기화 및 누적 연산 구동 (고정 상수 대신 변수 period 대입)
//        if (prevAvgTR == 0.0 && prevAvgDmPlus == 0.0 && prevAvgDmMinus == 0.0) {
//            double sumTR = 0;
//            double sumPlus = 0;
//            double sumMinus = 0;
//            for (int i = 0; i < period; i++) {
//                sumTR += trList.get(i);
//                sumPlus += dmPlusList.get(i);
//                sumMinus += dmMinusList.get(i);
//            }
//            prevAvgTR = sumTR / period;
//            prevAvgDmPlus = sumPlus / period;
//            prevAvgDmMinus = sumMinus / period;
//        } else {
//            prevAvgTR = ((prevAvgTR * (period - 1)) + todayTR) / period;
//            prevAvgDmPlus = ((prevAvgDmPlus * (period - 1)) + todayDmPlus) / period;
//            prevAvgDmMinus = ((prevAvgDmMinus * (period - 1)) + todayDmMinus) / period;
//        }
//
//        // 6. DI+, DI- 최종 백분율 산출
//        double diPlus = prevAvgTR == 0 ? 0 : (prevAvgDmPlus / prevAvgTR) * 100.0;
//        double diMinus = prevAvgTR == 0 ? 0 : (prevAvgDmMinus / prevAvgTR) * 100.0;
//
//        resultMap.put("diPlus", Math.round(diPlus * 10.0) / 10.0);
//        resultMap.put("diMinus", Math.round(diMinus * 10.0) / 10.0);
//
//        // 7. DX (Directional Index) 연산
//        double diDiff = Math.abs(diPlus - diMinus);
//        double diSum = diPlus + diMinus;
//        double todayDX = diSum == 0 ? 0 : (diDiff / diSum) * 100.0;
//        dxList.add(todayDX);
//
//        // 8. ADX (Average DX) 평활화 연산 (고정 상수 대신 변수 period 대입)
//        double adx;
//        if (dxList.size() < period) {
//            adx = 0.0;
//        } else if (dxList.size() == period) {
//            double sumDX = 0;
//            for (double dx : dxList) sumDX += dx;
//            adx = sumDX / period;
//            prevAvgDX = adx;
//        } else {
//            adx = ((prevAvgDX * (period - 1)) + todayDX) / period;
//            prevAvgDX = adx;
//        }
//
//        double roundedAdx = Math.round(adx * 10.0) / 10.0;
//        resultMap.put("adx", roundedAdx);
//
//        // 9. 전략적 투자 서머리 리포트 텍스트 리턴
//        if (roundedAdx >= 25.0) {
//            if (diPlus > diMinus) {
//                return String.format("⚡ [ADX 강매수] 매수우위 (ADX: %.1f | DI+: %.1f)", roundedAdx, diPlus);
//            } else {
//                return String.format("🔥 [ADX 강매도] 폭락위험 (ADX: %.1f | DI-: %.1f)", roundedAdx, diMinus);
//            }
//        } else {
//            return String.format("➖ [ADX 박스권] 변동성 응축 (ADX: %.1f)", roundedAdx);
//        }
//    }
//}