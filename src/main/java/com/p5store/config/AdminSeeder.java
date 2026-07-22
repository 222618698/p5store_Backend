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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ensures a known admin login, starter categories, and a handful of sample
 * products exist on every startup — required for local dev since the
 * in-memory H2 database is wiped on each restart, and also runs once against
 * the persistent production database to bootstrap the first admin account.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private static final List<String> STARTER_CATEGORIES = List.of(
            "Electronics", "Home & Living", "Beauty", "Fashion", "Sports"
    );

    // One-time taxonomy migration: the store used to seed a more granular
    // category set. Any products under these older names get moved to the
    // mapped target category below instead of being orphaned when the old
    // category is removed.
    private static final Map<String, String> CATEGORY_RENAME_MAP = Map.of(
            "Watches & Jewelry", "Fashion",
            "Fragrance", "Beauty",
            "Leather Goods", "Fashion",
            "Footwear", "Fashion",
            "Home Audio", "Home & Living",
            "Home & Kitchen", "Home & Living",
            "Accessories", "Home & Living"
    );

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedAdmin();
        migrateCategoryTaxonomy();
        seedCategories();
        seedProducts();
    }

    private void migrateCategoryTaxonomy() {
        boolean hasOldCategories = CATEGORY_RENAME_MAP.keySet().stream()
                .anyMatch(categoryRepository::existsByName);
        if (!hasOldCategories) {
            return;
        }

        List<Category> all = categoryRepository.findAll();
        Map<String, Category> byName = new LinkedHashMap<>();
        for (Category c : all) {
            byName.put(c.getName(), c);
        }

        for (Map.Entry<String, String> entry : CATEGORY_RENAME_MAP.entrySet()) {
            Category oldCategory = byName.get(entry.getKey());
            if (oldCategory == null) {
                continue;
            }
            String targetName = entry.getValue();
            Category target = byName.computeIfAbsent(targetName, name -> {
                Category c = new Category();
                c.setName(name);
                return categoryRepository.save(c);
            });
            Long oldCategoryId = oldCategory.getId();
            for (Product p : productRepository.findAll()) {
                if (p.getCategory() != null && oldCategoryId.equals(p.getCategory().getId())) {
                    p.setCategory(target);
                    productRepository.save(p);
                }
            }
            categoryRepository.delete(oldCategory);
        }

        log.info("Migrated category taxonomy to: {}", String.join(", ", STARTER_CATEGORIES));
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = new User();
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(UserRole.ADMIN);
        admin = userRepository.save(admin);

        Cart cart = new Cart();
        cart.setUser(admin);
        cartRepository.save(cart);

        log.info("Seeded admin account: {}", adminEmail);
    }

    private void seedCategories() {
        // Once any categories exist (whether the demo set or a real imported
        // catalog), stop injecting the demo starter categories — this used to
        // unconditionally re-add "Electronics" / "Home & Living" / "Sports"
        // on every restart even after the real catalog was imported, since
        // those specific names don't exist in it.
        if (categoryRepository.count() > 0) {
            return;
        }
        for (String name : STARTER_CATEGORIES) {
            if (!categoryRepository.existsByName(name)) {
                Category category = new Category();
                category.setName(name);
                categoryRepository.save(category);
                log.info("Seeded starter category: {}", name);
            }
        }
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
        createProduct(categories.get("Fashion"), "Monochrome Cloud Sneakers", "SNK-100",
                "120.00", "150.00", null, 60,
                "https://images.unsplash.com/photo-1560769629-975ec94e6a86?w=600&q=80", false);
        createProduct(categories.get("Home & Living"), "Symphony Home Speaker", "SPK-200",
                "299.00", "399.00", null, 25,
                "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=600&q=80", false);
        createProduct(categories.get("Home & Living"), "Barista Elite Espresso Maker", "ESP-300",
                "899.00", "1099.00", null, 15,
                "https://images.unsplash.com/photo-1517256064527-09c73fc73e38?w=600&q=80", false);
        createProduct(categories.get("Fashion"), "Pillar Signature Silk Scarf", "SCF-400",
                "75.00", "110.00", null, 40,
                "https://images.unsplash.com/photo-1601924994987-69e26d50dc26?w=600&q=80", false);

        // "New In" items — most recently created, so they lead the feed.
        createProduct(categories.get("Electronics"), "Elite Pro Noise-Cancelling Headphones", "HDP-500",
                "349.00", null, "New", 50,
                "https://images.unsplash.com/photo-1583394838336-acd977736f90?w=600&q=80", false);
        createProduct(categories.get("Fashion"), "Vanguard Chronograph Silver Edition", "WCH-600",
                "1299.00", null, "Featured", 10,
                "https://images.unsplash.com/photo-1524805444758-089113d48a6d?w=600&q=80", true);
        createProduct(categories.get("Beauty"), "Midnight Essence Eau de Parfum", "PRF-700",
                "185.00", null, null, 35,
                "https://images.unsplash.com/photo-1617897903246-719242758050?w=600&q=80", false);
        createProduct(categories.get("Fashion"), "The Architect Briefcase", "BRF-800",
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
