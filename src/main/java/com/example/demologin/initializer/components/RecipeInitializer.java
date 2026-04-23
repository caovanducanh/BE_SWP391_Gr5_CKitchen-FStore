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

        log.info("Creating initial recipes matching bakery ingredients...");

        List<Recipe> recipes = new ArrayList<>();

        // Products
        Product prod1 = productRepository.findById("PROD001").orElse(null); // Croissant
        Product prod2 = productRepository.findById("PROD002").orElse(null); // Bánh mì gối
        Product prod3 = productRepository.findById("PROD003").orElse(null); // Bánh quy bơ
        Product prod4 = productRepository.findById("PROD004").orElse(null); // Bánh kem dâu

        // Ingredients
        Ingredient flour11 = ingredientRepository.findById("BAKE003").orElse(null); // Bột mì số 11
        Ingredient flourCake = ingredientRepository.findById("BAKE002").orElse(null); // Bột mì số 8 (Cake)
        Ingredient butter = ingredientRepository.findById("BAKE030").orElse(null); // Bơ lạt
        Ingredient sugar = ingredientRepository.findById("BAKE020").orElse(null); // Đường cát trắng
        Ingredient egg = ingredientRepository.findById("BAKE040").orElse(null); // Trứng gà
        Ingredient yeast = ingredientRepository.findById("BAKE050").orElse(null); // Men nở khô

        // Recipe for PROD001 (Croissant) - Needs Flour 11, Butter, Yeast
        if (prod1 != null && flour11 != null && butter != null && yeast != null) {
            recipes.add(createRecipe(prod1, flour11, "0.5"));
            recipes.add(createRecipe(prod1, butter, "0.2"));
            recipes.add(createRecipe(prod1, yeast, "0.01"));
        }

        // Recipe for PROD002 (Bánh mì gối) - Needs Flour 11, Sugar, Yeast
        if (prod2 != null && flour11 != null && sugar != null && yeast != null) {
            recipes.add(createRecipe(prod2, flour11, "0.6"));
            recipes.add(createRecipe(prod2, sugar, "0.05"));
            recipes.add(createRecipe(prod2, yeast, "0.01"));
        }

        // Recipe for PROD003 (Bánh quy bơ) - Needs Flour Cake, Butter, Sugar, Egg
        if (prod3 != null && flourCake != null && butter != null && sugar != null && egg != null) {
            recipes.add(createRecipe(prod3, flourCake, "0.3"));
            recipes.add(createRecipe(prod3, butter, "0.15"));
            recipes.add(createRecipe(prod3, sugar, "0.1"));
            recipes.add(createRecipe(prod3, egg, "1.0")); // 1 egg
        }

        // Recipe for PROD004 (Bánh kem dâu) - Needs Flour Cake, Egg, Sugar, Butter
        if (prod4 != null && flourCake != null && egg != null && sugar != null && butter != null) {
            recipes.add(createRecipe(prod4, flourCake, "0.4"));
            recipes.add(createRecipe(prod4, egg, "4.0")); // 4 eggs
            recipes.add(createRecipe(prod4, sugar, "0.2"));
            recipes.add(createRecipe(prod4, butter, "0.1"));
        }

        recipeRepository.saveAll(recipes);
        log.info("✅ Created {} bakery-accurate recipe entries", recipes.size());
    }

    private Recipe createRecipe(Product p, Ingredient i, String qty) {
        return Recipe.builder()
                .product(p)
                .ingredient(i)
                .quantity(new BigDecimal(qty))
                .unit(i.getUnit()) // Consistent with ingredient unit
                .createdAt(LocalDateTime.now())
                .build();
    }
}
