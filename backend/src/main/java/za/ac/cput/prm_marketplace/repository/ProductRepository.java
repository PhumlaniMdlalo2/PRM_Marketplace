package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductCondition;

import java.math.BigDecimal;
import java.util.List;
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
}