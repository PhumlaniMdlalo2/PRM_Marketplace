package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProductImageTest {

    private VendorProfile buildVendor() {
        return new VendorProfile.Builder()
                .setId(UUID.randomUUID())
                .setUser(new User.Builder()
                        .setId(UUID.randomUUID())
                        .setName("Vendor Owner")
                        .setEmail("vendor@example.com")
                        .setPasswordHash("hash")
                        .setRole(Role.VENDOR)
                        .build())
                .setBusinessName("Campus Books")
                .build();
    }

    private Product buildProduct() {
        return new Product.Builder()
                .id(UUID.randomUUID())
                .name("Algebra Textbook")
                .description("Second edition")
                .price(new BigDecimal("120.00"))
                .category("Books")
                .stockQuantity(3)
                .vendor(buildVendor())
                .build();
    }

    private ProductImage buildImage(Product product) {
        return new ProductImage.Builder()
                .setId(UUID.randomUUID())
                .setProduct(product)
                .setImageUrl("https://example.com/textbook.jpg")
                .setSortOrder(1)
                .setPrimary(true)
                .build();
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        Product product = buildProduct();

        ProductImage image = new ProductImage.Builder()
                .setId(id)
                .setProduct(product)
                .setImageUrl("https://example.com/textbook.jpg")
                .setSortOrder(2)
                .setPrimary(true)
                .build();

        assertThat(image.getId()).isEqualTo(id);
        assertThat(image.getProduct()).isEqualTo(product);
        assertThat(image.getImageUrl()).isEqualTo("https://example.com/textbook.jpg");
        assertThat(image.getSortOrder()).isEqualTo(2);
        assertThat(image.isPrimary()).isTrue();
    }

    @Test
    void copyPreservesEveryField() {
        ProductImage original = buildImage(buildProduct());

        ProductImage copy = new ProductImage.Builder().copy(original).build();

        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getProduct()).isEqualTo(original.getProduct());
        assertThat(copy.getImageUrl()).isEqualTo(original.getImageUrl());
        assertThat(copy.getSortOrder()).isEqualTo(original.getSortOrder());
        assertThat(copy.isPrimary()).isEqualTo(original.isPrimary());
    }

    @Test
    void setProductReplacesTheBackReference() {
        ProductImage image = buildImage(buildProduct());
        Product replacement = buildProduct();

        image.setProduct(replacement);

        assertThat(image.getProduct()).isEqualTo(replacement);
    }

    @Test
    void sortOrderDefaultsToZeroAndPrimaryToFalse() {
        ProductImage image = new ProductImage.Builder()
                .setProduct(buildProduct())
                .setImageUrl("https://example.com/plain.jpg")
                .build();

        assertThat(image.getSortOrder()).isZero();
        assertThat(image.isPrimary()).isFalse();
    }

    @Test
    void equalityIsByIdAndDoesNotApplyBeforeOneExists() {
        UUID id = UUID.randomUUID();
        ProductImage first = new ProductImage.Builder().setId(id).setImageUrl("a.jpg").build();
        ProductImage second = new ProductImage.Builder().setId(id).setImageUrl("b.jpg").build();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);

        ProductImage transientOne = new ProductImage.Builder().setImageUrl("a.jpg").build();
        ProductImage otherTransient = new ProductImage.Builder().setImageUrl("b.jpg").build();

        assertThat(transientOne).isNotEqualTo(otherTransient);
        assertThat(transientOne).isEqualTo(transientOne);
    }
}