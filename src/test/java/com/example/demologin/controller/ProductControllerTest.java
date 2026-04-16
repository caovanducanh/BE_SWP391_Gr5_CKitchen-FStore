package com.example.demologin.controller;

import com.example.demologin.dto.request.product.CreateProductRequest;
import com.example.demologin.dto.request.product.UpdateProductRequest;
import com.example.demologin.dto.response.ProductResponse;
import com.example.demologin.enums.ProductCategory;
import com.example.demologin.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllProducts() {
        ProductResponse response = ProductResponse.builder()
                .id("PROD000001")
                .name("Bread")
                .category(ProductCategory.BAKERY)
                .unit("piece")
                .price(BigDecimal.valueOf(10))
                .cost(BigDecimal.valueOf(7))
                .build();
        Page<ProductResponse> page = new PageImpl<>(List.of(response));

        when(productService.getAllProducts(0, 20, "bre", "BAKERY")).thenReturn(page);

        Object result = controller.getAllProducts(0, 20, "bre", "BAKERY");

        assertSame(page, result);
        verify(productService).getAllProducts(0, 20, "bre", "BAKERY");
    }

    @Test
    void getAllCategories() {
        List<ProductCategory> categories = List.of(ProductCategory.BAKERY, ProductCategory.BEVERAGE);
        when(productService.getAllCategories()).thenReturn(categories);

        Object result = controller.getAllCategories();

        assertSame(categories, result);
        verify(productService).getAllCategories();
    }

    @Test
    void getProductById() {
        ProductResponse response = ProductResponse.builder().id("PROD000001").build();
        when(productService.getProductById("PROD000001")).thenReturn(response);

        Object result = controller.getProductById("PROD000001");

        assertSame(response, result);
        verify(productService).getProductById("PROD000001");
    }

    @Test
    void createProduct() {
        CreateProductRequest request = new CreateProductRequest();
        ProductResponse response = ProductResponse.builder().id("PROD000001").build();
        when(productService.createProduct(request)).thenReturn(response);

        Object result = controller.createProduct(request);

        assertSame(response, result);
        verify(productService).createProduct(request);
    }

    @Test
    void updateProduct() {
        UpdateProductRequest request = new UpdateProductRequest();
        ProductResponse response = ProductResponse.builder().id("PROD000001").build();
        when(productService.updateProduct("PROD000001", request)).thenReturn(response);

        Object result = controller.updateProduct("PROD000001", request);

        assertSame(response, result);
        verify(productService).updateProduct("PROD000001", request);
    }

    @Test
    void uploadProductImages() {
        MultipartFile[] images = new MultipartFile[]{org.mockito.Mockito.mock(MultipartFile.class)};
        ProductResponse response = ProductResponse.builder().id("PROD000001").build();
        when(productService.uploadProductImages("PROD000001", images)).thenReturn(response);

        Object result = controller.uploadProductImages("PROD000001", images);

        assertSame(response, result);
        verify(productService).uploadProductImages("PROD000001", images);
    }

    @Test
    void uploadSingleProductImage() {
        MultipartFile image = org.mockito.Mockito.mock(MultipartFile.class);
        ProductResponse response = ProductResponse.builder().id("PROD000001").build();
        when(productService.uploadProductImages(org.mockito.Mockito.eq("PROD000001"), any(MultipartFile[].class)))
            .thenReturn(response);

        Object result = controller.uploadProductImage("PROD000001", image);

        assertSame(response, result);
        verify(productService).uploadProductImages(org.mockito.Mockito.eq("PROD000001"),
            argThat(files -> files != null && files.length == 1 && files[0] == image));
    }

    @Test
    void deleteProductImage() {
        ProductResponse response = ProductResponse.builder().id("PROD000001").build();
        when(productService.deleteProductImage("PROD000001", "url")).thenReturn(response);

        Object result = controller.deleteProductImage("PROD000001", "url");

        assertSame(response, result);
        verify(productService).deleteProductImage("PROD000001", "url");
    }

    @Test
    void deleteProduct() {
        controller.deleteProduct("PROD000001");

        verify(productService).deleteProduct("PROD000001");
    }
}
