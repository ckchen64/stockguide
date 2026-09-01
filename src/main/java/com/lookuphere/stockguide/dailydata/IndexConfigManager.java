package com.lookuphere.stockguide.dailydata;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IndexConfigManager {

    private final StockIndexConfigRepository repository;
    private final Map<String, Object> configCache = new ConcurrentHashMap<>();

    public IndexConfigManager(StockIndexConfigRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        refreshCache();
    }

    /**
     * DB의 최신 설정값 동기화
     */
    public synchronized void refreshCache() {
        configCache.clear();
        repository.findAll().forEach(config -> {
            // 사용자별 설정후 아래 코드로 대체하는 임시조치
            String cacheKey = config.getConfigKey();

            if (config.getIntValue() != null) {
                configCache.put(cacheKey, config.getIntValue());
            } else if (config.getDoubleValue() != null) {
                configCache.put(cacheKey, config.getDoubleValue());
            }
            // 사용자별 설정(userId 존재 시 USER_ID:KEY 형태)과 공통 설정 구분 캐싱
//            String cacheKey = (config.getUserId() != null && !config.getUserId().isBlank())
//                    ? config.getUserId() + ":" + config.getConfigKey()
//                    : config.getConfigKey();
//
//            if (config.getIntValue() != null) {
//                configCache.put(cacheKey, config.getIntValue());
//            } else if (config.getDoubleValue() != null) {
//                configCache.put(cacheKey, config.getDoubleValue());
//            }

        });
    }

    // =========================================================================
    // 🌐 [공통 설정 조회 API]
    // =========================================================================
    public int getInt(String key, int defaultValue) {
        return parseInteger(configCache.get(key), defaultValue);
    }

    public double getDouble(String key, double defaultValue) {
        return parseDouble(configCache.get(key), defaultValue);
    }

    // =========================================================================
    // 👤 [User 개인 설정 수용 API] 사용자 설정 -> 시스템 기본 설정 -> Fallback 순으로 탐색
    // =========================================================================
    public int getUserInt(String userId, String key, int defaultValue) {
        if (userId != null && !userId.isBlank()) {
            Object userVal = configCache.get(userId + ":" + key);
            if (userVal != null) {
                return parseInteger(userVal, defaultValue);
            }
        }
        return getInt(key, defaultValue); // 사용자 설정 없으면 공통 설정 적용
    }

    public double getUserDouble(String userId, String key, double defaultValue) {
        if (userId != null && !userId.isBlank()) {
            Object userVal = configCache.get(userId + ":" + key);
            if (userVal != null) {
                return parseDouble(userVal, defaultValue);
            }
        }
        return getDouble(key, defaultValue);
    }

    // =========================================================================
    // 🛠️ Safe Type Parsing Helper Methods
    // =========================================================================
    private int parseInteger(Object val, int defaultValue) {
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double parseDouble(Object val, double defaultValue) {
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}









//구버전
//package com.lookuphere.stockguide.dailydata;
//
////DB에 저장된 파라미터들을 ConcurrentHashMap을 이용해 메모리에 안전하게 캐싱하고,
//// 관리자가 설정을 변경하면 즉시 동기화해 주는 관제탑 클래스입니다.
//
//import org.springframework.stereotype.Component;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
//@Component
//public class IndexConfigManager {
//
//    private final StockIndexConfigRepository repository;
//    private final Map<String, Object> configCache = new ConcurrentHashMap<>();
//
//    public IndexConfigManager(StockIndexConfigRepository repository) {
//        this.repository = repository;
//    }
//
//    // DB의 최신 설정값들을 캐시 맵으로 동기화하는 메서드
//    public void refreshCache() {
//        repository.findAll().forEach(config -> {
//            if (config.getIntValue() != null) {
//                configCache.put(config.getConfigKey(), config.getIntValue());
//            } else if (config.getDoubleValue() != null) {
//                configCache.put(config.getConfigKey(), config.getDoubleValue());
//            }
//        });
//    }
//
//    public int getInt(String key, int defaultValue) {
//        if (configCache.isEmpty()) refreshCache(); // 첫 호출 시 자동 로드
//        Object val = configCache.get(key);
//        return val instanceof Integer ? (Integer) val : defaultValue;
//    }
//
//    public double getDouble(String key, double defaultValue) {
//        if (configCache.isEmpty()) refreshCache();
//        Object val = configCache.get(key);
//        return val instanceof Double ? (Double) val : defaultValue;
//    }
//}