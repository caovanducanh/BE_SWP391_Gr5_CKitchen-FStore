package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.recipe.CreateRecipeRequest;
import com.example.demologin.dto.request.recipe.UpdateRecipeRequest;
import com.example.demologin.dto.response.RecipeResponse;
import com.example.demologin.entity.Ingredient;
import com.example.demologin.entity.Product;
import com.example.demologin.entity.Recipe;
import com.example.demologin.enums.ProductCategory;
import com.example.demologin.exception.exceptions.ConflictException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.IngredientRepository;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.repository.RecipeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceImplTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private RecipeServiceImpl recipeService;

    private Product product;
    private Ingredient ingredient;

    @BeforeEach
    void setUp() {
        product = Product.builder().id("PROD000001").name("Bread").category(ProductCategory.BAKERY).build();
        ingredient = Ingredient.builder().id("ING000001").name("Flour").build();
    }

    @Test
    void getAllRecipes_shouldReturnPagedResponse() {
        Recipe recipe = Recipe.builder().id(1).product(product).ingredient(ingredient).quantity(BigDecimal.ONE).unit("kg").build();
        when(recipeRepository.findAll(PageRequest.of(0, 20))).thenReturn(new PageImpl<>(List.of(recipe)));

        Page<RecipeResponse> result = recipeService.getAllRecipes(0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals("PROD000001", result.getContent().get(0).getProductId());
    }

    @Test
    void createRecipe_shouldThrowWhenDuplicate() {
        CreateRecipeRequest request = new CreateRecipeRequest();
        request.setProductId("PROD000001");
        request.setIngredientId("ING000001");

        when(recipeRepository.existsByProduct_IdAndIngredient_Id("PROD000001", "ING000001")).thenReturn(true);

        assertThrows(ConflictException.class, () -> recipeService.createRecipe(request));
    }

    @Test
    void createRecipe_shouldCreateWhenValid() {
        CreateRecipeRequest request = new CreateRecipeRequest();
        request.setProductId("PROD000001");
        request.setIngredientId("ING000001");
        request.setQuantity(BigDecimal.valueOf(2));
        request.setUnit(" kg ");

        when(recipeRepository.existsByProduct_IdAndIngredient_Id("PROD000001", "ING000001")).thenReturn(false);
        when(productRepository.findById("PROD000001")).thenReturn(Optional.of(product));
        when(ingredientRepository.findById("ING000001")).thenReturn(Optional.of(ingredient));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe saved = invocation.getArgument(0);
            saved.setId(10);
            return saved;
        });

        RecipeResponse result = recipeService.createRecipe(request);

        assertEquals(10, result.getId());
        assertEquals("kg", result.getUnit());
        assertEquals("Bread", result.getProductName());
    }

    @Test
    void createRecipe_shouldThrowWhenProductMissing() {
        CreateRecipeRequest request = new CreateRecipeRequest();
        request.setProductId("PROD404");
        request.setIngredientId("ING000001");

        when(recipeRepository.existsByProduct_IdAndIngredient_Id("PROD404", "ING000001")).thenReturn(false);
        when(productRepository.findById("PROD404")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> recipeService.createRecipe(request));
    }

    @Test
    void updateRecipe_shouldUpdateQuantityAndUnit() {
        UpdateRecipeRequest request = new UpdateRecipeRequest();
        request.setQuantity(BigDecimal.valueOf(3));
        request.setUnit(" g ");

        Recipe recipe = Recipe.builder().id(2).product(product).ingredient(ingredient).quantity(BigDecimal.ONE).unit("kg").build();

        when(recipeRepository.findById(2)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        RecipeResponse result = recipeService.updateRecipe(2, request);

        assertEquals(BigDecimal.valueOf(3), result.getQuantity());
        assertEquals("g", result.getUnit());
    }

    @Test
    void deleteRecipe_shouldDeleteWhenFound() {
        Recipe recipe = Recipe.builder().id(3).product(product).ingredient(ingredient).build();
        when(recipeRepository.findById(3)).thenReturn(Optional.of(recipe));

        recipeService.deleteRecipe(3);

        verify(recipeRepository).delete(recipe);
    }
}
