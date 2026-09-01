package com.lookuphere.stockguide.account;
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
//            @RequestParam String code,
//            // 🎯 [핵심 추가]: 리액트의 체크박스 상태를 배열(List) 형태로 수집합니다.
//            // 만약 체크박스가 하나도 선택 안 되었을 때를 대비해 required = false 가드를 세웁니다.
//            @RequestParam(value = "selectedIndicators", required = false) List<String> selectedIndicators
//    ) {
//        System.out.println("📬 [MTS 문지기] 리액트에서 지표 선택 상태 감지: " + selectedIndicators);
//
//        // 🎯 [완치 도킹]: 수집한 보조지표 리스트(selectedIndicators)를 주방장에게 함께 배달합니다!
//        Map<String, Object> simulationResult = stockService.runNextDaySimulation(code, selectedIndicators);
//
//        // 1번만 실행해서 얻은 결과를 로그로 안전하게 재사용 출력
////        System.out.println(simulationResult);
////        System.out.println("end");
//
//        // 리액트에게도 아까 받아둔 결과를 그대로 넘겨줍니다.
//        return simulationResult;
//    }
//}


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
//            @RequestParam String code,
//            // 🎯 [핵심 추가]: 리액트의 체크박스 상태를 배열(List) 형태로 수집합니다.
//            // 만약 체크박스가 하나도 선택 안 되었을 때를 대비해 required = false 가드를 세웁니다.
//            @RequestParam(value = "selectedIndicators", required = false) List<String> selectedIndicators
//    ) {
//        System.out.println("📬 [MTS 문지기] 리액트에서 지표 선택 상태 감지: " + selectedIndicators);
//
//        // 🎯 [완치 도킹]: 수집한 보조지표 리스트(selectedIndicators)를 주방장에게 함께 배달합니다!
//        Map<String, Object> simulationResult = stockService.runNextDaySimulation(code, selectedIndicators);
//
//        // 1번만 실행해서 얻은 결과를 로그로 안전하게 재사용 출력
//        System.out.println(simulationResult);
//        System.out.println("end");
//
//        // 리액트에게도 아까 받아둔 결과를 그대로 넘겨줍니다.
//        return simulationResult;
//    }
//}


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

        import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class StockSimulationController {

    private final StockService stockService;

    @GetMapping("/simulation")
    public Map<String, Object> triggerNextDaySimulation(
            @RequestParam String code,
            @RequestParam(value = "selectedIndicators", required = false) List<String> selectedIndicators
    ) {
        System.out.println("📬 [MTS 문지기] 리액트에서 지표 선택 상태 감지: " + selectedIndicators);

        // 수집한 보조지표 리스트를 주방장에게 함께 배달합니다!
        Map<String, Object> simulationResult = stockService.runNextDaySimulation(code, selectedIndicators);

        // 1번만 실행해서 얻은 결과를 로그로 안전하게 재사용 출력
        System.out.println(simulationResult);
        System.out.println("end");

        // 🎯 [종료 방지 락 도킹]: 현재 일자(now)까지 시뮬레이션이 최종 완료되었는지 검사합니다.
        // ※ 주의: 평소에도 status가 "SUCCESS"이므로, 마지막 날에만 작동하도록
        // StockService 단에서 마지막 날일 때만 "isLastDay": true 또는 "status": "FINISHED" 같은 플래그를 넘겨주도록 설계해야 합니다.
        if (simulationResult != null &&
                (Boolean.TRUE.equals(simulationResult.get("isLastDay")) || "FINISHED".equals(simulationResult.get("status")))) {

            holdServerForGraph();
        }

        // 리액트에게도 아까 받아둔 결과를 그대로 넘겨줍니다.
        return simulationResult;
    }

    /**
     * 🔥 [서버 셧다운 방지 메소드]
     * 리액트가 HTTP 응답(마지막 데이터)을 안전하게 전송받아 화면에 그릴 시간을 2초간 벌어준 뒤,
     * 백엔드 스레드를 무한 대기 상태로 잠가 톰캣 서버가 Graceful Shutdown 되는 것을 차단합니다.
     */
    private void holdServerForGraph() {
        new Thread(() -> {
            try {
                System.out.println("⏱️ [MTS 문지기] 리액트가 최종 데이터로 그래프를 그릴 수 있도록 2초간 대기합니다...");
                Thread.sleep(2000); // 리액트로 HTTP 응답 스트림이 전송 완료되는 시간을 보장

                System.out.println("🖥️ [MTS 문지기] 시뮬레이션 완료. 그래프 출력 유지를 위해 서버를 무한 대기(Holding) 상태로 묶습니다. (종료: Ctrl+C)");
                Object lock = new Object();
                synchronized (lock) {
                    lock.wait(); // 스레드를 영원히 정지시켜 JVM 및 톰캣 종료를 방지
                }
            } catch (InterruptedException e) {
                System.err.println("❌ 서버 대기 중 인터럽트가 발생했습니다.");
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}




