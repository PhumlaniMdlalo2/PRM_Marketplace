package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.Product;
import java.util.List;
import java.util.UUID;

public interface IProductService {
    Product create(Product product);
    Product read(UUID id);
    Product update(Product product);
    boolean delete(UUID id);
    List<Product> getAll();
    List<Product> getByVendor(UUID vendorId);
    List<Product> getByCategory(String category);
}
