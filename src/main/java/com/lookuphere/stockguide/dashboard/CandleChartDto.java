package com.lookuphere.stockguide.dashboard;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CandleChartDto {
    private String date;
    private long open;
    private long high;
    private long low;
    private long close;
    private long volume;

    // 이동평균선 3형제
    private Double sma05;
    private Double sma20;
    private Double sma60;

    // 보조지표 패널 A, B 데이터
    private Double macd;
    private Double macdSignal;
    private Double rsi;
    private Double mfi;
    private Double obv;
    private Double sigma;

    // 🧬 [ADX 패널 추가]: 이번에 프론트엔드 파이프라인과 도킹할 핵심 필드
    private Double adx;
    private Double diPlus;
    private Double diMinus;
}