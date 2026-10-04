package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductImage;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.repository.ProductImageRepository;
import za.ac.cput.prm_marketplace.repository.ProductRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceImplTest {

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductImageServiceImpl productImageService;

    private UUID sellerId;
    private Product product;
    private UUID productId;

    @BeforeEach
    void setUp() {
        sellerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        product = new Product.Builder()
                .id(productId)
                .name("Textbook")
                .vendor(new VendorProfile.Builder()
                        .setId(UUID.randomUUID())
                        .setUser(new za.ac.cput.prm_marketplace.domain.User.Builder()
                                .setId(sellerId)
                                .build())
                        .build())
                .build();
    }

    private ProductImage submitted(boolean primary, int sortOrder) {
        return new ProductImage.Builder()
                .setImageUrl("https://cdn.example.com/" + sortOrder + ".jpg")
                .setPrimary(primary)
                .setSortOrder(sortOrder)
                .build();
    }

    private ProductImage stored(UUID id, Product owner, boolean primary, int sortOrder) {
        return new ProductImage.Builder()
                .setId(id)
                .setProduct(owner)
                .setImageUrl("https://cdn.example.com/" + sortOrder + ".jpg")
                .setPrimary(primary)
                .setSortOrder(sortOrder)
                .build();
    }

    // create

    @Test
    @DisplayName("create attaches the image to the product named in the path")
    void create_attachesToThePathProduct() {
        when(productRepository.findByIdAndVendorUserId(productId, sellerId)).thenReturn(Optional.of(product));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(call -> call.getArgument(0));

        ProductImage result = productImageService.create(productId, submitted(false, 0), sellerId);

        assertThat(result).isNotNull();
        assertThat(result.getProduct()).isSameAs(product);
    }

    @Test
    @DisplayName("create ignores a product supplied in the body")
    void create_bodyProductIsIgnored() {
        when(productRepository.findByIdAndVendorUserId(productId, sellerId)).thenReturn(Optional.of(product));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(call -> call.getArgument(0));

        Product impostor = new Product.Builder().id(UUID.randomUUID()).name("Competitor").build();
        ProductImage hostile = new ProductImage.Builder()
                .setProduct(impostor)
                .setImageUrl("https://cdn.example.com/x.jpg")
                .build();

        assertThat(productImageService.create(productId, hostile, sellerId).getProduct())
                .isSameAs(product);
    }

    @Test
    @DisplayName("create refuses to attach an image to a product the caller does not own")
    void create_somebodyElsesProduct_returnsNull() {
        when(productRepository.findByIdAndVendorUserId(productId, sellerId)).thenReturn(Optional.empty());

        assertThat(productImageService.create(productId, submitted(false, 0), sellerId)).isNull();

        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    @DisplayName("create refuses a blank image URL")
    void create_blankUrl_returnsNull() {
        ProductImage blank = new ProductImage.Builder().setImageUrl("   ").build();

        assertThat(productImageService.create(productId, blank, sellerId)).isNull();

        verify(productImageRepository, never()).save(any(ProductImage.class));
        verify(productRepository, never()).findByIdAndVendorUserId(any(), any());
    }

    @Test
    @DisplayName("create with null arguments returns null instead of throwing")
    void create_nullArguments_returnNull() {
        assertThat(productImageService.create(null, submitted(false, 0), sellerId)).isNull();
        assertThat(productImageService.create(productId, null, sellerId)).isNull();
        assertThat(productImageService.create(productId, submitted(false, 0), null)).isNull();
    }

    @Test
    @DisplayName("a new primary image demotes the previous one, so a product has only one")
    void create_primaryDemotesTheOldPrimary() {
        when(productRepository.findByIdAndVendorUserId(productId, sellerId)).thenReturn(Optional.of(product));
        ProductImage oldPrimary = stored(UUID.randomUUID(), product, true, 0);
        when(productImageRepository.findByProductId(productId)).thenReturn(List.of(oldPrimary));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(call -> call.getArgument(0));

        productImageService.create(productId, submitted(true, 1), sellerId);

        assertThat(oldPrimary.isPrimary()).isFalse();
        verify(productImageRepository, atLeast(2)).save(any(ProductImage.class));
    }

    @Test
    @DisplayName("a non-primary image leaves the existing primary alone")
    void create_nonPrimaryDoesNotDisturbThePrimary() {
        when(productRepository.findByIdAndVendorUserId(productId, sellerId)).thenReturn(Optional.of(product));
        ProductImage oldPrimary = stored(UUID.randomUUID(), product, true, 0);
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(call -> call.getArgument(0));

        productImageService.create(productId, submitted(false, 1), sellerId);

        // The existing primary is never loaded, so it cannot have been touched.
        assertThat(oldPrimary.isPrimary()).isTrue();
        verify(productImageRepository, never()).findByProductId(any());
    }

    // read

    @Test
    @DisplayName("read returns the stored image")
    void read_existing_returnsImage() {
        ProductImage image = stored(UUID.randomUUID(), product, false, 0);
        when(productImageRepository.findById(image.getId())).thenReturn(Optional.of(image));

        assertThat(productImageService.read(image.getId())).isSameAs(image);
    }

    @Test
    @DisplayName("read missing image returns null")
    void read_missing_returnsNull() {
        UUID id = UUID.randomUUID();
        when(productImageRepository.findById(id)).thenReturn(Optional.empty());

        assertThat(productImageService.read(id)).isNull();
    }

    @Test
    @DisplayName("read with a null id returns null instead of throwing")
    void read_null_returnsNull() {
        assertThat(productImageService.read(null)).isNull();
    }

    // update

    @Test
    @DisplayName("update edits an image on the caller's own product")
    void update_ownImage_appliesTheEdit() {
        UUID imageId = UUID.randomUUID();
        ProductImage existing = stored(imageId, product, false, 0);
        when(productImageRepository.findByIdAndProductVendorUserId(imageId, sellerId))
                .thenReturn(Optional.of(existing));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(call -> call.getArgument(0));

        ProductImage edited = new ProductImage.Builder()
                .setImageUrl("https://cdn.example.com/new.jpg")
                .setSortOrder(3)
                .build();

        ProductImage result = productImageService.update(imageId, edited, sellerId);

        assertThat(result.getImageUrl()).isEqualTo("https://cdn.example.com/new.jpg");
        assertThat(result.getSortOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("update refuses an image on somebody else's product")
    void update_somebodyElsesImage_returnsNullAndDoesNotSave() {
        UUID imageId = UUID.randomUUID();
        when(productImageRepository.findByIdAndProductVendorUserId(imageId, sellerId))
                .thenReturn(Optional.empty());

        assertThat(productImageService.update(imageId, submitted(false, 0), sellerId)).isNull();

        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    @DisplayName("update cannot move an image onto a different product")
    void update_cannotRepointTheProduct() {
        UUID imageId = UUID.randomUUID();
        ProductImage existing = stored(imageId, product, false, 0);
        when(productImageRepository.findByIdAndProductVendorUserId(imageId, sellerId))
                .thenReturn(Optional.of(existing));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(call -> call.getArgument(0));

        Product impostor = new Product.Builder().id(UUID.randomUUID()).name("Competitor").build();
        ProductImage hostile = new ProductImage.Builder()
                .setProduct(impostor)
                .setImageUrl("https://cdn.example.com/new.jpg")
                .build();

        assertThat(productImageService.update(imageId, hostile, sellerId).getProduct())
                .isSameAs(product);
    }

    @Test
    @DisplayName("promoting an existing image to primary demotes the previous one")
    void update_promotingToPrimaryDemotesTheOldPrimary() {
        UUID imageId = UUID.randomUUID();
        ProductImage existing = stored(imageId, product, false, 0);
        ProductImage oldPrimary = stored(UUID.randomUUID(), product, true, 1);
        when(productImageRepository.findByIdAndProductVendorUserId(imageId, sellerId))
                .thenReturn(Optional.of(existing));
        when(productImageRepository.findByProductId(productId)).thenReturn(List.of(oldPrimary));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(call -> call.getArgument(0));

        ProductImage promote = new ProductImage.Builder()
                .setImageUrl(existing.getImageUrl())
                .setPrimary(true)
                .build();

        assertThat(productImageService.update(imageId, promote, sellerId).isPrimary()).isTrue();
        assertThat(oldPrimary.isPrimary()).isFalse();
    }

    @Test
    @DisplayName("update with null arguments returns null instead of throwing")
    void update_nullArguments_returnNull() {
        assertThat(productImageService.update(null, submitted(false, 0), sellerId)).isNull();
        assertThat(productImageService.update(UUID.randomUUID(), null, sellerId)).isNull();
        assertThat(productImageService.update(UUID.randomUUID(), submitted(false, 0), null)).isNull();
    }

    // delete

    @Test
    @DisplayName("delete removes the caller's own image")
    void delete_ownImage_deletes() {
        UUID imageId = UUID.randomUUID();
        ProductImage existing = stored(imageId, product, false, 0);
        when(productImageRepository.findByIdAndProductVendorUserId(imageId, sellerId))
                .thenReturn(Optional.of(existing));

        assertThat(productImageService.delete(imageId, sellerId)).isTrue();

        verify(productImageRepository).delete(existing);
    }

    @Test
    @DisplayName("delete refuses an image on somebody else's product")
    void delete_somebodyElsesImage_returnsFalse() {
        UUID imageId = UUID.randomUUID();
        when(productImageRepository.findByIdAndProductVendorUserId(imageId, sellerId))
                .thenReturn(Optional.empty());

        assertThat(productImageService.delete(imageId, sellerId)).isFalse();

        verify(productImageRepository, never()).delete(any(ProductImage.class));
    }

    @Test
    @DisplayName("delete with null arguments returns false instead of throwing")
    void delete_nullArguments_returnFalse() {
        assertThat(productImageService.delete(null, sellerId)).isFalse();
        assertThat(productImageService.delete(UUID.randomUUID(), null)).isFalse();
    }

    // getByProduct / getPrimary

    @Test
    @DisplayName("getByProduct returns images in display order")
    void getByProduct_delegates() {
        List<ProductImage> images = List.of(stored(UUID.randomUUID(), product, true, 0));
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
        ProductImage primary = stored(UUID.randomUUID(), product, true, 0);
        when(productImageRepository.findByProductIdAndPrimaryTrue(productId))
                .thenReturn(Optional.of(primary));

        assertThat(productImageService.getPrimary(productId)).isSameAs(primary);
    }

    @Test
    @DisplayName("getPrimary returns null when a product has no primary image")
    void getPrimary_missing_returnsNull() {
        when(productImageRepository.findByProductIdAndPrimaryTrue(productId)).thenReturn(Optional.empty());

        assertThat(productImageService.getPrimary(productId)).isNull();
    }

    @Test
    @DisplayName("getPrimary with null id returns null")
    void getPrimary_withNull_returnsNull() {
        assertThat(productImageService.getPrimary(null)).isNull();
    }

    // deleteByProduct

    @Test
    @DisplayName("deleteByProduct clears the gallery on the caller's own product")
    void deleteByProduct_ownProduct_clearsAll() {
        when(productImageRepository.existsByProductIdAndProductVendorUserId(productId, sellerId))
                .thenReturn(true);

        assertThat(productImageService.deleteByProduct(productId, sellerId)).isTrue();

        verify(productImageRepository).deleteByProductId(productId);
    }

    @Test
    @DisplayName("deleteByProduct refuses a product the caller does not own")
    void deleteByProduct_somebodyElsesProduct_returnsFalse() {
        when(productImageRepository.existsByProductIdAndProductVendorUserId(productId, sellerId))
                .thenReturn(false);

        assertThat(productImageService.deleteByProduct(productId, sellerId)).isFalse();

        verify(productImageRepository, never()).deleteByProductId(any());
    }

    @Test
    @DisplayName("deleteByProduct with null arguments is a no-op")
    void deleteByProduct_withNull_returnsFalse() {
        assertThat(productImageService.deleteByProduct(null, sellerId)).isFalse();
        assertThat(productImageService.deleteByProduct(productId, null)).isFalse();

        verify(productImageRepository, never()).deleteByProductId(any());
    }
}