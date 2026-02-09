package com.techgadget.ecommerce.config;

import com.techgadget.ecommerce.domain.UserRole;
import com.techgadget.ecommerce.entity.Category;
import com.techgadget.ecommerce.entity.Product;
import com.techgadget.ecommerce.entity.User;
import com.techgadget.ecommerce.repository.CategoryRepository;
import com.techgadget.ecommerce.repository.ProductRepository;
import com.techgadget.ecommerce.repository.UserRepository;
import com.techgadget.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Value("${app.admin.password}")
    private String adminPassword;

    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public void run(String... args) {

        // Create an admin account
        User user = new User();
        user.setEmail("admin@email.com");
        user.setPassword(passwordEncoder.encode(adminPassword));
        user.setFullName("Admin123");
        user.setRole(UserRole.ADMIN);
        user.setUsername("admin");
        user.setPhoneNumber("08172633");

        userRepository.save(user);

        // Make sure category is always empty
        if (categoryRepository.count() > 0) {
            return;
        }

        // Create categories
        Category smartphones = new Category();
        smartphones.setName("Smartphones");
        smartphones.setDescription("Mobile phones and devices");
        categoryRepository.save(smartphones);

        Category laptops = new Category();
        laptops.setName("Laptops");
        laptops.setDescription("Personal computers and notebooks");
        categoryRepository.save(laptops);

        Category audio = new Category();
        audio.setName("Audio");
        audio.setDescription("Headphones, speakers, and audio equipment");
        categoryRepository.save(audio);

        // Create products with Rupiah prices
        Product iphone15 = new Product();
        iphone15.setName("iPhone 15 Pro");
        iphone15.setDescription("Latest Apple flagship with A17 Pro chip");
        iphone15.setPriceInRupiah(14999000L);  // Rp 14.999.000
        iphone15.setStockQuantity(50);
        iphone15.setImageUrl("https://via.placeholder.com/300?text=iPhone+15");
        iphone15.setCategory(smartphones);
        iphone15.setSpecs(Map.of(
                "screen", "6.1 inch",
                "processor", "A17 Pro",
                "ram", "8GB",
                "storage", "256GB"
        ));
        productRepository.save(iphone15);

        Product samsung24 = new Product();
        samsung24.setName("Samsung Galaxy S24");
        samsung24.setDescription("Powerful Android flagship");
        samsung24.setPriceInRupiah(12999000L);  // Rp 12.999.000
        samsung24.setStockQuantity(45);
        samsung24.setImageUrl("https://via.placeholder.com/300?text=Samsung+S24");
        samsung24.setCategory(smartphones);
        samsung24.setSpecs(Map.of(
                "screen", "6.2 inch",
                "processor", "Snapdragon 8 Gen 3",
                "ram", "12GB",
                "storage", "512GB"
        ));
        productRepository.save(samsung24);

        Product macbook = new Product();
        macbook.setName("MacBook Pro 16\"");
        macbook.setDescription("Professional laptop with M3 Max chip");
        macbook.setPriceInRupiah(53999000L);  // Rp 53.999.000
        macbook.setStockQuantity(20);
        macbook.setImageUrl("https://via.placeholder.com/300?text=MacBook+Pro");
        macbook.setCategory(laptops);
        macbook.setSpecs(Map.of(
                "processor", "M3 Max",
                "ram", "36GB",
                "storage", "1TB SSD",
                "display", "16 inch Retina"
        ));
        productRepository.save(macbook);

        Product airpods = new Product();
        airpods.setName("AirPods Pro 2");
        airpods.setDescription("Premium wireless earbuds with noise cancellation");
        airpods.setPriceInRupiah(3799000L);  // Rp 3.799.000
        airpods.setStockQuantity(100);
        airpods.setImageUrl("https://via.placeholder.com/300?text=AirPods+Pro");
        airpods.setCategory(audio);
        airpods.setSpecs(Map.of(
                "type", "Earbuds",
                "battery", "6 hours",
                "charging_case", "30 hours",
                "noise_cancellation", "Active"
        ));
        productRepository.save(airpods);


        System.out.println("✅ Database seeded with Rupiah prices");
    }
}
