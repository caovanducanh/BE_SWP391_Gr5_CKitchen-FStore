package com.example.demologin.controller;

import com.example.demologin.annotation.ApiResponse;
import com.example.demologin.annotation.PageResponse;
import com.example.demologin.annotation.SecuredEndpoint;
import com.example.demologin.dto.request.product.CreateProductRequest;
import com.example.demologin.dto.request.product.UpdateProductRequest;
import com.example.demologin.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/products")
@Tag(name = "Product Management", description = "APIs for manager product management")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @PageResponse
    @ApiResponse(message = "Products retrieved successfully")
    @SecuredEndpoint("PRODUCT_MANAGE")
    @Operation(summary = "Get products", description = "Get paginated product list")
    public Object getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return productService.getAllProducts(page, size);
    }

    @GetMapping("/{id}")
    @ApiResponse(message = "Product retrieved successfully")
    @SecuredEndpoint("PRODUCT_MANAGE")
    @Operation(summary = "Get product by id", description = "Get a product by id")
    public Object getProductById(@PathVariable String id) {
        return productService.getProductById(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(message = "Product created successfully")
    @SecuredEndpoint("PRODUCT_MANAGE")
    @Operation(summary = "Create product", description = "Create a new product and optionally upload multiple images to MinIO")
    public Object createProduct(@Valid @ModelAttribute CreateProductRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(message = "Product updated successfully")
    @SecuredEndpoint("PRODUCT_MANAGE")
    @Operation(summary = "Update product", description = "Update product information and optionally replace all images")
    public Object updateProduct(@PathVariable String id, @Valid @ModelAttribute UpdateProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(message = "Product images uploaded successfully")
    @SecuredEndpoint("PRODUCT_MANAGE")
    @Operation(summary = "Upload product images", description = "Upload multiple product images to MinIO and append to imageUrl list")
    public Object uploadProductImages(@PathVariable String id, @RequestParam("images") MultipartFile[] images) {
        return productService.uploadProductImages(id, images);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(message = "Product image uploaded successfully")
    @SecuredEndpoint("PRODUCT_MANAGE")
    @Operation(summary = "Upload single product image", description = "Backward-compatible endpoint that appends one image to imageUrl list")
    public Object uploadProductImage(@PathVariable String id, @RequestParam("image") MultipartFile image) {
        return productService.uploadProductImages(id, new MultipartFile[]{image});
    }

    @DeleteMapping("/{id}/images")
    @ApiResponse(message = "Product image deleted successfully")
    @SecuredEndpoint("PRODUCT_MANAGE")
    @Operation(summary = "Delete product image", description = "Delete one image URL from product and remove object in MinIO")
    public Object deleteProductImage(@PathVariable String id, @RequestParam("imageUrl") String imageUrl) {
        return productService.deleteProductImage(id, imageUrl);
    }

    // @PutMapping("/{id}/images")
    // @ApiResponse(message = "Product image URLs replaced successfully")
    // @SecuredEndpoint("PRODUCT_MANAGE")
    // @Operation(summary = "Replace image URLs", description = "Replace product imageUrl list directly")
    // public Object replaceProductImageUrls(@PathVariable String id, @RequestBody List<String> imageUrl) {
    //     return productService.replaceProductImages(id, imageUrl);
    // }

    @DeleteMapping("/{id}")
    @ApiResponse(message = "Product deleted successfully")
    @SecuredEndpoint("PRODUCT_MANAGE")
    @Operation(summary = "Delete product", description = "Delete a product")
    public void deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
    }
}
