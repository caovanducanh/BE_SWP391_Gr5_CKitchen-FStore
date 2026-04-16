package com.example.demologin.initializer;

import com.example.demologin.initializer.components.DefaultUserInitializer;
import com.example.demologin.initializer.components.IngredientInitializer;
import com.example.demologin.initializer.components.ManagerDashboardDataInitializer;
import com.example.demologin.initializer.components.PermissionRoleInitializer;
import com.example.demologin.initializer.components.ProductInitializer;
import com.example.demologin.initializer.components.RecipeInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Main Data Initializer - Orchestrates all initialization processes
 * 
 * This class coordinates the execution of all data initialization components
 * in the correct order to ensure system integrity and proper dependencies.
 * 
 * Execution Order:
 * 1. PermissionRoleInitializer - Creates permissions and roles
 * 2. DefaultUserInitializer - Creates default users with assigned roles
 * 3. IngredientInitializer - Creates raw materials
 * 4. ProductInitializer - Creates products
 * 5. RecipeInitializer - Creates relationships between products and ingredients
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Ensure this runs first among all CommandLineRunners
public class MainDataInitializer implements CommandLineRunner {

    private final PermissionRoleInitializer permissionRoleInitializer;
    private final DefaultUserInitializer defaultUserInitializer;
    private final IngredientInitializer ingredientInitializer;
    private final ProductInitializer productInitializer;
    private final RecipeInitializer recipeInitializer;
    private final ManagerDashboardDataInitializer managerDashboardDataInitializer;

    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 Starting Main Data Initialization Process...");
        
        try {
            // Step 1: Initialize Permissions and Roles
            log.info("📋 Step 1: Initializing Permissions and Roles...");
            permissionRoleInitializer.initializePermissionsAndRoles();
            log.info("✅ Permissions and Roles initialization completed");
            
            // Step 2: Initialize Default Users
            log.info("👥 Step 2: Initializing Default Users...");
            defaultUserInitializer.initializeDefaultUsers();
            log.info("✅ Default Users initialization completed");
            
            // Step 3: Initialize Ingredients
            log.info("📦 Step 3: Initializing Ingredients...");
            ingredientInitializer.initializeIngredients();
            log.info("✅ Ingredients initialization completed");

            // Step 4: Initialize Products
            log.info("🍎 Step 4: Initializing Products...");
            productInitializer.initializeProducts();
            log.info("✅ Products initialization completed");

            // Step 5: Initialize Recipes
            log.info("📝 Step 5: Initializing Recipes...");
            recipeInitializer.initializeRecipes();
            log.info("✅ Recipes initialization completed");

            // Step 6: Initialize Manager Dashboard Data
            log.info("📊 Step 6: Initializing Manager Dashboard Data...");
            managerDashboardDataInitializer.initializeManagerDashboardData();
            log.info("✅ Manager Dashboard Data initialization completed");
            
            log.info("🎉 Main Data Initialization Process completed successfully!");
            
        } catch (Exception e) {
            log.error("❌ Error during data initialization: {}", e.getMessage(), e);
            throw e; // Re-throw to prevent application startup with incomplete data
        }
    }
}
