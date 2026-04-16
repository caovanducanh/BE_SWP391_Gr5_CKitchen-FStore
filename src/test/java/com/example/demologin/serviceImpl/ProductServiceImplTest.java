package com.example.demologin.serviceImpl;

import com.example.demologin.dto.response.ProductResponse;
import com.example.demologin.entity.Product;
import com.example.demologin.enums.ProductCategory;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.mapper.ProductMapper;
import com.example.demologin.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder().id("PROD000001").name("Bread").category(ProductCategory.BAKERY).build();
    }

    @Test
    void getAllProducts_shouldApplySearchAndCategoryFilter() {
        Page<Product> productPage = new PageImpl<>(List.of(product));
        ProductResponse response = ProductResponse.builder().id("PROD000001").name("Bread").category(ProductCategory.BAKERY).build();

        when(productRepository.searchProducts(eq("bread"), eq(ProductCategory.BAKERY), any(Pageable.class)))
                .thenReturn(productPage);
        when(productMapper.toResponse(product)).thenReturn(response);

        Page<ProductResponse> result = productService.getAllProducts(0, 20, "  bread ", "bakery");

        assertEquals(1, result.getTotalElements());
        assertEquals("PROD000001", result.getContent().get(0).getId());
        verify(productRepository).searchProducts(eq("bread"), eq(ProductCategory.BAKERY), any(Pageable.class));
        verify(productMapper).toResponse(product);
    }

    @Test
    void getAllProducts_shouldThrowWhenCategoryInvalid() {
        assertThrows(BadRequestException.class, () -> productService.getAllProducts(0, 20, "bread", "invalid"));
    }

    @Test
    void getAllCategories_shouldReturnAllEnumValues() {
        List<ProductCategory> categories = productService.getAllCategories();

        assertEquals(ProductCategory.values().length, categories.size());
        assertTrue(categories.contains(ProductCategory.BAKERY));
        assertTrue(categories.contains(ProductCategory.BEVERAGE));
        assertTrue(categories.contains(ProductCategory.SNACK));
        assertTrue(categories.contains(ProductCategory.FROZEN));
        assertTrue(categories.contains(ProductCategory.OTHER));
    }
}
