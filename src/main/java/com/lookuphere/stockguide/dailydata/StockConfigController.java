package com.lookuphere.stockguide.dailydata;

//사용자가 UI 웹페이지에서 값을 바꾸고 저장 버튼을 누르면 이 API가 트리거되어,
//시스템 전체의 연산 가중치가 즉시 변경됩니다.

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class StockConfigController {

    private final IndexConfigManager configManager;
    private final StockIndexConfigRepository configRepository;

    @PutMapping("/indicators")
    public ResponseEntity<String> updateIndicatorConfig(@RequestParam String key, @RequestParam Integer value) {
        // 1. DB의 값을 업데이트합니다.
        StockIndexConfig config = configRepository.findById(key)
                .orElse(new StockIndexConfig());
        config.setConfigKey(key);
        config.setIntValue(value);
        configRepository.save(config);

        // 2. ⚡ 엔진이 바라보는 메모리 캐시를 즉시 동기화합니다.
        configManager.refreshCache();

        return ResponseEntity.ok("지표 설정 변경 및 실시간 캐시 반영 완료: " + key + " = " + value);
    }
}