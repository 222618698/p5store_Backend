package com.p5store.config;

import com.p5store.domain.Cart;
import com.p5store.domain.Category;
import com.p5store.domain.Product;
import com.p5store.domain.User;
import com.p5store.enums.ProductStatus;
import com.p5store.enums.UserRole;
import com.p5store.repository.CartRepository;
import com.p5store.repository.CategoryRepository;
import com.p5store.repository.ProductRepository;
import com.p5store.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local-dev only: ensures a known admin login, starter categories, and a
 * handful of sample products exist on every startup, since the in-memory
 * H2 database is wiped on each restart.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@pillar5.co.za";
    private static final String ADMIN_PASSWORD = "admin";
    private static final List<String> STARTER_CATEGORIES = List.of(
            "Electronics", "Watches & Jewelry", "Fragrance", "Leather Goods",
            "Footwear", "Home Audio", "Home & Kitchen", "Accessories"
    );

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        seedCategories();
        seedProducts();
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }

        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        admin.setRole(UserRole.ADMIN);
        admin = userRepository.save(admin);

        Cart cart = new Cart();
        cart.setUser(admin);
        cartRepository.save(cart);

        log.info("Seeded admin account: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }
        for (String name : STARTER_CATEGORIES) {
            Category category = new Category();
            category.setName(name);
            categoryRepository.save(category);
        }
        log.info("Seeded {} starter categories", STARTER_CATEGORIES.size());
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        Map<String, Category> categories = new LinkedHashMap<>();
        for (Category c : categoryRepository.findAll()) {
            categories.put(c.getName(), c);
        }

        // "Latest Offers" items — seeded first so they rank behind the
        // "New In" batch below in the createdAt-ordered new-arrivals feed.
        createProduct(categories.get("Footwear"), "Monochrome Cloud Sneakers", "SNK-100",
                "120.00", "150.00", null, 60,
                "https://images.unsplash.com/photo-1560769629-975ec94e6a86?w=600&q=80", false);
        createProduct(categories.get("Home Audio"), "Symphony Home Speaker", "SPK-200",
                "299.00", "399.00", null, 25,
                "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=600&q=80", false);
        createProduct(categories.get("Home & Kitchen"), "Barista Elite Espresso Maker", "ESP-300",
                "899.00", "1099.00", null, 15,
                "https://images.unsplash.com/photo-1517256064527-09c73fc73e38?w=600&q=80", false);
        createProduct(categories.get("Accessories"), "Pillar Signature Silk Scarf", "SCF-400",
                "75.00", "110.00", null, 40,
                "https://images.unsplash.com/photo-1601924994987-69e26d50dc26?w=600&q=80", false);

        // "New In" items — most recently created, so they lead the feed.
        createProduct(categories.get("Electronics"), "Elite Pro Noise-Cancelling Headphones", "HDP-500",
                "349.00", null, "New", 50,
                "https://images.unsplash.com/photo-1583394838336-acd977736f90?w=600&q=80", false);
        createProduct(categories.get("Watches & Jewelry"), "Vanguard Chronograph Silver Edition", "WCH-600",
                "1299.00", null, "Featured", 10,
                "https://images.unsplash.com/photo-1524805444758-089113d48a6d?w=600&q=80", true);
        createProduct(categories.get("Fragrance"), "Midnight Essence Eau de Parfum", "PRF-700",
                "185.00", null, null, 35,
                "https://images.unsplash.com/photo-1617897903246-719242758050?w=600&q=80", false);
        createProduct(categories.get("Leather Goods"), "The Architect Briefcase", "BRF-800",
                "460.00", null, null, 20,
                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600&q=80", false);

        log.info("Seeded 8 sample products");
    }

    private void createProduct(Category category, String name, String sku, String price,
                                String compareAtPrice, String badge, int stock, String imageUrl,
                                boolean featured) {
        Product p = new Product();
        p.setCategory(category);
        p.setName(name);
        p.setSku(sku);
        p.setPrice(new BigDecimal(price));
        p.setCompareAtPrice(compareAtPrice != null ? new BigDecimal(compareAtPrice) : null);
        p.setBadge(badge);
        p.setStockQuantity(stock);
        p.setImageUrl(imageUrl);
        p.setFeatured(featured);
        p.setStatus(ProductStatus.ACTIVE);
        productRepository.save(p);
    }
}
