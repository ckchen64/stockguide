package com.lookuphere.stockguide.userstock;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserStockWatchListRepository extends JpaRepository<UserStockWatchList, Long> {

    List<UserStockWatchList> findByUserId(String userId);

    List<UserStockWatchList> findByIsRealtimeActiveTrue();

    void deleteByUserIdAndStockCode(String userId, String stockCode);
}