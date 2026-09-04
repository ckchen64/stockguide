package com.lookuphere.stockguide.account;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class StockSimulationController {

    private final StockService stockService;

    // 🎯 [신규 추가]: 브라우저 새로고침/초기화 시 호출될 인덱스 리셋 엔드포인트
    @PostMapping("/simulation/reset")
    public ResponseEntity<String> resetSimulation() {
        stockService.resetSimulationIndex();
        System.out.println("🔄 [MTS 문지기] 리액트 새로고침 요청으로 시뮬레이션 포인터가 0으로 초기화되었습니다.");
        return ResponseEntity.ok("시뮬레이션 인덱스가 초기화되었습니다.");
    }

    @GetMapping("/simulation")
    public Map<String, Object> triggerNextDaySimulation(
            @RequestParam(defaultValue = "005930") String code,
            @RequestParam(value = "selectedIndicators", required = false) List<String> selectedIndicators
    ) {
        // 🎯 [Null 방어 로직 추가]: 리액트에서 null 또는 빈 배열로 올 경우 기본 전체 지표 세팅
        List<String> activeIndicators = (selectedIndicators != null && !selectedIndicators.isEmpty())
                ? selectedIndicators
                : List.of("SMA", "MACD", "RSI", "MFI", "OBV", "SIGMA", "ADX", "CCI", "EOM");

        System.out.println("📬 [MTS 문지기] 리액트에서 지표 선택 상태 감지: " + activeIndicators);

        // 보조지표 리스트를 주방장에게 함께 배달합니다!
        Map<String, Object> simulationResult = stockService.runNextDaySimulation(code, activeIndicators);

        // 결과 로그 출력
        System.out.println(simulationResult);
        System.out.println("end");

        // 🎯 [종료 방지 락 도킹]: 시뮬레이션이 최종 완료되었는지 검사합니다.
        if (simulationResult != null &&
                (Boolean.TRUE.equals(simulationResult.get("isLastDay")) ||
                        "FINISHED".equals(simulationResult.get("status")) ||
                        "END".equals(simulationResult.get("status")))) {

            holdServerForGraph();
        }

        // 리액트에게도 결과를 넘겨줍니다.
        return simulationResult;
    }

    /**
     * 🔥 [서버 셧다운 방지 메소드]
     * 리액트가 HTTP 응답(마지막 데이터)을 안전하게 전송받아 화면에 그릴 시간을 2초간 벌어준 뒤,
     * 백엔드 스레드를 무한 대기 상태로 잠가 서버가 Shutdown 되는 것을 차단합니다.
     */
    private void holdServerForGraph() {
        new Thread(() -> {
            try {
                System.out.println("⏱️ [MTS 문지기] 리액트가 최종 데이터로 그래프를 그릴 수 있도록 2초간 대기합니다...");
                Thread.sleep(2000); // 리액트로 HTTP 응답 스트림 전송 완료 보장

                System.out.println("🖥️ [MTS 문지기] 시뮬레이션 완료. 그래프 출력 유지를 위해 서버를 무한 대기(Holding) 상태로 묶습니다. (종료: Ctrl+C)");
                Object lock = new Object();
                synchronized (lock) {
                    lock.wait(); // 스레드를 정지시켜 JVM/톰캣 종료 방지
                }
            } catch (InterruptedException e) {
                System.err.println("❌ 서버 대기 중 인터럽트가 발생했습니다.");
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}









//구코드
//package com.lookuphere.stockguide.account;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api")
//@RequiredArgsConstructor
//@CrossOrigin(origins = "http://localhost:5173")
//public class StockSimulationController {
//
//    private final StockService stockService;
//
//    @GetMapping("/simulation")
//    public Map<String, Object> triggerNextDaySimulation(
//            @RequestParam(defaultValue = "005930") String code,
//            @RequestParam(value = "selectedIndicators", required = false) List<String> selectedIndicators
//    ) {
//        // 🎯 [Null 방어 로직 추가]: 리액트에서 null 또는 빈 배열로 올 경우 기본 전체 지표 세팅
//        List<String> activeIndicators = (selectedIndicators != null && !selectedIndicators.isEmpty())
//                ? selectedIndicators
//                : List.of("SMA", "MACD", "RSI", "MFI", "OBV", "SIGMA", "ADX", "CCI", "EOM");
//
//        System.out.println("📬 [MTS 문지기] 리액트에서 지표 선택 상태 감지: " + activeIndicators);
//
//        // 보조지표 리스트를 주방장에게 함께 배달합니다!
//        Map<String, Object> simulationResult = stockService.runNextDaySimulation(code, activeIndicators);
//
//        // 결과 로그 출력
//        System.out.println(simulationResult);
//        System.out.println("end");
//
//        // 🎯 [종료 방지 락 도킹]: 시뮬레이션이 최종 완료되었는지 검사합니다.
//        if (simulationResult != null &&
//                (Boolean.TRUE.equals(simulationResult.get("isLastDay")) ||
//                        "FINISHED".equals(simulationResult.get("status")) ||
//                        "END".equals(simulationResult.get("status")))) {
//
//            holdServerForGraph();
//        }
//
//        // 리액트에게도 결과를 넘겨줍니다.
//        return simulationResult;
//    }
//
//    /**
//     * 🔥 [서버 셧다운 방지 메소드]
//     * 리액트가 HTTP 응답(마지막 데이터)을 안전하게 전송받아 화면에 그릴 시간을 2초간 벌어준 뒤,
//     * 백엔드 스레드를 무한 대기 상태로 잠가 서버가 Shutdown 되는 것을 차단합니다.
//     */
//    private void holdServerForGraph() {
//        new Thread(() -> {
//            try {
//                System.out.println("⏱️ [MTS 문지기] 리액트가 최종 데이터로 그래프를 그릴 수 있도록 2초간 대기합니다...");
//                Thread.sleep(2000); // 리액트로 HTTP 응답 스트림 전송 완료 보장
//
//                System.out.println("🖥️ [MTS 문지기] 시뮬레이션 완료. 그래프 출력 유지를 위해 서버를 무한 대기(Holding) 상태로 묶습니다. (종료: Ctrl+C)");
//                Object lock = new Object();
//                synchronized (lock) {
//                    lock.wait(); // 스레드를 정지시켜 JVM/톰캣 종료 방지
//                }
//            } catch (InterruptedException e) {
//                System.err.println("❌ 서버 대기 중 인터럽트가 발생했습니다.");
//                Thread.currentThread().interrupt();
//            }
//        }).start();
//    }
//}




