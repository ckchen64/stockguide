package com.lookuphere.stockguide.account;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

    @Entity
    @Table(name = "virtual_account")
    public class VirtualAccount {

        @Id
        private String stockCode;   // 종목 코드 (예: "005930")
        private Long balanceMoney;  // 나의 남은 가상 예수금 (원)
        private Long stockQuantity; // 현재 보유 중인 주식 수량 (주)
        private Long averagePrice;  // 주식 매입 평단가 (원)

        public VirtualAccount() {}

        // 💰 최초 계좌 개설을 위한 생성자 (기본 예수금 1,000만 원 지급)
        public VirtualAccount(String stockCode) {
            this.stockCode = stockCode;
            this.balanceMoney = 10000000L; // 1,000만 원 기본 자본금 지급
            this.stockQuantity = 0L;
            this.averagePrice = 0L;
        }

        // Getters and Setters
        public String getStockCode() { return stockCode; }
        public void setStockCode(String stockCode) { this.stockCode = stockCode; }

        public Long getBalanceMoney() { return balanceMoney; }
        public void setBalanceMoney(Long balanceMoney) { this.balanceMoney = balanceMoney; }

        public Long getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(Long stockQuantity) { this.stockQuantity = stockQuantity; }

        public Long getAveragePrice() { return averagePrice; }
        public void setAveragePrice(Long averagePrice) { this.averagePrice = averagePrice; }
    }
