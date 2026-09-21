package za.ac.cput.prm_marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.repository.ProductRepository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByVendorId(UUID vendorId);

    List<Product> findByCategory(String category);
}
