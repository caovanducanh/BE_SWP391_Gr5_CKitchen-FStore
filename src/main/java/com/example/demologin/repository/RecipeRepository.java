package com.example.demologin.repository;

import com.example.demologin.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Integer> {
	boolean existsByProduct_IdAndIngredient_Id(String productId, String ingredientId);

	List<Recipe> findAllByProduct_Id(String productId);

    @Modifying
    @Query("delete from Recipe r where r.ingredient.id in :ids")
    void deleteByIngredient_IdIn(@Param("ids") List<String> ids);
}
