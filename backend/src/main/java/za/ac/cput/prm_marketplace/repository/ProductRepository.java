package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductCondition;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    List<Product> findByVendorId(UUID vendorId);

    List<Product> findByCategory(String category);

    List<Product> findByCityIgnoreCase(String city);

    List<Product> findByActiveTrue();

    List<Product> findByActiveTrueAndCategory(String category);

    List<Product> findByNameContainingIgnoreCase(String keyword);

    List<Product> findByCategoryAndCityIgnoreCase(String category, String city);

    List<Product> findByCategoryAndPriceBetween(String category, BigDecimal min, BigDecimal max);

    List<Product> findByCondition(ProductCondition condition);

    List<Product> findByStockQuantityLessThanEqual(int quantity);

    List<Product> findByVendorIdAndActiveTrue(UUID vendorId);

    /**
     * Loads a product only when the given account's vendor profile owns it.
     *
     * <p>This is the ownership check behind every write to a product. It reaches through product to
     * vendor to user, because a product stores no owner id of its own: the owner is whoever holds
     * the vendor profile the listing sits under.
     */
    Optional<Product> findByIdAndVendorUserId(UUID id, UUID userId);

    /** The caller's own listings, newest first. */
    List<Product> findByVendorUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Reserves stock in a single statement.
     *
     * <p>The {@code stockQuantity >= :quantity} predicate is the whole point: doing a read of the
     * stock level and then writing it back lets two concurrent checkouts both observe the last
     * item as available and both succeed, overselling it. Letting the database evaluate and
     * decrement under one statement makes the second caller match zero rows instead.
     *
     * @return the number of rows updated: 1 when the stock was reserved, 0 when the product is
     *         inactive or there was not enough stock left
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Product p
               set p.stockQuantity = p.stockQuantity - :quantity
             where p.id = :id
               and p.active = true
               and p.stockQuantity >= :quantity
            """)
    int decrementStock(@Param("id") UUID id, @Param("quantity") int quantity);

    /**
     * Puts reserved stock back when an order is cancelled. Never drives the column below zero.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Product p set p.stockQuantity = p.stockQuantity + :quantity where p.id = :id")
    int incrementStock(@Param("id") UUID id, @Param("quantity") int quantity);
}