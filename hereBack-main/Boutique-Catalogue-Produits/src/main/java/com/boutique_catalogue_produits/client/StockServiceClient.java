package com.boutique_catalogue_produits.client;


import com.boutique_catalogue_produits.dto.StockDTO;
import com.boutique_catalogue_produits.dto.StockTransactionDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FeignClient(name = "GESTION-STOCKS", fallback = StockServiceClientFallback.class)
public interface StockServiceClient {
    @GetMapping("/api/stocks/{stockId}/vendeur")
    String getVendeurIdByStockId(@PathVariable("stockId") Long stockId);
    @GetMapping("/api/stocks/{id}")
    @CircuitBreaker(name = "stockService", fallbackMethod = "getStockByIdFallback")
    StockDTO getStockById(@PathVariable("id") Long id);
    @PostMapping("/api/stock-notifications/stock-critique")
    void envoyerNotificationStockCritique(@RequestBody Map<String, Object> notificationData);
    @GetMapping("/api/stocks/boutique/{idBoutique}")
    @CircuitBreaker(name = "stockService", fallbackMethod = "getStocksByBoutiqueIdFallback")
    List<StockDTO> getStocksByBoutiqueId(@PathVariable("idBoutique") Integer idBoutique);
    @PostMapping("/api/stock-notifications/expiration")
    void envoyerNotificationExpiration(@RequestBody Map<String, Object> notificationData);
    @GetMapping("/api/stocks/{stockId}/boutique")
    @CircuitBreaker(name = "stockService", fallbackMethod = "getBoutiqueIdByStockIdFallback")
    Long getBoutiqueIdByStockId(@PathVariable("stockId") Long stockId);

    default Long getBoutiqueIdByStockIdFallback(Long stockId, Exception e) {
        return null;
    }
    @PostMapping("/api/stock-transactions")
    @CircuitBreaker(name = "stockService", fallbackMethod = "createTransactionFallback")
    StockTransactionDTO createTransaction(@RequestBody Map<String, Object> transaction);

    default StockDTO getStockByIdFallback(Long id, Exception e) {
        return null;
    }

    default List<StockDTO> getStocksByBoutiqueIdFallback(Integer idBoutique, Exception e) {
        return List.of();
    }

    default StockTransactionDTO createTransactionFallback(Map<String, Object> transaction, Exception e) {
        return null;

}
}
