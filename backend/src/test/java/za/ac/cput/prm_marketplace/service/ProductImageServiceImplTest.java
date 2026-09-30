package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductImage;
import za.ac.cput.prm_marketplace.repository.ProductImageRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceImplTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @InjectMocks
    private ProductImageServiceImpl productImageService;

    private Product buildProduct() {
        return new Product.Builder()
                .id(UUID.randomUUID())
                .name("Textbook")
                .build();
    }

    private ProductImage buildImage(Product product, boolean primary, int sortOrder) {
        return new ProductImage.Builder()
                .setId(UUID.randomUUID())
                .setProduct(product)
                .setImageUrl("https://cdn.example.com/" + sortOrder + ".jpg")
                .setPrimary(primary)
                .setSortOrder(sortOrder)
                .build();
    }

    @Test
    @DisplayName("create persists the image")
    void create_saves() {
        ProductImage image = buildImage(buildProduct(), false, 0);
        when(productImageRepository.save(image)).thenReturn(image);

        assertThat(productImageService.create(image)).isSameAs(image);
    }

    @Test
    @DisplayName("read missing image returns null")
    void read_missing_returnsNull() {
        UUID id = UUID.randomUUID();
        when(productImageRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(productImageService.read(id)).isNull();
    }

    @Test
    @DisplayName("update requires an existing image")
    void update_missing_returnsNull() {
        ProductImage image = buildImage(buildProduct(), false, 0);
        when(productImageRepository.existsById(image.getId())).thenReturn(false);

        assertThat(productImageService.update(image)).isNull();
        verify(productImageRepository, never()).save(any());
    }

    @Test
    @DisplayName("update saves an existing image")
    void update_existing_saves() {
        ProductImage image = buildImage(buildProduct(), false, 0);
        when(productImageRepository.existsById(image.getId())).thenReturn(true);
        when(productImageRepository.save(image)).thenReturn(image);

        assertThat(productImageService.update(image)).isSameAs(image);
    }

    @Test
    @DisplayName("delete reports false for an unknown image")
    void delete_missing_returnsFalse() {
        UUID id = UUID.randomUUID();
        when(productImageRepository.existsById(id)).thenReturn(false);

        assertThat(productImageService.delete(id)).isFalse();
    }

    @Test
    @DisplayName("getByProduct returns images in display order")
    void getByProduct_delegates() {
        UUID productId = UUID.randomUUID();
        List<ProductImage> images = List.of(buildImage(buildProduct(), true, 0));
        when(productImageRepository.findByProductIdOrderBySortOrderAsc(productId)).thenReturn(images);

        assertThat(productImageService.getByProduct(productId)).isEqualTo(images);
    }

    @Test
    @DisplayName("getByProduct with null id returns empty")
    void getByProduct_withNull_returnsEmpty() {
        assertThat(productImageService.getByProduct(null)).isEmpty();
    }

    @Test
    @DisplayName("getPrimary returns the flagged image")
    void getPrimary_returnsPrimary() {
        UUID productId = UUID.randomUUID();
        ProductImage primary = buildImage(buildProduct(), true, 0);
        when(productImageRepository.findByProductIdAndPrimaryTrue(productId))
                .thenReturn(Optional.of(primary));

        assertThat(productImageService.getPrimary(productId)).isSameAs(primary);
    }

    @Test
    @DisplayName("getPrimary returns null when a product has no primary image")
    void getPrimary_missing_returnsNull() {
        UUID productId = UUID.randomUUID();
        when(productImageRepository.findByProductIdAndPrimaryTrue(productId)).thenReturn(Optional.empty());

        assertThat(productImageService.getPrimary(productId)).isNull();
    }

    @Test
    @DisplayName("getPrimary with null id returns null")
    void getPrimary_withNull_returnsNull() {
        assertThat(productImageService.getPrimary(null)).isNull();
    }

    @Test
    @DisplayName("deleteByProduct clears every image of a product")
    void deleteByProduct_clearsAll() {
        UUID productId = UUID.randomUUID();

        assertThat(productImageService.deleteByProduct(productId)).isTrue();
        verify(productImageRepository).deleteByProductId(productId);
    }

    @Test
    @DisplayName("deleteByProduct with null id is a no-op")
    void deleteByProduct_withNull_returnsFalse() {
        assertThat(productImageService.deleteByProduct(null)).isFalse();
        verify(productImageRepository, never()).deleteByProductId(any());
    }
}
