package com.lookuphere.stockguide.dailydata;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/index-config")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class IndexConfigController {

    private final IndexConfigManager indexConfigManager;

    /**
     * 현재 백엔드 계산기가 실제 사용하는 최종 파라미터 조회
     *
     * DB에 값이 있으면 DB 값 사용
     * DB에 값이 없으면 각 계산기와 동일한 기본값 사용
     */
    @GetMapping
    public Map<String, Object> getIndexConfigs() {

        Map<String, Object> configs = new LinkedHashMap<>();

        // SMA
        configs.put(
                "SMA_SHORT_WINDOW",
                indexConfigManager.getInt("SMA_SHORT_WINDOW", 5)
        );

        configs.put(
                "SMA_MEDIUM_WINDOW",
                indexConfigManager.getInt("SMA_MEDIUM_WINDOW", 20)
        );

        configs.put(
                "SMA_LONG_WINDOW",
                indexConfigManager.getInt("SMA_LONG_WINDOW", 60)
        );

        // MACD
        configs.put(
                "MACD_FAST_PERIOD",
                indexConfigManager.getInt("MACD_FAST_PERIOD", 12)
        );

        configs.put(
                "MACD_SLOW_PERIOD",
                indexConfigManager.getInt("MACD_SLOW_PERIOD", 26)
        );

        configs.put(
                "MACD_SIGNAL_PERIOD",
                indexConfigManager.getInt("MACD_SIGNAL_PERIOD", 9)
        );

        // RSI
        configs.put(
                "RSI_PERIOD",
                indexConfigManager.getInt("RSI_PERIOD", 14)
        );

        // MFI
        configs.put(
                "MFI_PERIOD",
                indexConfigManager.getInt("MFI_PERIOD", 14)
        );

        configs.put(
                "MFI_SIGNAL_PERIOD",
                indexConfigManager.getInt("MFI_SIGNAL_PERIOD", 9)
        );

        // Sigma
        configs.put(
                "SIGMA_PERIOD",
                indexConfigManager.getInt("SIGMA_PERIOD", 20)
        );

        configs.put(
                "SIGMA_SIGNAL_PERIOD",
                indexConfigManager.getInt("SIGMA_SIGNAL_PERIOD", 9)
        );

        // ADX / DI
        configs.put(
                "ADX_PERIOD",
                indexConfigManager.getInt("ADX_PERIOD", 14)
        );

        // CCI
        configs.put(
                "CCI_PERIOD",
                indexConfigManager.getInt("CCI_PERIOD", 20)
        );

        configs.put(
                "CCI_SIGNAL_PERIOD",
                indexConfigManager.getInt("CCI_SIGNAL_PERIOD", 9)
        );

        // EOM
        configs.put(
                "EOM_PERIOD",
                indexConfigManager.getInt("EOM_PERIOD", 14)
        );

        System.out.println(
                "📊 [IndexConfigController] 실제 적용 지표 설정: "
                        + configs
        );

        return configs;
    }

    /**
     * 지표 파라미터 변경
     *
     * 1. MySQL stock_index_config 저장
     * 2. IndexConfigManager 캐시 갱신
     * 3. 다음 지표 계산부터 즉시 새 값 적용
     */
    @PutMapping
    public Map<String, Object> updateIndexConfig(
            @RequestBody Map<String, Integer> request
    ) {

        if (request == null || request.isEmpty()) {
            throw new IllegalArgumentException(
                    "변경할 설정값이 없습니다."
            );
        }

        String key = request.keySet()
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "변경할 설정값이 없습니다."
                        )
                );

        Integer value = request.get(key);

        if (value == null || value <= 0) {
            throw new IllegalArgumentException(
                    "설정값은 1 이상이어야 합니다."
            );
        }

        indexConfigManager.saveIntConfig(
                key,
                value
        );

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put("status", "SUCCESS");
        result.put("key", key);
        result.put("value", value);
        result.put("persisted", true);

        return result;
    }
}