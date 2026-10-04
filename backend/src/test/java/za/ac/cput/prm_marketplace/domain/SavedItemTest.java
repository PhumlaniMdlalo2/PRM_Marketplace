package za.ac.cput.prm_marketplace.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SavedItemTest {

    private User buildUser() {
        return new User.Builder()
                .setId(UUID.randomUUID())
                .setName("Jane Doe")
                .setEmail("jane@example.com")
                .setPasswordHash("hash")
                .setRole(Role.STUDENT)
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
                .vendor(new VendorProfile.Builder()
                        .setId(UUID.randomUUID())
                        .setUser(buildUser())
                        .setBusinessName("Campus Books")
                        .build())
                .build();
    }

    @Test
    void builderSetsAllFields() {
        UUID id = UUID.randomUUID();
        User user = buildUser();
        Product product = buildProduct();

        SavedItem saved = new SavedItem.Builder()
                .setId(id)
                .setUser(user)
                .setProduct(product)
                .build();

        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getProduct()).isEqualTo(product);
    }

    @Test
    void copyPreservesUserAndProduct() {
        SavedItem original = new SavedItem.Builder()
                .setId(UUID.randomUUID())
                .setUser(buildUser())
                .setProduct(buildProduct())
                .build();

        SavedItem copy = new SavedItem.Builder().copy(original).build();

        assertThat(copy.getId()).isEqualTo(original.getId());
        assertThat(copy.getUser()).isEqualTo(original.getUser());
        assertThat(copy.getProduct()).isEqualTo(original.getProduct());
        assertThat(copy.getSavedAt()).isEqualTo(original.getSavedAt());
    }

    @Test
    void equalityIsByIdAndDoesNotApplyBeforeOneExists() {
        UUID id = UUID.randomUUID();
        SavedItem first = new SavedItem.Builder().setId(id).setUser(buildUser()).build();
        SavedItem second = new SavedItem.Builder().setId(id).setUser(buildUser()).build();

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);

        SavedItem transientOne = new SavedItem.Builder().setUser(buildUser()).build();
        SavedItem otherTransient = new SavedItem.Builder().setUser(buildUser()).build();

        assertThat(transientOne).isNotEqualTo(otherTransient);
        assertThat(transientOne).isEqualTo(transientOne);
    }

    @Test
    void toStringReferencesTheOwnerWithoutLeakingNestedGraphs() {
        User user = buildUser();
        SavedItem saved = new SavedItem.Builder()
                .setId(UUID.randomUUID())
                .setUser(user)
                .setProduct(buildProduct())
                .build();

        assertThat(saved.toString())
                .contains(user.getId().toString())
                .doesNotContain("passwordHash");
    }
}