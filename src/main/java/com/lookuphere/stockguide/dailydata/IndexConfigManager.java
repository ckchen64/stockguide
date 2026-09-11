package com.lookuphere.stockguide.dailydata;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IndexConfigManager {

    private final StockIndexConfigRepository repository;

    private final Map<String, Object> configCache =
            new ConcurrentHashMap<>();


    public IndexConfigManager(
            StockIndexConfigRepository repository
    ) {
        this.repository = repository;
    }


    /**
     * Spring Bean 생성 직후
     * DB의 설정값을 메모리 캐시에 적재합니다.
     */
    @PostConstruct
    public void init() {
        refreshCache();
    }


    /**
     * =========================================================
     * DB의 최신 설정값을 메모리 캐시에 동기화
     * =========================================================
     */
    public synchronized void refreshCache() {

        configCache.clear();

        repository.findAll().forEach(config -> {

            /*
             * 현재 단계에서는 공통 설정만 사용합니다.
             *
             * 추후 사용자별 설정 기능을 적용하면
             * USER_ID:KEY 형태로 변경할 예정입니다.
             */
            String cacheKey =
                    config.getConfigKey();

            if (config.getIntValue() != null) {

                configCache.put(
                        cacheKey,
                        config.getIntValue()
                );

            } else if (config.getDoubleValue() != null) {

                configCache.put(
                        cacheKey,
                        config.getDoubleValue()
                );
            }


            /*
             * =================================================
             * 추후 사용자별 설정 적용 예정 코드
             * =================================================
             *
             * String cacheKey =
             *         (config.getUserId() != null
             *          && !config.getUserId().isBlank())
             *         ? config.getUserId()
             *              + ":"
             *              + config.getConfigKey()
             *         : config.getConfigKey();
             *
             * if (config.getIntValue() != null) {
             *     configCache.put(
             *             cacheKey,
             *             config.getIntValue()
             *     );
             *
             * } else if (config.getDoubleValue() != null) {
             *     configCache.put(
             *             cacheKey,
             *             config.getDoubleValue()
             *     );
             * }
             */
        });
    }


    // =========================================================
    // 🌐 공통 설정 조회 API
    // =========================================================

    /**
     * 정수형 설정값 조회
     */
    public int getInt(
            String key,
            int defaultValue
    ) {

        return parseInteger(
                configCache.get(key),
                defaultValue
        );
    }


    /**
     * 실수형 설정값 조회
     */
    public double getDouble(
            String key,
            double defaultValue
    ) {

        return parseDouble(
                configCache.get(key),
                defaultValue
        );
    }


    /**
     * =========================================================
     * 실행 중인 설정값을 메모리 캐시에만 변경
     * =========================================================
     *
     * 주의:
     * 이 메서드는 DB에 저장하지 않습니다.
     *
     * 서버를 재시작하면
     * DB의 값으로 다시 초기화됩니다.
     */
    public synchronized void setRuntimeInt(
            String key,
            int value
    ) {

        if (key == null || key.isBlank()) {

            throw new IllegalArgumentException(
                    "설정 key가 비어 있습니다."
            );
        }

        if (value <= 0) {

            throw new IllegalArgumentException(
                    "설정값은 1 이상이어야 합니다."
            );
        }

        configCache.put(
                key,
                value
        );

        System.out.println(
                "🔧 [IndexConfigManager] Runtime 설정 변경: "
                        + key
                        + " = "
                        + value
        );
    }


    /**
     * =========================================================
     * 프론트엔드 등에 전달하기 위한 전체 설정 복사본
     * =========================================================
     *
     * configCache 자체를 반환하지 않고
     * 새로운 HashMap으로 복사하여 반환합니다.
     */
    public Map<String, Object> getAllConfigs() {

        /*
         * 혹시 캐시가 비어 있는 경우
         * DB에서 다시 읽어옵니다.
         */
        if (configCache.isEmpty()) {
            refreshCache();
        }

        return new HashMap<>(
                configCache
        );
    }


    // =========================================================
    // 👤 사용자별 설정 조회 API
    // =========================================================
    //
    // 조회 우선순위:
    //
    // 1. 사용자별 설정
    // 2. 시스템 공통 설정
    // 3. 전달받은 fallback 기본값
    //
    // =========================================================

    /**
     * 사용자별 정수 설정 조회
     */
    public int getUserInt(
            String userId,
            String key,
            int defaultValue
    ) {

        if (userId != null
                && !userId.isBlank()) {

            Object userVal =
                    configCache.get(
                            userId
                                    + ":"
                                    + key
                    );

            if (userVal != null) {

                return parseInteger(
                        userVal,
                        defaultValue
                );
            }
        }

        /*
         * 사용자별 설정이 없으면
         * 공통 설정 사용
         */
        return getInt(
                key,
                defaultValue
        );
    }


    /**
     * 사용자별 실수 설정 조회
     */
    public double getUserDouble(
            String userId,
            String key,
            double defaultValue
    ) {

        if (userId != null
                && !userId.isBlank()) {

            Object userVal =
                    configCache.get(
                            userId
                                    + ":"
                                    + key
                    );

            if (userVal != null) {

                return parseDouble(
                        userVal,
                        defaultValue
                );
            }
        }

        /*
         * 사용자별 설정이 없으면
         * 공통 설정 사용
         */
        return getDouble(
                key,
                defaultValue
        );
    }


    // =========================================================
    // 💾 DB 영구 저장 API
    // =========================================================

    /**
     * =========================================================
     * 정수형 지표 설정값을 DB에 영구 저장
     * =========================================================
     *
     * 중요:
     *
     * 1. DB에 이미 존재하는 key만 수정합니다.
     *
     * 2. 존재하지 않는 key가 들어와도
     *    새로운 row를 자동 생성하지 않습니다.
     *
     * 3. 저장 완료 후 refreshCache()를 호출하여
     *    메모리 캐시도 즉시 갱신합니다.
     *
     * 따라서 다음 지표 계산부터
     * 변경된 값이 바로 적용됩니다.
     */
    public synchronized void saveIntConfig(
            String key,
            int value
    ) {

        /*
         * key 검증
         */
        if (key == null || key.isBlank()) {

            throw new IllegalArgumentException(
                    "설정 key가 비어 있습니다."
            );
        }


        /*
         * 값 검증
         *
         * 기간값은 최소 1 이상이어야 합니다.
         */
        if (value <= 0) {

            throw new IllegalArgumentException(
                    "설정값은 1 이상이어야 합니다."
            );
        }


        /*
         * DB에서 기존 설정을 찾습니다.
         *
         * 존재하지 않는 key는 새로 만들지 않고
         * 오류 처리합니다.
         */
        StockIndexConfig config =
                repository.findById(key)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "DB에 존재하지 않는 설정 key입니다: "
                                                + key
                                )
                        );


        /*
         * int_value만 변경합니다.
         *
         * double_value는 건드리지 않습니다.
         */
        config.setIntValue(value);


        /*
         * DB 저장
         */
        repository.save(config);


        /*
         * DB 저장 직후
         * 메모리 캐시를 다시 동기화합니다.
         */
        refreshCache();


        System.out.println(
                "💾 [IndexConfigManager] DB 설정 저장 완료: "
                        + key
                        + " = "
                        + value
        );
    }


    // =========================================================
    // 🛠️ Safe Type Parsing Helper Methods
    // =========================================================

    /**
     * Object 값을 안전하게 int로 변환
     */
    private int parseInteger(
            Object val,
            int defaultValue
    ) {

        if (val == null) {
            return defaultValue;
        }

        if (val instanceof Number) {
            return ((Number) val).intValue();
        }

        try {

            return Integer.parseInt(
                    val.toString()
            );

        } catch (NumberFormatException e) {

            return defaultValue;
        }
    }


    /**
     * Object 값을 안전하게 double로 변환
     */
    private double parseDouble(
            Object val,
            double defaultValue
    ) {

        if (val == null) {
            return defaultValue;
        }

        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }

        try {

            return Double.parseDouble(
                    val.toString()
            );

        } catch (NumberFormatException e) {

            return defaultValue;
        }
    }
}