package com.gestionstocks.repository;

import com.gestionstocks.model.StockTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {
    Logger logger = LoggerFactory.getLogger(StockTransactionRepository.class);
    /**
     * Trouve toutes les transactions pour un produit spécifique
     */
    List<StockTransaction> findByProductId(Long productId);

    // Méthode optionnelle avec logging supplémentaire
    default List<StockTransaction> safelyFindByProductId(Long productId) {
        List<StockTransaction> transactions = findByProductId(productId);
        logger.info("Transactions trouvées pour le produit {}: {}", productId, transactions.size());
        return transactions;
    }
    /**
     * Trouve toutes les transactions pour un stock spécifique
     */
    List<StockTransaction> findByStockId(Long stockId);

    /**
     * Trouve toutes les transactions pour un produit dans un stock spécifique
     */
    List<StockTransaction> findByProductIdAndStockId(Long productId, Long stockId);

    /**
     * Trouve les transactions d'un type spécifique pour un stock
     */
    List<StockTransaction> findByStockIdAndType(Long stockId, StockTransaction.TransactionType type);

    /**
     * Trouve les transactions dans une période spécifique
     */
    List<StockTransaction> findByTransactionDateBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Calcule le revenu total pour un stock spécifique
     */
    @Query("SELECT SUM(t.revenuTotal) FROM StockTransaction t WHERE t.stockId = :stockId AND t.type = 'REMOVE'")
    BigDecimal calculateTotalRevenueForStock(@Param("stockId") Long stockId);

    /**
     * Calcule le revenu total pour un produit spécifique
     */
    @Query("SELECT SUM(t.revenuTotal) FROM StockTransaction t WHERE t.productId = :productId AND t.type = 'REMOVE'")
    BigDecimal calculateTotalRevenueForProduct(@Param("productId") Long productId);

    /**
     * Récupère le revenu quotidien pour un stock
     */
    @Query("SELECT FUNCTION('DATE', t.transactionDate) as date, SUM(t.revenuTotal) as revenue " +
            "FROM StockTransaction t " +
            "WHERE t.stockId = :stockId AND t.type = 'REMOVE' " +
            "GROUP BY FUNCTION('DATE', t.transactionDate)")
    List<Object[]> getDailyRevenueForStock(@Param("stockId") Long stockId);
}