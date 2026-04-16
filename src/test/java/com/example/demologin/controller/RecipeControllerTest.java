package com.example.demologin.controller;

import com.example.demologin.dto.request.recipe.CreateRecipeRequest;
import com.example.demologin.dto.request.recipe.UpdateRecipeRequest;
import com.example.demologin.dto.response.RecipeResponse;
import com.example.demologin.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeControllerTest {

    @Mock
    private RecipeService recipeService;

    @InjectMocks
    private RecipeController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllRecipes() {
        RecipeResponse response = RecipeResponse.builder().id(1).build();
        Page<RecipeResponse> page = new PageImpl<>(List.of(response));
        when(recipeService.getAllRecipes(0, 20)).thenReturn(page);

        Object result = controller.getAllRecipes(0, 20);

        assertSame(page, result);
        verify(recipeService).getAllRecipes(0, 20);
    }

    @Test
    void createRecipe() {
        CreateRecipeRequest request = new CreateRecipeRequest();
        request.setProductId("PROD000001");
        request.setIngredientId("ING000001");
        request.setQuantity(BigDecimal.ONE);
        request.setUnit("kg");

        RecipeResponse response = RecipeResponse.builder().id(1).build();
        when(recipeService.createRecipe(request)).thenReturn(response);

        Object result = controller.createRecipe(request);

        assertSame(response, result);
        verify(recipeService).createRecipe(request);
    }

    @Test
    void updateRecipe() {
        UpdateRecipeRequest request = new UpdateRecipeRequest();
        request.setQuantity(BigDecimal.TEN);
        request.setUnit("kg");

        RecipeResponse response = RecipeResponse.builder().id(1).build();
        when(recipeService.updateRecipe(1, request)).thenReturn(response);

        Object result = controller.updateRecipe(1, request);

        assertSame(response, result);
        verify(recipeService).updateRecipe(1, request);
    }

    @Test
    void deleteRecipe() {
        controller.deleteRecipe(1);

        verify(recipeService).deleteRecipe(1);
    }
}
