package com.lookuphere.stockguide.account;

import com.lookuphere.stockguide.dailydata.DailyStockPrice;
import com.lookuphere.stockguide.dailydata.DailyStockPriceRepository;
import com.lookuphere.stockguide.history.TradeHistoryService;
import com.lookuphere.stockguide.index.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockService {

    private final DailyStockPriceRepository dailyStockPriceRepository;
    private final TradeHistoryService tradeHistoryService;

    // 🎯 규격화된 계산기들 개별 주입
    private final SmaCalculator smaCalculator;
    private final MacdCalculator macdCalculator;
    private final RsiCalculator rsiCalculator;
    private final MfiCalculator mfiCalculator;
    private final ObvCalculator obvCalculator;
    private final SigmaCalculator sigmaCalculator;
    private final AdxCalculator adxCalculator;
    private final CciCalculator cciCalculator;
    private final EomCalculator eomCalculator;

    // 시뮬레이션 제어용 상태 변수들
    private int currentSimulationIndex = 0;
    private List<DailyStockPrice> historicalDataCache = new ArrayList<>();
    private LocalDate currentSimulatedDate = LocalDate.of(2025, 5, 1);

    /**
     * 🔄 [신규 추가]: 브라우저 새로고침 시 시뮬레이션 진행 포인터 및 상태 완전 초기화
     */
    public void resetSimulationIndex() {
        this.currentSimulationIndex = 0;
        this.currentSimulatedDate = LocalDate.of(2025, 5, 1);
        if (this.historicalDataCache != null) {
            this.historicalDataCache.clear(); // DB 조회 캐시 초기화하여 첫날부터 다시 로드 가능
        }
        System.out.println("🔄 [StockService] 시뮬레이션 인덱스 및 캐시가 성공적으로 리셋되었습니다.");
    }

    /**
     * 🛡️ IndexOutOfBoundsException 방어선이 적용된 종가 조회
     */
    public int getCurrentClosePrice() {
        if (this.historicalDataCache == null || this.historicalDataCache.isEmpty()) {
            return 60000;
        }

        int targetIdx = Math.max(0, Math.min(this.currentSimulationIndex - 1, this.historicalDataCache.size() - 1));
        return this.historicalDataCache.get(targetIdx).getClosePrice();
    }

    public String getCurrentSimulatedDate() {
        return this.currentSimulatedDate == null ? "" : this.currentSimulatedDate.toString();
    }

    /**
     * 🎨 시뮬레이션 코어
     */
    public Map<String, Object> runNextDaySimulation(String stockCode, List<String> selectedIndicators) {
        // 기본값 가드 세우기
        List<String> activeIndicators = (selectedIndicators != null && !selectedIndicators.isEmpty()) ? selectedIndicators :
                List.of("SMA", "MACD", "RSI", "MFI", "OBV", "SIGMA", "ADX", "CCI", "EOM");

        // 1. 최초 가동 시 또는 리셋 후 빈 캐시 주머니 채워주기
        if (this.historicalDataCache == null || this.historicalDataCache.isEmpty()) {
            this.historicalDataCache = dailyStockPriceRepository.findByStockCodeOrderByTradeDateAsc(stockCode);
        }

        // 2. 종료 가드 코드 배치
        if (this.historicalDataCache.isEmpty() || this.currentSimulationIndex >= this.historicalDataCache.size()) {
            Map<String, Object> endMap = new HashMap<>();
            endMap.put("status", "END");
            endMap.put("message", "🏁 모든 시뮬레이션 데이터가 소모되었습니다.");
            return endMap;
        }

        // 3. 타겟 데이터 및 날짜 확정
        DailyStockPrice targetDayData = this.historicalDataCache.get(this.currentSimulationIndex);
        this.currentSimulatedDate = targetDayData.getTradeDate();

        Map<String, Object> resultMap = new HashMap<>();

        // 4. 단 한 번의 진입으로 수치 매핑과 필터링된 리포트 추출 동시에 처리
        List<String> filteredReportList = this.analyzeIndicatorsForReact(targetDayData, activeIndicators, resultMap);

        // 5. 보조지표 리포트 텍스트 완성
        String textReport = String.format("📅 [%d일차 시뮬레이션] 날짜: %s | 마감 주가: %d원\n-> 지표 분석 결과:\n%s",
                this.currentSimulationIndex + 1,
                this.currentSimulatedDate.toString(),
                targetDayData.getClosePrice(),
                String.join("\n", filteredReportList)
        );

        // 6. 종합 선물세트 JSON 바인딩
        resultMap.put("status", "SUCCESS");
        resultMap.put("reportText", textReport);
        resultMap.put("date", this.currentSimulatedDate.toString());
        resultMap.put("open", targetDayData.getOpenPrice());
        resultMap.put("high", targetDayData.getHighPrice());
        resultMap.put("low", targetDayData.getLowPrice());
        resultMap.put("close", targetDayData.getClosePrice());
        resultMap.put("volume", targetDayData.getVolume());

        // 7. 가상 시계 1칸 전진
        this.currentSimulationIndex++;

        return resultMap;
    }

    /**
     * 🧮 리액트 전용 차트 데이터 바인딩 및 활성 지표 단일 연산 엔진
     */
    public List<String> analyzeIndicatorsForReact(DailyStockPrice targetDayData, List<String> activeIndicators, Map<String, Object> resultMap) {
        List<String> filteredReportList = new ArrayList<>();

        if (activeIndicators.contains("SMA")) {
            filteredReportList.add(smaCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
        }
        if (activeIndicators.contains("MACD")) {
            filteredReportList.add(macdCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
        }
        if (activeIndicators.contains("RSI")) {
            filteredReportList.add(rsiCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
        }
        if (activeIndicators.contains("MFI")) {
            filteredReportList.add(mfiCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
        }
        if (activeIndicators.contains("OBV")) {
            filteredReportList.add(obvCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
        }
        if (activeIndicators.contains("SIGMA")) {
            filteredReportList.add(sigmaCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
        }
        if (activeIndicators.contains("ADX")) {
            filteredReportList.add(adxCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
        }
        if (activeIndicators.contains("CCI")) {
            filteredReportList.add(cciCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
        }
        if (activeIndicators.contains("EOM")) {
            filteredReportList.add(eomCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
        }

        // 🛡️ 프론트엔드 Null 붕괴 방지용 기본값 세팅
        resultMap.putIfAbsent("sma05", (double) targetDayData.getClosePrice());
        resultMap.putIfAbsent("sma20", (double) targetDayData.getClosePrice());
        resultMap.putIfAbsent("sma60", (double) targetDayData.getClosePrice());
        resultMap.putIfAbsent("macd", 0.0);
        resultMap.putIfAbsent("macdSignal", 0.0);
        resultMap.putIfAbsent("macdHist", 0.0);
        resultMap.putIfAbsent("rsi", 50.0);
        resultMap.putIfAbsent("mfi", 50.0);
        resultMap.putIfAbsent("obv", 0.0);
        resultMap.putIfAbsent("sigma", 0.0);
        resultMap.putIfAbsent("adx", 0.0);
        resultMap.putIfAbsent("diPlus", 0.0);
        resultMap.putIfAbsent("diMinus", 0.0);
        resultMap.putIfAbsent("cci", 0.0);
        resultMap.putIfAbsent("eom", 0.0);

        return filteredReportList;
    }
}









// 구코드
//package com.lookuphere.stockguide.account;
//
//import com.lookuphere.stockguide.dailydata.DailyStockPrice;
//import com.lookuphere.stockguide.dailydata.DailyStockPriceRepository;
//import com.lookuphere.stockguide.history.TradeHistoryService;
//import com.lookuphere.stockguide.index.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class StockService {
//
//    private final DailyStockPriceRepository dailyStockPriceRepository;
//    private final TradeHistoryService tradeHistoryService;
//
//    // 🎯 규격화된 계산기들 개별 주입111
//    private final SmaCalculator smaCalculator;
//    private final MacdCalculator macdCalculator;
//    private final RsiCalculator rsiCalculator;
//    private final MfiCalculator mfiCalculator;
//    private final ObvCalculator obvCalculator;
//    private final SigmaCalculator sigmaCalculator;
//    private final AdxCalculator adxCalculator;
//    private final CciCalculator cciCalculator;
//
//    // 시뮬레이션 제어용 상태 변수들
//    private int currentSimulationIndex = 0;
//    private List<DailyStockPrice> historicalDataCache = new ArrayList<>();
//    private LocalDate currentSimulatedDate = LocalDate.of(2025, 5, 1);
//
//    /**
//     * 🛡️ IndexOutOfBoundsException 방어선이 적용된 종가 조회
//     */
//    public int getCurrentClosePrice() {
//        if (this.historicalDataCache == null || this.historicalDataCache.isEmpty()) {
//            return 60000;
//        }
//
//        // currentSimulationIndex가 0이거나 범위 밖인 경우 안전 처리
//        int targetIdx = Math.max(0, Math.min(this.currentSimulationIndex - 1, this.historicalDataCache.size() - 1));
//        return this.historicalDataCache.get(targetIdx).getClosePrice();
//    }
//
//    public String getCurrentSimulatedDate() {
//        return this.currentSimulatedDate == null ? "" : this.currentSimulatedDate.toString();
//    }
//
//    /**
//     * 🎨 시뮬레이션 코어
//     */
//    public Map<String, Object> runNextDaySimulation(String stockCode, List<String> selectedIndicators) {
//        // 기본값 가드 세우기 (선택된 지표가 없으면 전체 가동)
//        List<String> activeIndicators = (selectedIndicators != null && !selectedIndicators.isEmpty()) ? selectedIndicators :
//                List.of("SMA", "MACD", "RSI", "MFI", "OBV", "SIGMA", "ADX", "CCI", "EOM");
//
//        // 1. 최초 가동 시 빈 캐시 주머니 채워주기
//        if (this.historicalDataCache == null || this.historicalDataCache.isEmpty()) {
//            this.historicalDataCache = dailyStockPriceRepository.findByStockCodeOrderByTradeDateAsc(stockCode);
//        }
//
//        // 2. 종료 가드 코드 배치
//        if (this.historicalDataCache.isEmpty() || this.currentSimulationIndex >= this.historicalDataCache.size()) {
//            Map<String, Object> endMap = new HashMap<>();
//            endMap.put("status", "END");
//            endMap.put("message", "🏁 모든 시뮬레이션 데이터가 소모되었습니다.");
//            return endMap;
//        }
//
//        // 3. 타겟 데이터 및 날짜 확정
//        DailyStockPrice targetDayData = this.historicalDataCache.get(this.currentSimulationIndex);
//        this.currentSimulatedDate = targetDayData.getTradeDate();
//
//        Map<String, Object> resultMap = new HashMap<>();
//
//        // 4. 단 한 번의 진입으로 수치 매핑과 필터링된 리포트 추출 동시에 처리
//        List<String> filteredReportList = this.analyzeIndicatorsForReact(targetDayData, activeIndicators, resultMap);
//
//        // 5. 보조지표 리포트 텍스트 완성
//        String textReport = String.format("📅 [%d일차 시뮬레이션] 날짜: %s | 마감 주가: %d원\n-> 지표 분석 결과:\n%s",
//                this.currentSimulationIndex + 1,
//                this.currentSimulatedDate.toString(),
//                targetDayData.getClosePrice(),
//                String.join("\n", filteredReportList)
//        );
//
//        // 6. 종합 선물세트 JSON 바인딩
//        resultMap.put("status", "SUCCESS");
//        resultMap.put("reportText", textReport);
//        resultMap.put("date", this.currentSimulatedDate.toString());
//        resultMap.put("open", targetDayData.getOpenPrice());
//        resultMap.put("high", targetDayData.getHighPrice());
//        resultMap.put("low", targetDayData.getLowPrice());
//        resultMap.put("close", targetDayData.getClosePrice());
//        resultMap.put("volume", targetDayData.getVolume());
//
//        // 7. 가상 시계 1칸 전진
//        this.currentSimulationIndex++;
//
//        return resultMap;
//    }
//
//    /**
//     * 🧮 리액트 전용 차트 데이터 바인딩 및 활성 지표 단일 연산 엔진 (파라미터 규격 통일)
//     */
//    public List<String> analyzeIndicatorsForReact(DailyStockPrice targetDayData, List<String> activeIndicators, Map<String, Object> resultMap) {
//        List<String> filteredReportList = new ArrayList<>();
//
//        // 🎯 인터페이스 파라미터 표준(targetDayData, historicalDataCache, currentSimulationIndex, resultMap) 통일 적용
//        if (activeIndicators.contains("SMA")) {
//            filteredReportList.add(smaCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("MACD")) {
//            filteredReportList.add(macdCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("RSI")) {
//            filteredReportList.add(rsiCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("MFI")) {
//            filteredReportList.add(mfiCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("OBV")) {
//            filteredReportList.add(obvCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("SIGMA")) {
//            filteredReportList.add(sigmaCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("ADX")) {
//            filteredReportList.add(adxCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("CCI")) {
//            filteredReportList.add(cciCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("EOM")) {
//            filteredReportList.add(cciCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//
//        // 🛡️ 프론트엔드 Null 붕괴 방지용 기본값 세팅
//        resultMap.putIfAbsent("sma05", (double) targetDayData.getClosePrice());
//        resultMap.putIfAbsent("sma20", (double) targetDayData.getClosePrice());
//        resultMap.putIfAbsent("sma60", (double) targetDayData.getClosePrice());
//        resultMap.putIfAbsent("macd", 0.0);
//        resultMap.putIfAbsent("macdSignal", 0.0);
//        resultMap.putIfAbsent("macdHist", 0.0);
//        resultMap.putIfAbsent("rsi", 50.0);
//        resultMap.putIfAbsent("mfi", 50.0);
//        resultMap.putIfAbsent("obv", 0.0);
//        resultMap.putIfAbsent("sigma", 0.0);
//        resultMap.putIfAbsent("adx", 0.0);
//        resultMap.putIfAbsent("diPlus", 0.0);
//        resultMap.putIfAbsent("diMinus", 0.0);
//        resultMap.putIfAbsent("cci", 0.0);
//        resultMap.putIfAbsent("eom", 0.0);
//
//        return filteredReportList;
//    }
//}









//구코드
//package com.lookuphere.stockguide.account;
//
//import com.lookuphere.stockguide.dailydata.DailyStockPrice;
//import com.lookuphere.stockguide.dailydata.DailyStockPriceRepository;
//import com.lookuphere.stockguide.history.TradeHistoryService;
//import com.lookuphere.stockguide.index.*;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor // 💡 스프링이 final이 붙은 모든 계산기(빈)들을 완벽하게 생성자 주입해 줍니다.
//public class StockService {
//
//    private final DailyStockPriceRepository dailyStockPriceRepository;
//    private final TradeHistoryService tradeHistoryService;
//
//    // 🎯 [아키텍처 치유]: 더 이상 new를 쓰지 않고 스프링 컨테이너에서 정상 작동하는 계산기들을 주입받음
//    private final SmaCalculator smaCalculator;
//    private final MacdCalculator macdCalculator;
//    private final RsiCalculator rsiCalculator;
//    private final MfiCalculator mfiCalculator;
//    private final ObvCalculator obvCalculator;
//    private final SigmaCalculator sigmaCalculator;
//    private final AdxCalculator adxCalculator;
//    private final CciCalculator cciCalculator;
//
//    // 시뮬레이션 제어용 상태 변수들
//    private final List<Integer> priceList = new ArrayList<>();
//    private int currentSimulationIndex = 0;
//    private List<DailyStockPrice> historicalDataCache = new ArrayList<>();
//    private LocalDate currentSimulatedDate = LocalDate.of(2025, 5, 1);
//
//    public int getCurrentClosePrice() {
//        return this.historicalDataCache.isEmpty() ? 60000 :
//                this.historicalDataCache.get(this.currentSimulationIndex - 1).getClosePrice();
//    }
//
//    public String getCurrentSimulatedDate() {
//        return this.currentSimulatedDate == null ? "" : this.currentSimulatedDate.toString();
//    }
//
//    /**
//     * 🎨 종합 선물세트 시뮬레이션 엔진 가동 코어
//     */
//    public Map<String, Object> runNextDaySimulation(String stockCode, List<String> selectedIndicators) {
//        // 기본값 가드 세우기 (선택된 지표가 없으면 전체 가동)
//        List<String> activeIndicators = (selectedIndicators != null) ? selectedIndicators :
//                List.of("SMA", "MACD", "RSI", "MFI", "OBV", "SIGMA", "ADX", "CCI");
//
//        // 1. 최초 가동 시 빈 캐시 주머니 채워주기
//        if (this.historicalDataCache == null || this.historicalDataCache.isEmpty()) {
//            this.historicalDataCache = dailyStockPriceRepository.findByStockCodeOrderByTradeDateAsc(stockCode);
//        }
//
//        // 2. 종료 가드 코드 배치
//        if (this.historicalDataCache.isEmpty() || this.currentSimulationIndex >= this.historicalDataCache.size()) {
//            Map<String, Object> endMap = new HashMap<>();
//            endMap.put("status", "END");
//            endMap.put("message", "🏁 모든 시뮬레이션 데이터가 소모되었습니다.");
//            return endMap;
//        }
//
//        // 3. 타겟 데이터 및 날짜 확정
//        DailyStockPrice targetDayData = this.historicalDataCache.get(this.currentSimulationIndex);
//        this.currentSimulatedDate = targetDayData.getTradeDate();
//
//        Map<String, Object> resultMap = new HashMap<>();
//
//        // 4. 🔥 [버그 완치]: 단 한 번의 진입으로 수치 매핑과 필터링된 리포트 추출을 동시에 처리!
//        List<String> filteredReportList = this.analyzeIndicatorsForReact(targetDayData, activeIndicators, resultMap);
//
//        // 5. 보조지표 리포트 텍스트 완성
//        String textReport = String.format("📅 [%d일차 시뮬레이션] 날짜: %s | 마감 주가: %d원\n-> 지표 분석 결과:\n%s",
//                this.currentSimulationIndex + 1,
//                this.currentSimulatedDate.toString(),
//                targetDayData.getClosePrice(),
//                String.join("\n", filteredReportList)
//        );
//
//        // 6. 종합 선물세트 JSON 바인딩
//        resultMap.put("status", "SUCCESS");
//        resultMap.put("reportText", textReport);
//        resultMap.put("date", this.currentSimulatedDate.toString());
//        resultMap.put("open", targetDayData.getOpenPrice());
//        resultMap.put("high", targetDayData.getHighPrice());
//        resultMap.put("low", targetDayData.getLowPrice());
//        resultMap.put("close", targetDayData.getClosePrice());
//        resultMap.put("volume", targetDayData.getVolume());
//
//        // 7. 가상 시계 1칸 전진
//        this.currentSimulationIndex++;
//
//        return resultMap;
//    }
//
//    /**
//     * 🧮 리액트 전용 차트 데이터 바인딩 및 활성 지표 단일 연산 엔진 (중복 호출 전면 차단)
//     */
//    public List<String> analyzeIndicatorsForReact(DailyStockPrice targetDayData, List<String> activeIndicators, Map<String, Object> resultMap) {
//        int todayPrice = targetDayData.getClosePrice();
//        this.priceList.add(todayPrice);
//
//        List<String> filteredReportList = new ArrayList<>();
//
//        // 🎯 사용자가 요청한(체크박스에 체크된) 지표만 딱 '한 번씩만' 연산하여 리포트와 resultMap을 동기화합니다.
//        if (activeIndicators.contains("SMA")) {
//            filteredReportList.add(smaCalculator.calculate(this.priceList, resultMap));
//        }
//        if (activeIndicators.contains("MACD")) {
//            filteredReportList.add(macdCalculator.calculate(this.priceList, resultMap));
//        }
//        if (activeIndicators.contains("RSI")) {
//            filteredReportList.add(rsiCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("MFI")) {
//            filteredReportList.add(mfiCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("OBV")) {
//            filteredReportList.add(obvCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("SIGMA")) {
//            filteredReportList.add(sigmaCalculator.calculate(targetDayData, this.priceList, resultMap));
//        }
//        if (activeIndicators.contains("ADX")) {
//            filteredReportList.add(adxCalculator.calculate(targetDayData, this.historicalDataCache, this.currentSimulationIndex, resultMap));
//        }
//        if (activeIndicators.contains("CCI")) {
//            filteredReportList.add(cciCalculator.calculate(targetDayData, this.currentSimulationIndex, resultMap));
//        }
//
//        // 🛡️ 안전 방어선 가드 구역 (초기 데이터 부족으로 인한 프론트엔드 Null 붕괴 방지)
//        // putIfAbsent를 사용하면 계산기 내부에서 저장하지 못했을 때만 안전하게 0.0 기본값을 채워넣습니다.
//        resultMap.putIfAbsent("sigma", 0.0);
//        resultMap.putIfAbsent("adx", 0.0);
//        resultMap.putIfAbsent("diPlus", 0.0);
//        resultMap.putIfAbsent("diMinus", 0.0);
//        resultMap.putIfAbsent("cci", 0.0);
//        resultMap.putIfAbsent("cciSignal", 0.0);
//
//        return filteredReportList;
//    }
//}