package za.ac.cput.prm_marketplace.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import za.ac.cput.prm_marketplace.domain.Conversation;
import za.ac.cput.prm_marketplace.domain.Product;
import za.ac.cput.prm_marketplace.domain.ProductCondition;
import za.ac.cput.prm_marketplace.domain.Role;
import za.ac.cput.prm_marketplace.domain.User;
import za.ac.cput.prm_marketplace.domain.VendorProfile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the conversation lookups against a real database.
 *
 * <p>These two queries decide whether a message continues an existing thread or silently starts a
 * second one, so their predicates are load bearing: {@code AndProductIsNull} has to match the general
 * thread and must not match a thread about a listing. The mocked service test cannot tell them apart,
 * because a mock returns whatever the test told it to return.
 *
 * <p>The unread tally in {@link ConversationRepository#countUnreadConversations} is exercised in
 * {@code MessageRepositoryTest} and is not repeated here.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ConversationRepositoryTest {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VendorProfileRepository vendorProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private User jane;
    private User john;
    private User stranger;
    private Product textbook;
    private Conversation aboutListing;
    private Conversation general;

    @BeforeEach
    void setUp() {
        jane = userRepository.save(buildUser("jane", Role.STUDENT));
        john = userRepository.save(buildUser("john", Role.VENDOR));
        stranger = userRepository.save(buildUser("stranger", Role.STUDENT));
        textbook = saveProduct();

        aboutListing = conversationRepository.save(new Conversation.Builder()
                .setBuyer(jane)
                .setSeller(john)
                .setProduct(textbook)
                .setLastMessageAt(LocalDateTime.of(2026, 3, 2, 9, 0))
                .build());
        general = conversationRepository.save(new Conversation.Builder()
                .setBuyer(jane)
                .setSeller(john)
                .setLastMessageAt(LocalDateTime.of(2026, 3, 1, 9, 0))
                .build());
    }

    @Test
    @DisplayName("messaging a seller about a listing finds that listing's thread again")
    void findByBuyerIdAndSellerIdAndProductId_resumesTheSameThread() {
        assertThat(productThread(jane, john, textbook)).containsExactly(aboutListing);
    }

    @Test
    @DisplayName("a different listing gets a different thread")
    void findByBuyerIdAndSellerIdAndProductId_doesNotReturnAnotherListingsThread() {
        Product other = saveProduct();

        assertThat(productThread(jane, john, other)).isEmpty();
    }

    @Test
    @DisplayName("the general thread is found without a listing and the listing thread is not")
    void findByBuyerIdAndSellerIdAndProductIsNull_matchesOnlyTheGeneralThread() {
        assertThat(generalThread(jane, john)).containsExactly(general);
    }

    @Test
    @DisplayName("a thread between two other people is invisible to both lookups")
    void lookupByBothParties_excludesEveryoneElsesThreads() {
        Conversation someoneElsesThread = conversationRepository.save(new Conversation.Builder()
                .setBuyer(stranger)
                .setSeller(john)
                .setProduct(textbook)
                .setLastMessageAt(LocalDateTime.of(2026, 3, 3, 9, 0))
                .build());

        assertThat(productThread(jane, john, textbook))
                .as("otherwise anyone could read another buyer's thread by guessing the listing")
                .doesNotContain(someoneElsesThread);
    }

    @Test
    @DisplayName("a thread is not found when the two parties are the wrong way round")
    void findByBuyerIdAndSellerIdAndProductId_isNotSymmetric() {
        assertThat(productThread(john, jane, textbook)).isEmpty();
    }

    @Test
    @DisplayName("the inbox lists the user's own threads, newest activity first")
    void findByBuyerIdOrSellerIdOrderByLastMessageAtDesc_ordersByLastActivity() {
        Conversation newest = conversationRepository.save(new Conversation.Builder()
                .setBuyer(jane)
                .setSeller(john)
                .setLastMessageAt(LocalDateTime.of(2026, 3, 3, 9, 0))
                .build());

        // Both arguments are the same id on purpose: the predicate reads "threads where this account is
        // the buyer or the seller". ConversationServiceImpl passes it that way, and passing two
        // different ids would ask for one person's threads plus the other's, which is not an inbox.
        assertThat(conversationRepository.findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(jane.getId(), jane.getId()))
                .extracting(Conversation::getId)
                .containsExactly(newest.getId(), aboutListing.getId(), general.getId());
    }

    @Test
    @DisplayName("the inbox does not include conversations the user is not part of")
    void findByBuyerIdOrSellerIdOrderByLastMessageAtDesc_excludesOtherPeople() {
        Conversation someoneElses = conversationRepository.save(new Conversation.Builder()
                .setBuyer(stranger)
                .setSeller(john)
                .setProduct(textbook)
                .setLastMessageAt(LocalDateTime.of(2026, 3, 3, 9, 0))
                .build());

        assertThat(conversationRepository.findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(jane.getId(), jane.getId()))
                .doesNotContain(someoneElses);
        assertThat(conversationRepository.findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(john.getId(), john.getId()))
                .contains(someoneElses);
    }

    private List<Conversation> productThread(User buyer, User seller, Product product) {
        return conversationRepository
                .findByBuyerIdAndSellerIdAndProductId(buyer.getId(), seller.getId(), product.getId())
                .stream()
                .toList();
    }

    private List<Conversation> generalThread(User buyer, User seller) {
        return conversationRepository
                .findByBuyerIdAndSellerIdAndProductIsNull(buyer.getId(), seller.getId())
                .stream()
                .toList();
    }

    private Product saveProduct() {
        User seller = userRepository.save(buildUser("vendor-" + UUID.randomUUID(), Role.VENDOR));
        VendorProfile vendor = vendorProfileRepository.save(new VendorProfile.Builder()
                .setUser(seller)
                .setBusinessName("Block " + UUID.randomUUID() + " Books")
                .setRegistrationNo("REG-" + UUID.randomUUID())
                .build());
        return productRepository.save(new Product.Builder()
                .vendor(vendor)
                .name("Calculus textbook")
                .description("Second edition.")
                .price(new BigDecimal("250"))
                .stockQuantity(3)
                .category("Books")
                .condition(ProductCondition.GOOD)
                .build());
    }

    private User buildUser(String prefix, Role role) {
        return new User.Builder()
                .setName(prefix)
                .setEmail(prefix.toLowerCase() + "-" + UUID.randomUUID() + "@example.com")
                .setPasswordHash("hash")
                .setRole(role)
                .setVerified(true)
                .build();
    }
}