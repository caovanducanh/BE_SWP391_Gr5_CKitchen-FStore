package com.example.demologin.service;

import com.example.demologin.dto.request.recipe.CreateRecipeRequest;
import com.example.demologin.dto.request.recipe.UpdateRecipeRequest;
import com.example.demologin.dto.response.RecipeResponse;
import org.springframework.data.domain.Page;

public interface RecipeService {
    Page<RecipeResponse> getAllRecipes(int page, int size);

    RecipeResponse createRecipe(CreateRecipeRequest request);

    RecipeResponse updateRecipe(Integer id, UpdateRecipeRequest request);

    void deleteRecipe(Integer id);
}
