package com.lookuphere.stockguide.dailydata;

import com.lookuphere.stockguide.index.IndicatorCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockIndicatorEngineService {

    private final DailyStockPriceRepository priceRepository;
    private final IndexConfigManager configManager;

    // 💡 IndicatorCalculator를 상속받은 모든 @Component 빈을 일괄 주입받습니다.
    private final List<IndicatorCalculator> calculators;

    @Transactional
    public DailyStockPrice processIncrementalCandle(String stockCode, DailyStockPrice newCandle) {
        int lookbackWindow = configManager.getInt("MAX_LOOKBACK_WINDOW", 200);

        List<DailyStockPrice> recentHistory = priceRepository.findRecentPrices(
                stockCode, PageRequest.of(0, lookbackWindow)
        );

        List<DailyStockPrice> calculationList = new ArrayList<>(recentHistory);
        Collections.reverse(calculationList);
        calculationList.add(newCandle);

        calculateAllIndicators(calculationList);

        DailyStockPrice calculatedData = calculationList.get(calculationList.size() - 1);
        return priceRepository.findByStockCodeAndTradeDate(stockCode, calculatedData.getTradeDate())
                .map(existing -> {
                    updateFields(existing, calculatedData);
                    return priceRepository.save(existing);
                })
                .orElseGet(() -> priceRepository.save(calculatedData));
    }

    private void calculateAllIndicators(List<DailyStockPrice> list) {
        int targetIdx = list.size() - 1;
        if (targetIdx < 0) return;

        DailyStockPrice target = list.get(targetIdx);
        Map<String, Object> resultMap = new HashMap<>();

        // 💡 주입받은 보조지표 계산기들을 순회하며 일괄 연산 실행
        for (IndicatorCalculator calculator : calculators) {
            calculator.calculate(target, list, targetIdx, resultMap);
        }
    }

    private void updateFields(DailyStockPrice target, DailyStockPrice source) {
        target.setStockName(source.getStockName()); // 💡 종목명 동기화 추가
        target.setOpenPrice(source.getOpenPrice());
        target.setHighPrice(source.getHighPrice());
        target.setLowPrice(source.getLowPrice());
        target.setClosePrice(source.getClosePrice());
        target.setVolume(source.getVolume());

        // 지표 필드 일괄 갱신
        target.setSma05(source.getSma05());
        target.setSma20(source.getSma20());
        target.setSma60(source.getSma60());
        target.setRsi(source.getRsi());
        target.setMacd(source.getMacd());
        target.setMacdSignal(source.getMacdSignal());
        target.setMacdHist(source.getMacdHist());
        target.setObv(source.getObv());
        target.setMfi(source.getMfi());
        target.setMfiSignal(source.getMfiSignal());
        target.setSigma(source.getSigma());
        target.setSigmaSignal(source.getSigmaSignal());
        target.setAdx(source.getAdx());
        target.setDiPlus(source.getDiPlus());
        target.setDiMinus(source.getDiMinus());
        target.setCci(source.getCci());
        target.setCciSignal(source.getCciSignal());
        target.setEom(source.getEom());
    }
}









