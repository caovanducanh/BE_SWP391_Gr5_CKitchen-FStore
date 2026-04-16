package com.example.demologin.service;

import com.example.demologin.dto.request.product.CreateProductRequest;
import com.example.demologin.dto.request.product.UpdateProductRequest;
import com.example.demologin.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    Page<ProductResponse> getAllProducts(int page, int size);

    ProductResponse getProductById(String id);

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(String id, UpdateProductRequest request);

    ProductResponse uploadProductImages(String id, MultipartFile[] images);

    ProductResponse deleteProductImage(String id, String imageUrl);

    ProductResponse replaceProductImages(String id, List<String> imageUrl);

    void deleteProduct(String id);
}
