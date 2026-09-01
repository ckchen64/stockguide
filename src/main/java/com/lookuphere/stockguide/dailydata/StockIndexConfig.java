package com.lookuphere.stockguide.dailydata;

//지표명과 해당 지표에 들어갈 파라미터 수치를 쌍(Key-Value)으로 관리하는 테이블입니다.

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_index_config")
@Getter @Setter
@NoArgsConstructor
public class StockIndexConfig {

    @Id
    @Column(name = "config_key", length = 50)
    private String configKey;  // 예: "SMA_SHORT_WINDOW", "RSI_PERIOD"

    @Column(name = "int_value")
    private Integer intValue;   // 정수형 설정값 (예: 5, 20, 60)

    @Column(name = "double_value")
    private Double doubleValue; // 실수형 설정값 (예: 1.96, 0.05)

    private String description; // 설명
}