//구버전
//package com.lookuphere.stockguide.dailydata;
//
////이제 이전 단계에서 구현했던 증분 계산 서비스가 하드코딩된 숫자 대신
//// IndexConfigManager의 실시간 캐시값을 바라보도록 변경합니다.
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class StockIndicatorEngineService {
//
//    private final DailyStockPriceRepository priceRepository;
//    private final IndexConfigManager configManager;
//
//    @Transactional
//    public DailyStockPrice processIncrementalCandle(String stockCode, DailyStockPrice newCandle) {
//        // [1] 시스템 전체 가중치 적용: 최대 탐색 윈도우 동적 통제
//        int lookbackWindow = configManager.getInt("MAX_LOOKBACK_WINDOW", 200);
//
//        List<DailyStockPrice> recentHistory = priceRepository.findRecentPrices(
//                stockCode, PageRequest.of(0, lookbackWindow)
//        );
//
//        List<DailyStockPrice> calculationList = new ArrayList<>(recentHistory);
//        Collections.reverse(calculationList);
//        calculationList.add(newCandle);
//
//        // [2] 모든 파라미터가 반영되는 종합 지표 연산기 가동
//        calculateAllIndicators(calculationList);
//
//        DailyStockPrice calculatedData = calculationList.get(calculationList.size() - 1);
//        return priceRepository.findByStockCodeAndTradeDate(stockCode, calculatedData.getTradeDate())
//                .map(existing -> {
//                    updateFields(existing, calculatedData);
//                    return priceRepository.save(existing);
//                })
//                .orElseGet(() -> priceRepository.save(calculatedData));
//    }
//
//    /**
//     * DB의 모든 설정값을 가져와 변수에 바인딩하고 전체 기술적 지표를 계산합니다.
//     */
//    private void calculateAllIndicators(List<DailyStockPrice> list) {
//        int targetIdx = list.size() - 1;
//        if (targetIdx < 0) return;
//
//        DailyStockPrice target = list.get(targetIdx);
//
//        // 🎛️ DB 파라미터 실시간 제어 스위치들
//        int shortSma = configManager.getInt("SMA_SHORT_WINDOW", 5);
//        int midSma = configManager.getInt("SMA_MEDIUM_WINDOW", 20);
//        int longSma = configManager.getInt("SMA_LONG_WINDOW", 60);
//
//        int rsiPeriod = configManager.getInt("RSI_PERIOD", 14);
//        int mfiPeriod = configManager.getInt("MFI_PERIOD", 14);
//
//        int macdFast = configManager.getInt("MACD_FAST", 12);
//        int macdSlow = configManager.getInt("MACD_SLOW", 26);
//        int macdSignalPeriod = configManager.getInt("MACD_SIGNAL", 9);
//
//        int cciPeriod = configManager.getInt("CCI_PERIOD", 20);
//        int cciSignalPeriod = configManager.getInt("CCI_SIGNAL", 9);
//
//        int sigmaPeriod = configManager.getInt("SIGMA_PERIOD", 20);
//        double sigmaMultiplier = configManager.getDouble("SIGMA_MULTIPLIER", 2.0);
//
//        int adxPeriod = configManager.getInt("ADX_PERIOD", 14);
//
//        // ------------------------------------------------------------------------
//        // 1층. 이동평균선 (SMA 05, 20, 60) 계산
//        // ------------------------------------------------------------------------
//        target.setSma05(calculateSMA(list, targetIdx, shortSma));
//        target.setSma20(calculateSMA(list, targetIdx, midSma));
//        target.setSma60(calculateSMA(list, targetIdx, longSma));
//
//        // ------------------------------------------------------------------------
//        // 2층-1. RSI (%) 계산
//        // ------------------------------------------------------------------------
//        if (list.size() > rsiPeriod) {
//            double gains = 0;
//            double losses = 0;
//            for (int i = targetIdx - rsiPeriod + 1; i <= targetIdx; i++) {
//                double diff = list.get(i).getClosePrice() - list.get(i - 1).getClosePrice();
//                if (diff > 0) gains += diff;
//                else losses -= diff;
//            }
//            double avgGain = gains / rsiPeriod;
//            double avgLoss = losses / rsiPeriod;
//            if (avgLoss == 0) target.setRsi(100.0);
//            else {
//                double rs = avgGain / avgLoss;
//                target.setRsi(100.0 - (100.0 / (1.0 + rs)));
//            }
//        }
//
//        // ------------------------------------------------------------------------
//        // 2층-2. MFI (자금흐름지수) 계산
//        // ------------------------------------------------------------------------
//        if (list.size() > mfiPeriod) {
//            double posFlow = 0;
//            double negFlow = 0;
//            for (int i = targetIdx - mfiPeriod + 1; i <= targetIdx; i++) {
//                double tpToday = (list.get(i).getHighPrice() + list.get(i).getLowPrice() + list.get(i).getClosePrice()) / 3.0;
//                double tpPrev = (list.get(i-1).getHighPrice() + list.get(i-1).getLowPrice() + list.get(i-1).getClosePrice()) / 3.0;
//                double mf = tpToday * list.get(i).getVolume();
//                if (tpToday > tpPrev) posFlow += mf;
//                else if (tpToday < tpPrev) negFlow += mf;
//            }
//            if (negFlow == 0) target.setMfi(100.0);
//            else target.setMfi(100.0 - (100.0 / (1.0 + (posFlow / negFlow))));
//        }
//
//        // ------------------------------------------------------------------------
//        // 2층-3. MACD 및 시그널 계산 (EMA 기반)
//        // ------------------------------------------------------------------------
//        List<Double> macdHistory = new ArrayList<>();
//        double emaFast = list.get(0).getClosePrice();
//        double emaSlow = list.get(0).getClosePrice();
//
//        double kFast = 2.0 / (macdFast + 1.0);
//        double kSlow = 2.0 / (macdSlow + 1.0);
//
//        for (DailyStockPrice price : list) {
//            emaFast = (price.getClosePrice() * kFast) + (emaFast * (1.0 - kFast));
//            emaSlow = (price.getClosePrice() * kSlow) + (emaSlow * (1.0 - kSlow));
//            macdHistory.add(emaFast - emaSlow);
//        }
//        target.setMacd(macdHistory.get(macdHistory.size() - 1));
//
//        // MACD 시그널선 계산 (MACD 데이터의 EMA)
//        if (macdHistory.size() >= macdSignalPeriod) {
//            double signalEma = macdHistory.get(macdHistory.size() - macdSignalPeriod);
//            double kSignal = 2.0 / (macdSignalPeriod + 1.0);
//            for (int i = macdHistory.size() - macdSignalPeriod; i < macdHistory.size(); i++) {
//                signalEma = (macdHistory.get(i) * kSignal) + (signalEma * (1.0 - kSignal));
//            }
//            target.setMacdSignal(signalEma);
//        }
//
//        // ------------------------------------------------------------------------
//        // 3층-1. OBV 에너지 계산
//        // ------------------------------------------------------------------------
//        if (targetIdx > 0) {
//            double prevObv = (list.get(targetIdx - 1).getObv() != null) ? list.get(targetIdx - 1).getObv() : 0.0;
//            double prevClose = list.get(targetIdx - 1).getClosePrice();
//            if (target.getClosePrice() > prevClose) target.setObv(prevObv + target.getVolume());
//            else if (target.getClosePrice() < prevClose) target.setObv(prevObv - target.getVolume());
//            else target.setObv(prevObv);
//        } else {
//            target.setObv((double)target.getVolume());
//        }
//
//        // ------------------------------------------------------------------------
//        // 3층-2. SIGMA 변동성 및 볼린저밴드 이격 가중치 계산
//        // ------------------------------------------------------------------------
//        Double sigmaSma = calculateSMA(list, targetIdx, sigmaPeriod);
//        if (sigmaSma != null) {
//            double variance = 0;
//            for (int i = targetIdx - sigmaPeriod + 1; i <= targetIdx; i++) {
//                variance += Math.pow(list.get(i).getClosePrice() - sigmaSma, 2);
//            }
//            double stdDev = Math.sqrt(variance / sigmaPeriod);
//            // 가중치(Multiplier)를 통제하여 최종 시그마 변동성 지수 확정
//            target.setSigma(stdDev * sigmaMultiplier);
//        }
//
//        // ------------------------------------------------------------------------
//        // 3층-3. CCI 및 CCI 시그널 계산
//        // ------------------------------------------------------------------------
//        List<Double> cciHistory = new ArrayList<>();
//        if (list.size() >= cciPeriod) {
//            for (int j = cciPeriod - 1; j < list.size(); j++) {
//                double sumTp = 0;
//                for (int i = j - cciPeriod + 1; i <= j; i++) {
//                    sumTp += (list.get(i).getHighPrice() + list.get(i).getLowPrice() + list.get(i).getClosePrice()) / 3.0;
//                }
//                double smaTp = sumTp / cciPeriod;
//                double currentTp = (list.get(j).getHighPrice() + list.get(j).getLowPrice() + list.get(j).getClosePrice()) / 3.0;
//
//                double meanDev = 0;
//                for (int i = j - cciPeriod + 1; i <= j; i++) {
//                    double tp = (list.get(i).getHighPrice() + list.get(i).getLowPrice() + list.get(i).getClosePrice()) / 3.0;
//                    meanDev += Math.abs(tp - smaTp);
//                }
//                meanDev = meanDev / cciPeriod;
//                double cci = (meanDev == 0) ? 0 : (currentTp - smaTp) / (0.015 * meanDev);
//                cciHistory.add(cci);
//            }
//            target.setCci(cciHistory.get(cciHistory.size() - 1));
//
//            if (cciHistory.size() >= cciSignalPeriod) {
//                double cciSignalSum = 0;
//                for (int i = cciHistory.size() - cciSignalPeriod; i < cciHistory.size(); i++) {
//                    cciSignalSum += cciHistory.get(i);
//                }
//                target.setCciSignal(cciSignalSum / cciSignalPeriod);
//            }
//        }
//
//        // ------------------------------------------------------------------------
//        // 4층. ADX / DMI 방향성 지수 계산
//        // ------------------------------------------------------------------------
//        if (list.size() > adxPeriod) {
//            double trSum = 0, dmPlusSum = 0, dmMinusSum = 0;
//            // 웰스 와일더(Welles Wilder) 평활화 적용을 위한 기초 체력 연산
//            for (int i = targetIdx - adxPeriod + 1; i <= targetIdx; i++) {
//                double highDiff = list.get(i).getHighPrice() - list.get(i - 1).getHighPrice();
//                double lowDiff = list.get(i - 1).getLowPrice() - list.get(i).getLowPrice();
//
//                double tr = Math.max(list.get(i).getHighPrice() - list.get(i).getLowPrice(),
//                        Math.max(Math.abs(list.get(i).getHighPrice() - list.get(i - 1).getClosePrice()),
//                                Math.abs(list.get(i).getLowPrice() - list.get(i - 1).getClosePrice())));
//                trSum += tr;
//
//                double dp = (highDiff > lowDiff && highDiff > 0) ? highDiff : 0;
//                double dm = (lowDiff > highDiff && lowDiff > 0) ? lowDiff : 0;
//                dmPlusSum += dp;
//                dmMinusSum += dm;
//            }
//
//            if (trSum != 0) {
//                double diPlus = (dmPlusSum / trSum) * 100.0;
//                double diMinus = (dmMinusSum / trSum) * 100.0;
//                target.setDiPlus(diPlus);
//                target.setDiMinus(diMinus);
//
//                double dx = (diPlus + diMinus == 0) ? 0 : (Math.abs(diPlus - diMinus) / (diPlus + diMinus)) * 100.0;
//                target.setAdx(dx); // 간소화된 증분 ADX 주입
//            }
//        }
//    }
//
//    /**
//     * 단순이동평균(SMA) 공통 헬퍼 메서드
//     */
//    private Double calculateSMA(List<DailyStockPrice> list, int targetIdx, int period) {
//        if (list.size() < period) return null;
//        double sum = 0;
//        for (int i = targetIdx; i > targetIdx - period; i--) {
//            sum += list.get(i).getClosePrice();
//        }
//        return sum / (double) period;
//    }
//
//    private void updateFields(DailyStockPrice target, DailyStockPrice source) {
//        target.setOpenPrice(source.getOpenPrice()); target.setHighPrice(source.getHighPrice());
//        target.setLowPrice(source.getLowPrice()); target.setClosePrice(source.getClosePrice());
//        target.setVolume(source.getVolume()); target.setSma05(source.getSma05());
//        target.setSma20(source.getSma20()); target.setSma60(source.getSma60());
//        target.setMacd(source.getMacd()); target.setMacdSignal(source.getMacdSignal());
//        target.setRsi(source.getRsi()); target.setObv(source.getObv());
//        target.setMfi(source.getMfi()); target.setSigma(source.getSigma());
//        target.setAdx(source.getAdx()); target.setDiPlus(source.getDiPlus());
//        target.setDiMinus(source.getDiMinus()); target.setCci(source.getCci());
//        target.setCciSignal(source.getCciSignal());
//    }
//}