package com.example.demologin.initializer.components;

import com.example.demologin.entity.Product;
import com.example.demologin.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductInitializer {

    private final ProductRepository productRepository;

    @Transactional
    public void initializeProducts() {
        if (productRepository.count() > 0) {
            log.info("⏭️ Products already initialized. Skipping...");
            return;
        }

        log.info("Creating initial products...");

        List<Product> products = List.of(
                Product.builder()
                        .id("PROD001")
                        .name("Bánh Mì Sừng Bò")
                        .category("Bakery")
                        .unit("cái")
                        .price(new BigDecimal("25000"))
                        .cost(new BigDecimal("12000"))
                        .image("croissant.jpg")
                        .createdAt(LocalDateTime.now())
                        .build(),
                Product.builder()
                        .id("PROD002")
                        .name("Bánh Mì Gối")
                        .category("Bakery")
                        .unit("ổ")
                        .price(new BigDecimal("15000"))
                        .cost(new BigDecimal("7000"))
                        .image("bread.jpg")
                        .createdAt(LocalDateTime.now())
                        .build(),
                Product.builder()
                        .id("PROD003")
                        .name("Bánh Quy Bơ")
                        .category("Bakery")
                        .unit("hộp")
                        .price(new BigDecimal("45000"))
                        .cost(new BigDecimal("20000"))
                        .image("cookies.jpg")
                        .createdAt(LocalDateTime.now())
                        .build(),
                Product.builder()
                        .id("PROD004")
                        .name("Bánh Kem Dâu")
                        .category("Bakery")
                        .unit("ổ")
                        .price(new BigDecimal("200000"))
                        .cost(new BigDecimal("90000"))
                        .image("strawberry_cake.jpg")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        productRepository.saveAll(products);
        log.info("✅ Created {} products", products.size());
    }
}
