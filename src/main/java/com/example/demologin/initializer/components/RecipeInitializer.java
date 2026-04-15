package com.example.demologin.initializer.components;

import com.example.demologin.entity.Ingredient;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.Recipe;
import com.example.demologin.repository.IngredientRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipeInitializer {

    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;

    @Transactional
    public void initializeRecipes() {
        if (recipeRepository.count() > 0) {
            log.info("⏭️ Recipes already initialized. Skipping...");
            return;
        }

        log.info("Creating initial recipes...");

        List<Recipe> recipes = new ArrayList<>();

        // Get some products and ingredients
        Product prod1 = productRepository.findById("PROD001").orElse(null); // Croissant
        Product prod2 = productRepository.findById("PROD002").orElse(null); // Bánh mì gối

        Ingredient ing1 = ingredientRepository.findById("ING001").orElse(null); // Bột mì
        Ingredient ing5 = ingredientRepository.findById("ING005").orElse(null); // Bơ

        if (prod1 != null && ing1 != null && ing5 != null) {
            // Recipe for Croissant
            recipes.add(Recipe.builder()
                    .product(prod1)
                    .ingredient(ing1)
                    .quantity(new BigDecimal("0.2")) // 200g bột mì
                    .unit("kg")
                    .createdAt(LocalDateTime.now())
                    .build());
            recipes.add(Recipe.builder()
                    .product(prod1)
                    .ingredient(ing5)
                    .quantity(new BigDecimal("0.1")) // 100g bơ
                    .unit("kg")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        if (prod2 != null && ing1 != null) {
            // Recipe for Bánh mì gối
            recipes.add(Recipe.builder()
                    .product(prod2)
                    .ingredient(ing1)
                    .quantity(new BigDecimal("0.5")) // 500g bột mì
                    .unit("kg")
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        recipeRepository.saveAll(recipes);
        log.info("✅ Created {} recipe entries", recipes.size());
    }
}
