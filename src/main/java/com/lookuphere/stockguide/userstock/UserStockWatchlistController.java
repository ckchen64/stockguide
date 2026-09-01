package com.lookuphere.stockguide.userstock;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
public class UserStockWatchlistController {

    private final UserStockWatchListRepository watchlistRepository;

    /**
     * 사용자 지정 감시 종목 목록 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<UserStockWatchList>> getUserWatchlist(@PathVariable String userId) {
        return ResponseEntity.ok(watchlistRepository.findByUserId(userId));
    }

    /**
     * 감시 종목 신규 추가
     */
    @PostMapping
    public ResponseEntity<UserStockWatchList> addStock(@RequestBody UserStockWatchList watchlist) {
        UserStockWatchList saved = watchlistRepository.save(watchlist);
        return ResponseEntity.ok(saved);
    }

    /**
     * 감시 종목 삭제
     */
    @DeleteMapping("/{userId}/{stockCode}")
    public ResponseEntity<Void> removeStock(@PathVariable String userId, @PathVariable String stockCode) {
        watchlistRepository.deleteByUserIdAndStockCode(userId, stockCode);
        return ResponseEntity.noContent().build();
    }
}