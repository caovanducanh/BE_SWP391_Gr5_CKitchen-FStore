package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.recipe.CreateRecipeRequest;
import com.example.demologin.dto.request.recipe.UpdateRecipeRequest;
import com.example.demologin.dto.response.RecipeResponse;
import com.example.demologin.entity.Ingredient;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.Recipe;
import com.example.demologin.exception.exceptions.ConflictException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.IngredientRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.RecipeRepository;
import com.example.demologin.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final ProductRepository productRepository;
    private final IngredientRepository ingredientRepository;

    @Override
    public Page<RecipeResponse> getAllRecipes(int page, int size) {
        return recipeRepository.findAll(PageRequest.of(page, size)).map(this::toResponse);
    }

    @Override
    public RecipeResponse createRecipe(CreateRecipeRequest request) {
        if (recipeRepository.existsByProduct_IdAndIngredient_Id(request.getProductId(), request.getIngredientId())) {
            throw new ConflictException("Recipe already exists for this product and ingredient");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProductId()));

        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new NotFoundException("Ingredient not found with id: " + request.getIngredientId()));

        Recipe recipe = Recipe.builder()
                .product(product)
                .ingredient(ingredient)
                .quantity(request.getQuantity())
                .unit(request.getUnit().trim())
                .createdAt(LocalDateTime.now())
                .build();

        return toResponse(recipeRepository.save(recipe));
    }

    @Override
    public RecipeResponse updateRecipe(Integer id, UpdateRecipeRequest request) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recipe not found with id: " + id));

        recipe.setQuantity(request.getQuantity());
        recipe.setUnit(request.getUnit().trim());

        return toResponse(recipeRepository.save(recipe));
    }

    @Override
    public void deleteRecipe(Integer id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recipe not found with id: " + id));
        recipeRepository.delete(recipe);
    }

    private RecipeResponse toResponse(Recipe recipe) {
        return RecipeResponse.builder()
                .id(recipe.getId())
                .productId(recipe.getProduct().getId())
                .productName(recipe.getProduct().getName())
                .ingredientId(recipe.getIngredient().getId())
                .ingredientName(recipe.getIngredient().getName())
                .quantity(recipe.getQuantity())
                .unit(recipe.getUnit())
                .createdAt(recipe.getCreatedAt())
                .build();
    }
}
