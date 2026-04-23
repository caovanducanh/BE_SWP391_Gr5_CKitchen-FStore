package com.example.demologin.initializer.components;

import com.example.demologin.entity.Ingredient;
import com.example.demologin.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IngredientInitializer {

    private final IngredientRepository ingredientRepository;

    @Transactional
    public void initializeIngredients() {
        // We only initialize if we haven't reached our targeted diverse baking list count
        if (ingredientRepository.count() >= 50 && ingredientRepository.existsById("BAKE001")) {
            log.info("⏭️ Baking ingredients already initialized. Skipping...");
            return;
        }

        log.info("Creating diverse baking ingredients list...");

        List<Ingredient> ingredients = new ArrayList<>();

        // 1. Flours & Grains (Bột & Ngũ cốc)
        addIfMissing(ingredients, "BAKE001", "Bột mì đa dụng (All-purpose)", "kg", 16000, "Wilmar", 50);
        addIfMissing(ingredients, "BAKE002", "Bột mì số 8 (Cake Flour)", "kg", 18000, "Wilmar", 20);
        addIfMissing(ingredients, "BAKE003", "Bột mì số 11 (Bread Flour)", "kg", 17000, "Wilmar", 50);
        addIfMissing(ingredients, "BAKE004", "Bột mì số 13 (High Protein)", "kg", 20000, "Wilmar", 50);
        addIfMissing(ingredients, "BAKE005", "Bột mì nguyên cám", "kg", 35000, "Mỹ", 10);
        addIfMissing(ingredients, "BAKE006", "Bột bắp", "kg", 18000, "Công ty Nông sản", 10);
        addIfMissing(ingredients, "BAKE007", "Bột năng", "kg", 16000, "Công ty Nông sản", 5);
        addIfMissing(ingredients, "BAKE008", "Bột nếp", "kg", 22000, "Công ty Nông sản", 5);
        addIfMissing(ingredients, "BAKE009", "Yến mạch cán dẹt", "kg", 45000, "Nhập khẩu", 10);
        addIfMissing(ingredients, "BAKE010", "Bột hạnh nhân", "kg", 380000, "Mỹ", 5);

        // 2. Sweeteners (Chất làm ngọt)
        addIfMissing(ingredients, "BAKE020", "Đường kính trắng", "kg", 20000, "Biên Hòa", 50);
        addIfMissing(ingredients, "BAKE021", "Đường bột (Icing Sugar)", "kg", 35000, "Biên Hòa", 20);
        addIfMissing(ingredients, "BAKE022", "Đường nâu", "kg", 45000, "Biên Hòa", 10);
        addIfMissing(ingredients, "BAKE023", "Mật ong nguyên chất", "chai", 150000, "Gia Lai", 5);
        addIfMissing(ingredients, "BAKE024", "Siro bắp (Corn Syrup)", "chai", 75000, "Hàn Quốc", 5);
        addIfMissing(ingredients, "BAKE025", "Nước đường bánh nướng", "kg", 120000, "Hỷ Lâm Môn", 10);
        addIfMissing(ingredients, "BAKE026", "Siro lá phong", "chai", 320000, "Canada", 2);

        // 3. Fats & Dairy (Chất béo & Sữa)
        addIfMissing(ingredients, "BAKE030", "Bơ lạt (Unsalted Butter)", "kg", 180000, "Anchor", 20);
        addIfMissing(ingredients, "BAKE031", "Bơ mặn (Salted Butter)", "kg", 190000, "Anchor", 5);
        addIfMissing(ingredients, "BAKE032", "Sữa tươi không đường", "l", 25000, "Vinamilk", 30);
        addIfMissing(ingredients, "BAKE033", "Whipping Cream", "l", 135000, "Anchor", 10);
        addIfMissing(ingredients, "BAKE034", "Topping Cream", "l", 75000, "Rich's", 10);
        addIfMissing(ingredients, "BAKE035", "Cream Cheese", "kg", 280000, "Philadelphia", 10);
        addIfMissing(ingredients, "BAKE036", "Phô mai Mascarpone", "hộp", 180000, "Tatua", 5);
        addIfMissing(ingredients, "BAKE037", "Sữa đặc", "lon", 24000, "Ông Thọ", 20);
        addIfMissing(ingredients, "BAKE038", "Sữa bột nguyên kem", "kg", 220000, "New Zealand", 5);

        // 4. Eggs (Trứng)
        addIfMissing(ingredients, "BAKE040", "Trứng gà tươi", "quả", 3500, "Ba Huân", 100);
        addIfMissing(ingredients, "BAKE041", "Lòng đỏ trứng muối", "quả", 8000, "Cơ sở uy tín", 50);

        // 5. Leavening & Additives (Chất tạo nở & Phụ gia)
        addIfMissing(ingredients, "BAKE050", "Men khô (Dry Yeast)", "kg", 125000, "Saf-Instant", 5);
        addIfMissing(ingredients, "BAKE051", "Bột nở (Baking Powder)", "kg", 85000, "Alsa", 5);
        addIfMissing(ingredients, "BAKE052", "Muối nở (Baking Soda)", "kg", 45000, "Arm & Hammer", 2);
        addIfMissing(ingredients, "BAKE053", "Gelatin lá", "xấp", 85000, "Đức", 2);
        addIfMissing(ingredients, "BAKE054", "Gelatin bột", "kg", 320000, "Pháp", 2);
        addIfMissing(ingredients, "BAKE055", "Bột Tartar", "hũ", 120000, "McCormick", 1);

        // 6. Nuts & Dried Fruits (Hạt & Trái cây khô)
        addIfMissing(ingredients, "BAKE060", "Hạnh nhân lát", "kg", 350000, "Mỹ", 5);
        addIfMissing(ingredients, "BAKE061", "Hạt óc chó vụn", "kg", 420000, "Mỹ", 3);
        addIfMissing(ingredients, "BAKE062", "Hạt dẻ cười", "kg", 550000, "Mỹ", 2);
        addIfMissing(ingredients, "BAKE063", "Nho khô đen", "kg", 180000, "Mỹ", 5);
        addIfMissing(ingredients, "BAKE064", "Việt quất khô", "kg", 480000, "Mỹ", 3);
        addIfMissing(ingredients, "BAKE065", "Mứt dâu tây", "hũ", 45000, "Đà Lạt", 10);
        addIfMissing(ingredients, "BAKE066", "Mứt việt quất", "hũ", 55000, "Đà Lạt", 5);

        // 7. Flavorings & Colors (Hương vị & Màu sắc)
        addIfMissing(ingredients, "BAKE070", "Tinh chất Vani", "chai", 160000, "Rayner's", 5);
        addIfMissing(ingredients, "BAKE071", "Bột Matcha Nhật", "kg", 850000, "Meiko", 2);
        addIfMissing(ingredients, "BAKE072", "Bột quế", "kg", 250000, "Yên Bái", 2);
        addIfMissing(ingredients, "BAKE073", "Màu thực phẩm đỏ", "chai", 35000, "AmeriColor", 2);
        addIfMissing(ingredients, "BAKE074", "Màu thực phẩm xanh", "chai", 35000, "AmeriColor", 2);

        // 8. Chocolate & Cocoa (Socola & Cacao)
        addIfMissing(ingredients, "BAKE080", "Bột Cacao nguyên chất", "kg", 180000, "Daklak", 10);
        addIfMissing(ingredients, "BAKE081", "Socola Chip đen", "kg", 220000, "Grand-Place", 10);
        addIfMissing(ingredients, "BAKE082", "Socola Chip trắng", "kg", 240000, "Grand-Place", 5);
        addIfMissing(ingredients, "BAKE083", "Socola thanh 70%", "kg", 350000, "Marou", 5);

        if (!ingredients.isEmpty()) {
            ingredientRepository.saveAll(ingredients);
            log.info("✅ Created {} bakery-specific ingredients", ingredients.size());
        }
    }

    private void addIfMissing(List<Ingredient> list, String id, String name, String unit, double price, String supplier, int minStock) {
        if (!ingredientRepository.existsById(id)) {
            list.add(createIngredient(id, name, unit, price, supplier, minStock));
        }
    }

    private Ingredient createIngredient(String id, String name, String unit, double price, String supplier, int minStock) {
        return Ingredient.builder()
                .id(id)
                .name(name)
                .unit(unit)
                .price(BigDecimal.valueOf(price))
                .supplier(supplier)
                .minStock(minStock)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
