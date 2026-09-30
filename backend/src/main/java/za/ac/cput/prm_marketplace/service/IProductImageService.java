package za.ac.cput.prm_marketplace.service;

import za.ac.cput.prm_marketplace.domain.ProductImage;

import java.util.List;
import java.util.UUID;

public interface IProductImageService {

    ProductImage create(ProductImage image);

    ProductImage read(UUID id);

    ProductImage update(ProductImage image);

    boolean delete(UUID id);

    List<ProductImage> getAll();

    List<ProductImage> getByProduct(UUID productId);

    ProductImage getPrimary(UUID productId);

    boolean deleteByProduct(UUID productId);
}