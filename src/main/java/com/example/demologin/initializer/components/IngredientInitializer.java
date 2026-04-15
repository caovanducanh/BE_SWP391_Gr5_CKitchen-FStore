package com.example.demologin.initializer.components;

import com.example.demologin.entity.Ingredient;
import com.example.demologin.repository.IngredientRepository;
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
public class IngredientInitializer {

    private final IngredientRepository ingredientRepository;

    @Transactional
    public void initializeIngredients() {
        if (ingredientRepository.count() > 0) {
            log.info("⏭️ Ingredients already initialized. Skipping...");
            return;
        }

        log.info("Creating initial ingredients...");

        List<Ingredient> ingredients = List.of(
                Ingredient.builder()
                        .id("ING001")
                        .name("Bột mì")
                        .unit("kg")
                        .price(new BigDecimal("15000"))
                        .supplier("Công ty Bột Mì")
                        .minStock(10)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Ingredient.builder()
                        .id("ING002")
                        .name("Đường")
                        .unit("kg")
                        .price(new BigDecimal("20000"))
                        .supplier("Công ty Đường")
                        .minStock(5)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Ingredient.builder()
                        .id("ING003")
                        .name("Sữa tươi")
                        .unit("l")
                        .price(new BigDecimal("25000"))
                        .supplier("Vinamilk")
                        .minStock(20)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Ingredient.builder()
                        .id("ING004")
                        .name("Trứng gà")
                        .unit("quả")
                        .price(new BigDecimal("3000"))
                        .supplier("Trang trại")
                        .minStock(50)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Ingredient.builder()
                        .id("ING005")
                        .name("Bơ")
                        .unit("kg")
                        .price(new BigDecimal("150000"))
                        .supplier("Nhập khẩu")
                        .minStock(2)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        ingredientRepository.saveAll(ingredients);
        log.info("✅ Created {} ingredients", ingredients.size());
    }
}
