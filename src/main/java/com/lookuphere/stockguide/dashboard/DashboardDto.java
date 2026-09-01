package com.lookuphere.stockguide.dashboard;

public class DashboardDto {
    private long balanceMoney;   // 남은 예수금
    private long stockQuantity;  // 보유 주식수
    private long averagePrice;   // 매입 평단가
    private long currentPrice;   // 현재 가상 주가
    private long totalAsset;     // 총 자산 (예수금 + 주식평가액)
    private long evaluationProfit; // 평가 손익 (원)
    private double evaluationRate; // 평가 수익률 (%)

    // 생성자 및 Getters (리액트가 읽어갈 규격서)
    public DashboardDto(long balanceMoney, long stockQuantity, long averagePrice, long currentPrice) {
        this.balanceMoney = balanceMoney;
        this.stockQuantity = stockQuantity;
        this.averagePrice = averagePrice;
        this.currentPrice = currentPrice;

        long stockEvaluation = stockQuantity * currentPrice; // 현재 주식 가치 평가
        this.totalAsset = balanceMoney + stockEvaluation;     // 예수금 + 평가액
        this.evaluationProfit = (currentPrice - averagePrice) * stockQuantity;

        if (stockQuantity > 0 && averagePrice > 0) {
            this.evaluationRate = ((double)(currentPrice - averagePrice) / (double)averagePrice) * 100.0;
        } else {
            this.evaluationProfit = 0L;
            this.evaluationRate = 0.0;
        }
    }

    public long getBalanceMoney() { return balanceMoney; }
    public long getStockQuantity() { return stockQuantity; }
    public long getAveragePrice() { return averagePrice; }
    public long getCurrentPrice() { return currentPrice; }
    public long getTotalAsset() { return totalAsset; }
    public long getEvaluationProfit() { return evaluationProfit; }
    public double getEvaluationRate() { return evaluationRate; }
}


