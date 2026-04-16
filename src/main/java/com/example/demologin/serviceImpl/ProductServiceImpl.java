package com.example.demologin.serviceImpl;

import com.example.demologin.dto.request.product.CreateProductRequest;
import com.example.demologin.dto.request.product.UpdateProductRequest;
import com.example.demologin.dto.response.ProductResponse;
import com.example.demologin.entity.Product;
import com.example.demologin.exception.exceptions.BadRequestException;
import com.example.demologin.exception.exceptions.InternalServerErrorException;
import com.example.demologin.exception.exceptions.NotFoundException;
import com.example.demologin.repository.ProductRepository;
import com.example.demologin.service.ProductService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_ID_PREFIX = "PROD";

    private final ProductRepository productRepository;

    @Value("${minio.endpoint}")
    private String minioEndpoint;

    @Value("${minio.access-key}")
    private String minioAccessKey;

    @Value("${minio.secret-key}")
    private String minioSecretKey;

    @Value("${minio.bucket-name}")
    private String minioBucketName;

    @Value("${minio.public-url}")
    private String minioPublicUrl;

    @Override
    public Page<ProductResponse> getAllProducts(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size)).map(this::toResponse);
    }

    @Override
    public ProductResponse getProductById(String id) {
        Product product = findProductById(id);
        return toResponse(product);
    }

    @Override
    public ProductResponse createProduct(CreateProductRequest request) {
        validatePriceAndCost(request.getPrice(), request.getCost());

        Product product = Product.builder()
                .id(generateProductId())
                .name(request.getName().trim())
                .category(request.getCategory().trim())
                .unit(request.getUnit().trim())
                .price(request.getPrice())
                .cost(request.getCost())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<String> uploadedImageUrls = uploadImagesToMinio(product.getId(), request.getImages());
        product.setImageUrl(uploadedImageUrls);

        Product savedProduct = productRepository.save(product);
        return toResponse(savedProduct);
    }

    @Override
    public ProductResponse updateProduct(String id, UpdateProductRequest request) {
        validatePriceAndCost(request.getPrice(), request.getCost());

        Product product = findProductById(id);
        product.setName(request.getName().trim());
        product.setCategory(request.getCategory().trim());
        product.setUnit(request.getUnit().trim());
        product.setPrice(request.getPrice());
        product.setCost(request.getCost());
        product.setUpdatedAt(LocalDateTime.now());

        List<String> uploadedImageUrls = uploadImagesToMinio(product.getId(), request.getImages());
        if (!uploadedImageUrls.isEmpty()) {
            product.setImageUrl(uploadedImageUrls);
        }

        Product updatedProduct = productRepository.save(product);
        return toResponse(updatedProduct);
    }

    @Override
    public ProductResponse uploadProductImages(String id, MultipartFile[] images) {
        if (images == null || images.length == 0) {
            throw new BadRequestException("At least one image file is required");
        }

        Product product = findProductById(id);
        List<String> uploadedImageUrls = uploadImagesToMinio(product.getId(), images);
        List<String> currentImageUrls = product.getImageUrl() == null ? new ArrayList<>() : new ArrayList<>(product.getImageUrl());
        currentImageUrls.addAll(uploadedImageUrls);

        product.setImageUrl(currentImageUrls);
        product.setUpdatedAt(LocalDateTime.now());

        Product updatedProduct = productRepository.save(product);
        return toResponse(updatedProduct);
    }

    @Override
    public ProductResponse deleteProductImage(String id, String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new BadRequestException("imageUrl is required");
        }

        Product product = findProductById(id);
        List<String> currentImageUrls = product.getImageUrl() == null
                ? new ArrayList<>()
                : new ArrayList<>(product.getImageUrl());

        String normalizedInput = imageUrl.trim();
        boolean removed = currentImageUrls.remove(normalizedInput);
        if (!removed) {
            throw new BadRequestException("Image URL is not linked to this product");
        }

        String objectName = extractObjectName(normalizedInput);
        if (objectName != null && !objectName.isBlank()) {
            try {
                MinioClient minioClient = buildMinioClient();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioBucketName)
                                .object(objectName)
                                .build()
                );
            } catch (Exception ex) {
                log.error("Failed to delete image from MinIO", ex);
                throw new InternalServerErrorException("Failed to delete image from MinIO");
            }
        }

        product.setImageUrl(currentImageUrls);
        product.setUpdatedAt(LocalDateTime.now());
        Product updatedProduct = productRepository.save(product);
        return toResponse(updatedProduct);
    }

    @Override
    public ProductResponse replaceProductImages(String id, List<String> imageUrl) {
        Product product = findProductById(id);
        List<String> normalizedUrls = imageUrl == null ? new ArrayList<>() : imageUrl.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .toList();

        product.setImageUrl(new ArrayList<>(normalizedUrls));
        product.setUpdatedAt(LocalDateTime.now());

        Product updatedProduct = productRepository.save(product);
        return toResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(String id) {
        Product product = findProductById(id);
        productRepository.delete(product);
    }

    private Product findProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }

    private void validatePriceAndCost(java.math.BigDecimal price, java.math.BigDecimal cost) {
        if (price == null || cost == null) {
            throw new BadRequestException("Price and cost are required");
        }

        if (price.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Price must be greater than 0");
        }

        if (cost.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Cost must be greater than 0");
        }
    }

    private String generateProductId() {
        int nextNumber = productRepository.findAll().stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .filter(id -> id.startsWith(PRODUCT_ID_PREFIX))
                .map(id -> id.substring(PRODUCT_ID_PREFIX.length()))
                .filter(suffix -> suffix.matches("\\d+"))
                .map(Integer::parseInt)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;

        return PRODUCT_ID_PREFIX + String.format("%06d", nextNumber);
    }

    private List<String> uploadImagesToMinio(String productId, MultipartFile[] imageFiles) {
        if (imageFiles == null || imageFiles.length == 0) {
            return new ArrayList<>();
        }

        MinioClient minioClient = buildMinioClient();
        List<String> uploadedUrls = new ArrayList<>();

        try {
            ensureBucketExists(minioClient);

            for (MultipartFile imageFile : imageFiles) {
                if (imageFile == null || imageFile.isEmpty()) {
                    continue;
                }
                validateImageFile(imageFile);

                String extension = getFileExtension(imageFile.getOriginalFilename());
                String objectName = productId + "/" + UUID.randomUUID() + extension;

                try (InputStream inputStream = imageFile.getInputStream()) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(minioBucketName)
                                    .object(objectName)
                                    .stream(inputStream, imageFile.getSize(), -1)
                                    .contentType(imageFile.getContentType())
                                    .build()
                    );
                }

                uploadedUrls.add(buildPublicImageUrl(objectName));
            }

            return uploadedUrls;
        } catch (Exception ex) {
            log.error("Failed to upload image to MinIO", ex);
            throw new InternalServerErrorException("Failed to upload images");
        }
    }

    private void ensureBucketExists(MinioClient minioClient) throws Exception {
        boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(minioBucketName)
                        .build()
        );

        if (!bucketExists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(minioBucketName)
                            .build()
            );
        }
    }

    private MinioClient buildMinioClient() {
        return MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKey, minioSecretKey)
                .build();
    }

    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }
    }

    private String getFileExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.'));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .unit(product.getUnit())
                .price(product.getPrice())
                .cost(product.getCost())
                .imageUrl(product.getImageUrl() == null ? new ArrayList<>() : product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    private String buildPublicImageUrl(String objectName) {
        String normalizedPublicUrl = minioPublicUrl.endsWith("/")
                ? minioPublicUrl.substring(0, minioPublicUrl.length() - 1)
                : minioPublicUrl;

        String normalizedImagePath = objectName.startsWith("/") ? objectName.substring(1) : objectName;

        return normalizedPublicUrl + "/" + minioBucketName + "/" + normalizedImagePath;
    }

    private String extractObjectName(String imageUrl) {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            String normalizedPublicUrl = minioPublicUrl.endsWith("/")
                    ? minioPublicUrl.substring(0, minioPublicUrl.length() - 1)
                    : minioPublicUrl;
            String prefix = normalizedPublicUrl + "/" + minioBucketName + "/";
            if (imageUrl.startsWith(prefix)) {
                return imageUrl.substring(prefix.length());
            }
            return null;
        }
        return imageUrl.startsWith("/") ? imageUrl.substring(1) : imageUrl;
    }
}
