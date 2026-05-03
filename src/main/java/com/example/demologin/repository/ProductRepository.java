package com.example.demologin.repository;

import com.example.demologin.enums.ProductCategory;
import com.example.demologin.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

		@Query("""
						SELECT p
						FROM Product p
						WHERE (p.status IS NULL OR p.status != 'INACTIVE')
							AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.id) LIKE LOWER(CONCAT('%', :search, '%')))
							AND (:category IS NULL OR p.category = :category)
						""")
		Page<Product> searchProducts(@Param("search") String search,
																 @Param("category") ProductCategory category,
																 Pageable pageable);
}
