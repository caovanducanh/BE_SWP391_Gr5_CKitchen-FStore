package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.recipe.CreateRecipeRequest;
import com.example.demologin.dto.request.recipe.UpdateRecipeRequest;
import com.example.demologin.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/recipes")
@Tag(name = "Recipe Management", description = "APIs for manager recipe and ingredient norms")
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping
    @PageResponse
    @ApiResponse(message = "Recipes retrieved successfully")
    @SecuredEndpoint("RECIPE_MANAGE")
    @Operation(summary = "Get recipes", description = "Get paginated recipe list")
    public Object getAllRecipes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return recipeService.getAllRecipes(page, size);
    }

    @PostMapping
    @ApiResponse(message = "Recipe created successfully")
    @SecuredEndpoint("RECIPE_MANAGE")
    @Operation(summary = "Create recipe", description = "Create recipe norm for product and ingredient")
    public Object createRecipe(@Valid @RequestBody CreateRecipeRequest request) {
        return recipeService.createRecipe(request);
    }

    @PutMapping("/{id}")
    @ApiResponse(message = "Recipe updated successfully")
    @SecuredEndpoint("RECIPE_MANAGE")
    @Operation(summary = "Update recipe", description = "Update recipe quantity and unit")
    public Object updateRecipe(@PathVariable Integer id, @Valid @RequestBody UpdateRecipeRequest request) {
        return recipeService.updateRecipe(id, request);
    }

    @DeleteMapping("/{id}")
    @ApiResponse(message = "Recipe deleted successfully")
    @SecuredEndpoint("RECIPE_MANAGE")
    @Operation(summary = "Delete recipe", description = "Delete recipe by id")
    public void deleteRecipe(@PathVariable Integer id) {
        recipeService.deleteRecipe(id);
    }
}
