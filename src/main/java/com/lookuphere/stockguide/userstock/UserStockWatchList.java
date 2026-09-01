package com.lookuphere.stockguide.userstock;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor
@Table(
        name = "user_stock_watchlist",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_stock", columnNames = {"userId", "stockCode"})
        }
)
public class UserStockWatchList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 10)
    private String stockCode;

    @Column(nullable = false, length = 50)
    private String stockName;

    private boolean isRealtimeActive; // 실시간 수신 활성화 여부

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isRealtimeActive = true;
    }
}