package za.ac.cput.prm_marketplace.service;

import org.springframework.stereotype.Service;
import za.ac.cput.prm_marketplace.domain.ProductImage;
import za.ac.cput.prm_marketplace.repository.ProductImageRepository;

import java.util.List;
import java.util.UUID;

@Service
public class ProductImageServiceImpl implements IProductImageService {

    private final ProductImageRepository productImageRepository;

    public ProductImageServiceImpl(ProductImageRepository productImageRepository) {
        this.productImageRepository = productImageRepository;
    }

    @Override
    public ProductImage create(ProductImage image) {
        if (image == null) {
            return null;
        }
        return productImageRepository.save(image);
    }

    @Override
    public ProductImage read(UUID id) {
        if (id == null) {
            return null;
        }
        return productImageRepository.findById(id).orElse(null);
    }

    @Override
    public ProductImage update(ProductImage image) {
        if (image == null || image.getId() == null || !productImageRepository.existsById(image.getId())) {
            return null;
        }
        return productImageRepository.save(image);
    }

    @Override
    public boolean delete(UUID id) {
        if (id == null || !productImageRepository.existsById(id)) {
            return false;
        }
        productImageRepository.deleteById(id);
        return true;
    }

    @Override
    public List<ProductImage> getAll() {
        return productImageRepository.findAll();
    }

    @Override
    public List<ProductImage> getByProduct(UUID productId) {
        if (productId == null) {
            return List.of();
        }
        return productImageRepository.findByProductIdOrderBySortOrderAsc(productId);
    }

    @Override
    public ProductImage getPrimary(UUID productId) {
        if (productId == null) {
            return null;
        }
        return productImageRepository.findByProductIdAndPrimaryTrue(productId).orElse(null);
    }

    @Override
    public boolean deleteByProduct(UUID productId) {
        if (productId == null) {
            return false;
        }
        productImageRepository.deleteByProductId(productId);
        return true;
    }
}