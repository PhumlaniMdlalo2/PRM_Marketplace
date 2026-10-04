package za.ac.cput.prm_marketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductCondition;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;
import za.ac.cput.prm_marketplace.dto.ProductSearchCriteria;
import za.ac.cput.prm_marketplace.factory.ProductFactory;
import za.ac.cput.prm_marketplace.repository.ProductRepository;
import za.ac.cput.prm_marketplace.repository.VendorProfileRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VendorProfileRepository vendorProfileRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private UUID vendorId;
    private UUID otherVendorId;
    private UUID sellerId;
    private UUID id;
    private VendorProfile sellerProfile;
    private Product laptop;
    private Product ownedLaptop;
    private Product phone;

    @BeforeEach
    void setUp() {
        vendorId = UUID.randomUUID();
        otherVendorId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        id = UUID.randomUUID();

        sellerProfile = new VendorProfile.Builder()
                .setId(vendorId)
                .setBusinessName("Phumlani Devices")
                .build();

        laptop = ProductFactory.createProduct("Laptop", "15 inch",
                new BigDecimal("9999.99"), "Electronics", 7, vendorId);
        phone = ProductFactory.createProduct("Phone", "128GB",
                new BigDecimal("3999.00"), "Electronics", 10, vendorId);

        ownedLaptop = Product.builder()
                .copy(laptop)
                .id(id)
                .vendor(sellerProfile)
                .active(true)
                .build();
    }

    private void givenSellerHasProfile() {
        when(vendorProfileRepository.findByUserId(sellerId)).thenReturn(Optional.of(sellerProfile));
    }

    // create

    @Test
    @DisplayName("create puts the listing under the caller's own vendor profile")
    void create_usesTheCallersVendorProfile() {
        givenSellerHasProfile();
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        Product result = productService.create(laptop, sellerId);

        assertSame(sellerProfile, result.getVendor());
    }

    @Test
    @DisplayName("create ignores a vendor supplied in the body")
    void create_bodyVendorIsIgnored() {
        givenSellerHasProfile();
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        Product submitted = Product.builder()
                .copy(laptop)
                .vendor(new VendorProfile.Builder().setId(otherVendorId).build())
                .build();

        assertSame(sellerProfile, productService.create(submitted, sellerId).getVendor());
    }

    @Test
    @DisplayName("create refuses to list anything for a caller with no vendor profile")
    void create_callerHasNoProfile_returnsNullAndDoesNotSave() {
        when(vendorProfileRepository.findByUserId(sellerId)).thenReturn(Optional.empty());

        assertNull(productService.create(laptop, sellerId));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("create starts the listing active")
    void create_startsActive() {
        givenSellerHasProfile();
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        Product submitted = Product.builder().copy(laptop).active(false).build();

        assertTrue(productService.create(submitted, sellerId).isActive());
    }

    @Test
    @DisplayName("create applies the factory's validation instead of writing a bad listing")
    void create_invalidDetails_returnsNullAndDoesNotSave() {
        givenSellerHasProfile();

        Product noName = Product.builder().copy(laptop).name("  ").build();
        Product freePrice = Product.builder().copy(laptop).price(BigDecimal.ZERO).build();
        Product noCategory = Product.builder().copy(laptop).category(null).build();
        Product negativeStock = Product.builder().copy(laptop).stockQuantity(-1).build();

        assertNull(productService.create(noName, sellerId));
        assertNull(productService.create(freePrice, sellerId));
        assertNull(productService.create(noCategory, sellerId));
        assertNull(productService.create(negativeStock, sellerId));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("create with a null product or caller returns null instead of throwing")
    void create_nullArguments_returnNull() {
        assertNull(productService.create(null, sellerId));
        assertNull(productService.create(laptop, null));
    }

    @Test
    @DisplayName("create does not cascade images supplied in the body")
    void create_doesNotAcceptImagesFromTheBody() {
        givenSellerHasProfile();
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        Product result = productService.create(laptop, sellerId);

        assertTrue(result.getImages().isEmpty());
    }

    // read

    @Test
    void read_existingId_returnsProduct() {
        when(productRepository.findById(id)).thenReturn(Optional.of(ownedLaptop));

        assertSame(ownedLaptop, productService.read(id));
    }

    @Test
    void read_missingId_returnsNull() {
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertNull(productService.read(id));
    }

    @Test
    @DisplayName("read with a null id returns null instead of throwing")
    void read_nullId_returnsNull() {
        assertNull(productService.read(null));
    }

    // update

    @Test
    @DisplayName("update edits a listing the caller owns")
    void update_ownProduct_appliesTheEdit() {
        when(productRepository.findByIdAndVendorUserId(id, sellerId))
                .thenReturn(Optional.of(ownedLaptop));
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        Product submitted = Product.builder().copy(ownedLaptop).name("Laptop Pro").build();

        Product result = productService.update(id, submitted, sellerId);

        assertEquals("Laptop Pro", result.getName());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("update refuses a listing belonging to another vendor")
    void update_somebodyElsesProduct_returnsNullAndDoesNotSave() {
        when(productRepository.findByIdAndVendorUserId(id, sellerId)).thenReturn(Optional.empty());

        assertNull(productService.update(id, ownedLaptop, sellerId));

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("update keeps the stored vendor and creation time, so a listing cannot be sold on")
    void update_keepsStoredOwnership() {
        when(productRepository.findByIdAndVendorUserId(id, sellerId))
                .thenReturn(Optional.of(ownedLaptop));
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        Product hostile = Product.builder()
                .copy(ownedLaptop)
                .vendor(new VendorProfile.Builder().setId(otherVendorId).build())
                .createdAt(java.time.LocalDateTime.now().minusYears(1))
                .build();

        Product result = productService.update(id, hostile, sellerId);

        assertSame(sellerProfile, result.getVendor());
        assertEquals(ownedLaptop.getCreatedAt(), result.getCreatedAt());
    }

    @Test
    @DisplayName("update rejects a price of zero rather than storing it")
    void update_rejectsNonPositivePrice() {
        when(productRepository.findByIdAndVendorUserId(id, sellerId))
                .thenReturn(Optional.of(ownedLaptop));
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        Product hostile = Product.builder().copy(ownedLaptop).price(BigDecimal.ZERO).build();

        assertEquals(0, ownedLaptop.getPrice().compareTo(new BigDecimal("9999.99")));
        assertEquals(0, productService.update(id, hostile, sellerId).getPrice()
                .compareTo(new BigDecimal("9999.99")));
    }

    @Test
    @DisplayName("update rejects negative stock rather than storing it")
    void update_rejectsNegativeStock() {
        when(productRepository.findByIdAndVendorUserId(id, sellerId))
                .thenReturn(Optional.of(ownedLaptop));
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        Product hostile = Product.builder().copy(ownedLaptop).stockQuantity(-5).build();

        assertEquals(7, productService.update(id, hostile, sellerId).getStockQuantity());
    }

    @Test
    @DisplayName("update is how a seller retires a listing")
    void update_retiresTheListing() {
        when(productRepository.findByIdAndVendorUserId(id, sellerId))
                .thenReturn(Optional.of(ownedLaptop));
        when(productRepository.save(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        Product retired = Product.builder().copy(ownedLaptop).active(false).build();

        assertFalse(productService.update(id, retired, sellerId).isActive());
    }

    @Test
    @DisplayName("update with null arguments returns null instead of throwing")
    void update_nullArguments_returnNull() {
        assertNull(productService.update(null, ownedLaptop, sellerId));
        assertNull(productService.update(id, null, sellerId));
        assertNull(productService.update(id, ownedLaptop, null));
    }

    // getMine

    @Test
    void getMine_returnsTheCallersListings() {
        when(productRepository.findByVendorUserIdOrderByCreatedAtDesc(sellerId))
                .thenReturn(List.of(laptop, phone));

        List<Product> result = productService.getMine(sellerId);

        assertEquals(2, result.size());
        verify(productRepository).findByVendorUserIdOrderByCreatedAtDesc(sellerId);
    }

    @Test
    @DisplayName("getMine with a null caller returns empty rather than querying everything")
    void getMine_nullCaller_returnsEmpty() {
        assertTrue(productService.getMine(null).isEmpty());

        verify(productRepository, never())
                .findByVendorUserIdOrderByCreatedAtDesc(any(UUID.class));
    }

    // getAll

    @Test
    void getAll_returnsEveryProduct() {
        when(productRepository.findAll()).thenReturn(List.of(laptop, phone));

        List<Product> result = productService.getAll();

        assertEquals(2, result.size());
        assertTrue(result.contains(laptop));
        assertTrue(result.contains(phone));
    }

    @Test
    void getAll_noProducts_returnsEmptyList() {
        when(productRepository.findAll()).thenReturn(List.of());

        assertTrue(productService.getAll().isEmpty());
    }

    // getByVendor / getByCategory

    @Test
    void getByVendor_returnsVendorsProducts() {
        when(productRepository.findByVendorId(vendorId)).thenReturn(List.of(laptop, phone));

        List<Product> result = productService.getByVendor(vendorId);

        assertEquals(2, result.size());
        verify(productRepository).findByVendorId(vendorId);
    }

    @Test
    @DisplayName("getByVendor with a null vendor returns empty rather than matching every row")
    void getByVendor_nullVendor_returnsEmpty() {
        assertTrue(productService.getByVendor(null).isEmpty());
    }

    @Test
    void getByCategory_returnsMatchingProducts() {
        when(productRepository.findByCategory("Electronics")).thenReturn(List.of(laptop, phone));

        List<Product> result = productService.getByCategory("Electronics");

        assertEquals(2, result.size());
        verify(productRepository).findByCategory("Electronics");
    }

    @Test
    @DisplayName("getByCategory with a null category returns empty")
    void getByCategory_nullCategory_returnsEmpty() {
        assertTrue(productService.getByCategory(null).isEmpty());
    }

    // search

    private ProductSearchCriteria criteria(int page, int size, String sortBy, String direction) {
        return new ProductSearchCriteria(null, null, null, null, null, null, null,
                page, size, sortBy, direction);
    }

    @Test
    void search_defaultsToNewestFirst() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ownedLaptop)));

        productService.search(criteria(0, 20, "createdAt", "desc"));

        Pageable pageable = capturedPageable();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void search_blankSortFallsBackToNewestFirst() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ownedLaptop)));

        productService.search(criteria(0, 20, "  ", "asc"));

        Pageable pageable = capturedPageable();
        assertEquals(Sort.Direction.ASC, pageable.getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void search_honoursRequestedPageAndSize() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<Product>(List.of(), PageRequest.of(3, 5), 0));

        productService.search(criteria(3, 5, "name", "asc"));

        Pageable pageable = capturedPageable();
        assertEquals(3, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals("name", pageable.getSort().getOrderFor("name").getProperty());
    }

    @Test
    void search_sortKeyIsCaseInsensitive() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ownedLaptop)));

        productService.search(criteria(0, 20, "Price", "asc"));

        assertEquals("price", capturedPageable().getSort().getOrderFor("price").getProperty());
    }

    @Test
    void search_rejectsUnknownSortFieldInsteadOfFailing() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.search(criteria(0, 20, "passwordHash", "asc")));

        verify(productRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_unparseableDirectionFallsBackToDescending() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ownedLaptop)));

        productService.search(criteria(0, 20, "name", "sideways"));

        assertEquals(Sort.Direction.DESC,
                capturedPageable().getSort().getOrderFor("name").getDirection());
    }

    @Test
    void search_returnsPageContents() {
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(laptop, phone)));

        Page<Product> page = productService.search(criteria(0, 20, "createdAt", "desc"));

        assertEquals(2, page.getContent().size());
    }

    @Test
    void search_rejectsNegativePage() {
        assertThrows(IllegalArgumentException.class, () -> criteria(-1, 20, "name", "asc"));
    }

    @Test
    void search_rejectsZeroSize() {
        assertThrows(IllegalArgumentException.class, () -> criteria(0, 0, "name", "asc"));
    }

    @Test
    void search_rejectsOversizedPage() {
        assertThrows(IllegalArgumentException.class, () -> criteria(0, 500, "name", "asc"));
    }

    @Test
    void search_rejectsInvertedPriceRange() {
        assertThrows(IllegalArgumentException.class, () -> new ProductSearchCriteria(
                null, null, null, new BigDecimal("100"), new BigDecimal("10"),
                null, null, 0, 20, "name", "asc"));
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), captor.capture());
        return captor.getValue();
    }
}