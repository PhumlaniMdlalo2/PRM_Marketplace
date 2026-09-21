package za.ac.cput.prm_marketplace.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.repository.ProductRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ProductServiceImpl implements IProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Product create(Product product) {
        return productRepository.save(product);
    }

    @Override
    public Product read(UUID id) {
        return productRepository.findById(id).orElse(null);
    }

    @Override
    public Product update(Product product) {
        if (productRepository.existsById(product.getId())) {
            return productRepository.save(product);
        }
        return null;
    }

    @Override
    public boolean delete(UUID id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @Override
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    @Override
    public List<Product> getByVendor(UUID vendorId) {
        return productRepository.findByVendorId(vendorId);
    }

    @Override
    public List<Product> getByCategory(String category) {
        return productRepository.findByCategory(category);
    }
}
