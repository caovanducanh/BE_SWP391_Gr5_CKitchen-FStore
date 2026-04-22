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
        if (ingredientRepository.count() >= 200) {
            log.info("⏭️ Ingredients already fully initialized. Skipping...");
            return;
        }

        log.info("Checking and creating missing ingredients (up to 200 items)...");

        List<Ingredient> ingredients = new ArrayList<>();

        // Group 1: Base & Grains (ING001 - ING020)
        addIfMissing(ingredients, "ING001", "Bột mì", "kg", 15000, "Công ty Bột Mì", 10);
        addIfMissing(ingredients, "ING002", "Đường", "kg", 20000, "Công ty Đường", 5);
        addIfMissing(ingredients, "ING003", "Sữa tươi", "l", 25000, "Vinamilk", 20);
        addIfMissing(ingredients, "ING004", "Trứng gà", "quả", 3000, "Trang trại", 50);
        addIfMissing(ingredients, "ING005", "Bơ", "kg", 150000, "Nhập khẩu", 2);
        addIfMissing(ingredients, "ING006", "Bột bắp", "kg", 18000, "Công ty Nông sản", 5);
        addIfMissing(ingredients, "ING007", "Bột năng", "kg", 16000, "Công ty Nông sản", 5);
        addIfMissing(ingredients, "ING008", "Gạo tẻ", "kg", 18000, "VinaFood", 50);
        addIfMissing(ingredients, "ING009", "Gạo nếp", "kg", 25000, "VinaFood", 20);
        addIfMissing(ingredients, "ING010", "Yến mạch", "kg", 45000, "Nhập khẩu", 5);
        addIfMissing(ingredients, "ING011", "Bột chiên giòn", "gói", 12000, "Ajinomoto", 10);
        addIfMissing(ingredients, "ING012", "Bột chiên xù", "gói", 10000, "Ajinomoto", 10);
        addIfMissing(ingredients, "ING013", "Bột gạo", "kg", 15000, "Công ty Nông sản", 10);
        addIfMissing(ingredients, "ING014", "Bánh tráng", "xấp", 5000, "Làng nghề", 20);
        addIfMissing(ingredients, "ING015", "Mì tôm", "gói", 4000, "Acecook", 100);
        addIfMissing(ingredients, "ING016", "Phở tươi", "kg", 15000, "Lò bún", 10);
        addIfMissing(ingredients, "ING017", "Bún tươi", "kg", 12000, "Lò bún", 15);
        addIfMissing(ingredients, "ING018", "Miến dong", "kg", 60000, "Đặc sản", 5);
        addIfMissing(ingredients, "ING019", "Nui ống", "gói", 18000, "Safoco", 10);
        addIfMissing(ingredients, "ING020", "Bột nếp", "kg", 20000, "Công ty Nông sản", 5);

        // Group 2: Vegetables (ING021 - ING060)
        addIfMissing(ingredients, "ING021", "Hành tây", "kg", 25000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING022", "Tỏi", "kg", 60000, "Lý Sơn", 2);
        addIfMissing(ingredients, "ING023", "Gừng", "kg", 40000, "Chợ đầu mối", 1);
        addIfMissing(ingredients, "ING024", "Ớt cay", "kg", 50000, "Chợ đầu mối", 1);
        addIfMissing(ingredients, "ING025", "Khoai tây", "kg", 22000, "Đà Lạt", 10);
        addIfMissing(ingredients, "ING026", "Cà rốt", "kg", 18000, "Đà Lạt", 10);
        addIfMissing(ingredients, "ING027", "Cà chua", "kg", 20000, "Đà Lạt", 5);
        addIfMissing(ingredients, "ING028", "Súp lơ xanh", "cái", 15000, "Đà Lạt", 5);
        addIfMissing(ingredients, "ING029", "Súp lơ trắng", "cái", 14000, "Đà Lạt", 5);
        addIfMissing(ingredients, "ING030", "Bắp cải", "kg", 12000, "Đà Lạt", 10);
        addIfMissing(ingredients, "ING031", "Cải thảo", "kg", 15000, "Đà Lạt", 10);
        addIfMissing(ingredients, "ING032", "Cải thìa", "kg", 18000, "Chợ đầu mối", 8);
        addIfMissing(ingredients, "ING033", "Rau muống", "bó", 7000, "Vườn rau", 20);
        addIfMissing(ingredients, "ING034", "Rau dền", "bó", 6000, "Vườn rau", 10);
        addIfMissing(ingredients, "ING035", "Mồng tơi", "bó", 6000, "Vườn rau", 10);
        addIfMissing(ingredients, "ING036", "Cải ngọt", "kg", 15000, "Chợ đầu mối", 10);
        addIfMissing(ingredients, "ING037", "Đậu bắp", "kg", 25000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING038", "Bí đỏ", "kg", 15000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING039", "Bí xanh", "kg", 12000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING040", "Đậu cove", "kg", 28000, "Đà Lạt", 5);
        addIfMissing(ingredients, "ING041", "Dưa leo", "kg", 15000, "Chợ đầu mối", 10);
        addIfMissing(ingredients, "ING042", "Hành lá", "kg", 30000, "Vườn rau", 2);
        addIfMissing(ingredients, "ING043", "Ngò rí", "kg", 40000, "Vườn rau", 1);
        addIfMissing(ingredients, "ING044", "Ớt chuông đỏ", "kg", 45000, "Đà Lạt", 2);
        addIfMissing(ingredients, "ING045", "Ớt chuông xanh", "kg", 40000, "Đà Lạt", 2);
        addIfMissing(ingredients, "ING046", "Ớt chuông vàng", "kg", 45000, "Đà Lạt", 2);
        addIfMissing(ingredients, "ING047", "Bắp ngô", "bắp", 5000, "Nông trại", 20);
        addIfMissing(ingredients, "ING048", "Xà lách", "kg", 35000, "Đà Lạt", 3);
        addIfMissing(ingredients, "ING049", "Giá đỗ", "kg", 10000, "Lò giá", 5);
        addIfMissing(ingredients, "ING050", "Khổ qua", "kg", 22000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING051", "Củ năng", "kg", 40000, "Chợ đầu mối", 2);
        addIfMissing(ingredients, "ING052", "Củ sen", "kg", 60000, "Chợ đầu mối", 2);
        addIfMissing(ingredients, "ING053", "Khoai môn", "kg", 35000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING054", "Khoai lang", "kg", 20000, "Chợ đầu mối", 10);
        addIfMissing(ingredients, "ING055", "Rau chân vịt", "kg", 45000, "Đà Lạt", 2);
        addIfMissing(ingredients, "ING056", "Sả", "kg", 20000, "Chợ đầu mối", 3);
        addIfMissing(ingredients, "ING057", "Rau răm", "bó", 5000, "Vườn rau", 5);
        addIfMissing(ingredients, "ING058", "Lá lốt", "bó", 5000, "Vườn rau", 5);
        addIfMissing(ingredients, "ING059", "Tía tô", "bó", 5000, "Vườn rau", 5);
        addIfMissing(ingredients, "ING060", "Kinh giới", "bó", 5000, "Vườn rau", 5);

        // Group 3: Fruits (ING061 - ING090)
        addIfMissing(ingredients, "ING061", "Chanh tươi", "kg", 30000, "Chợ đầu mối", 2);
        addIfMissing(ingredients, "ING062", "Cam sành", "kg", 35000, "Miền Tây", 5);
        addIfMissing(ingredients, "ING063", "Táo Mỹ", "kg", 80000, "Nhập khẩu", 5);
        addIfMissing(ingredients, "ING064", "Chuối sứ", "nải", 20000, "Miền Tây", 5);
        addIfMissing(ingredients, "ING065", "Xoài cát", "kg", 50000, "Hòa Lộc", 5);
        addIfMissing(ingredients, "ING066", "Đu đủ", "kg", 20000, "Miền Tây", 3);
        addIfMissing(ingredients, "ING067", "Dứa", "quả", 12000, "Tiền Giang", 5);
        addIfMissing(ingredients, "ING068", "Thanh long", "kg", 25000, "Bình Thuận", 5);
        addIfMissing(ingredients, "ING069", "Bưởi Da Xanh", "quả", 60000, "Miền Tây", 3);
        addIfMissing(ingredients, "ING070", "Dâu tây", "kg", 180000, "Đà Lạt", 1);
        addIfMissing(ingredients, "ING071", "Nho đen", "kg", 120000, "Nhập khẩu", 2);
        addIfMissing(ingredients, "ING072", "Kiwi", "kg", 150000, "Nhập khẩu", 1);
        addIfMissing(ingredients, "ING073", "Việt quất", "hộp", 80000, "Nhập khẩu", 2);
        addIfMissing(ingredients, "ING074", "Bơ sáp", "kg", 45000, "Đắk Lắk", 3);
        addIfMissing(ingredients, "ING075", "Mít thái", "kg", 30000, "Miền Tây", 5);
        addIfMissing(ingredients, "ING076", "Vải thiều", "kg", 40000, "Bắc Giang", 10);
        addIfMissing(ingredients, "ING077", "Nhãn xuồng", "kg", 55000, "Vũng Tàu", 5);
        addIfMissing(ingredients, "ING078", "Măng cụt", "kg", 70000, "Miền Tây", 5);
        addIfMissing(ingredients, "ING079", "Chôm chôm", "kg", 30000, "Miền Tây", 10);
        addIfMissing(ingredients, "ING080", "Na (Mãng cầu ta)", "kg", 65000, "Tây Ninh", 5);
        addIfMissing(ingredients, "ING081", "Mãng cầu xiêm", "kg", 45000, "Miền Tây", 3);
        addIfMissing(ingredients, "ING082", "Dưa hấu", "kg", 15000, "Long An", 10);
        addIfMissing(ingredients, "ING083", "Lê Nam Phi", "kg", 60000, "Nhập khẩu", 5);
        addIfMissing(ingredients, "ING084", "Hồng giòn", "kg", 40000, "Đà Lạt", 5);
        addIfMissing(ingredients, "ING085", "Sơ ri", "kg", 25000, "Gò Công", 3);
        addIfMissing(ingredients, "ING086", "Cóc non", "kg", 20000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING087", "Me thái", "kg", 90000, "Nhập khẩu", 2);
        addIfMissing(ingredients, "ING088", "Ổi nữ hoàng", "kg", 18000, "Miền Tây", 5);
        addIfMissing(ingredients, "ING089", "Lựu Đỏ", "kg", 75000, "Nhập khẩu", 2);
        addIfMissing(ingredients, "ING090", "Đào", "kg", 55000, "Sa Pa", 3);

        // Group 4: Meats (ING091 - ING120)
        addIfMissing(ingredients, "ING091", "Thịt heo ba chỉ", "kg", 140000, "Vissan", 10);
        addIfMissing(ingredients, "ING092", "Thịt heo nạc", "kg", 130000, "Vissan", 10);
        addIfMissing(ingredients, "ING093", "Sườn heo", "kg", 160000, "Vissan", 5);
        addIfMissing(ingredients, "ING094", "Thịt bò thăn", "kg", 280000, "Bò Việt", 5);
        addIfMissing(ingredients, "ING095", "Bắp bò", "kg", 240000, "Bò Việt", 5);
        addIfMissing(ingredients, "ING096", "Thịt gà đùi", "kg", 80000, "CP Foods", 15);
        addIfMissing(ingredients, "ING097", "Ức gà", "kg", 75000, "CP Foods", 10);
        addIfMissing(ingredients, "ING098", "Cánh gà", "kg", 95000, "CP Foods", 10);
        addIfMissing(ingredients, "ING099", "Thịt vịt", "kg", 70000, "Trang trại", 5);
        addIfMissing(ingredients, "ING100", "Thịt cừu", "kg", 350000, "Nhập khẩu", 2);
        addIfMissing(ingredients, "ING101", "Giò sống", "kg", 150000, "Vissan", 3);
        addIfMissing(ingredients, "ING102", "Chả lụa", "kg", 180000, "Cơ sở uy tín", 2);
        addIfMissing(ingredients, "ING103", "Thịt bò xay", "kg", 220000, "Vissan", 5);
        addIfMissing(ingredients, "ING104", "Thịt heo xay", "kg", 120000, "Vissan", 10);
        addIfMissing(ingredients, "ING105", "Chân giò heo", "kg", 110000, "Vissan", 5);
        addIfMissing(ingredients, "ING106", "Tim heo", "kg", 180000, "Vissan", 2);
        addIfMissing(ingredients, "ING107", "Gan heo", "kg", 60000, "Vissan", 2);
        addIfMissing(ingredients, "ING108", "Đuôi bò", "kg", 200000, "Bò Việt", 3);
        addIfMissing(ingredients, "ING109", "Gà ta nguyên con", "kg", 140000, "Trang trại", 5);
        addIfMissing(ingredients, "ING110", "Dồi trường", "kg", 250000, "Chợ đầu mối", 2);
        addIfMissing(ingredients, "ING111", "Mề gà", "kg", 85000, "CP Foods", 3);
        addIfMissing(ingredients, "ING112", "Cốt lết heo", "kg", 125000, "Vissan", 5);
        addIfMissing(ingredients, "ING113", "Bò viên", "kg", 160000, "Cơ sở uy tín", 5);
        addIfMissing(ingredients, "ING114", "Cá viên", "kg", 90000, "Cơ sở uy tín", 5);
        addIfMissing(ingredients, "ING115", "Tôm viên", "kg", 110000, "Cơ sở uy tín", 3);
        addIfMissing(ingredients, "ING116", "Trứng vịt", "quả", 4000, "Trang trại", 30);
        addIfMissing(ingredients, "ING117", "Trứng cút", "chục", 10000, "Trang trại", 10);
        addIfMissing(ingredients, "ING118", "Lạp xưởng", "kg", 200000, "Sóc Trăng", 2);
        addIfMissing(ingredients, "ING119", "Xúc xích", "gói", 45000, "Đức Việt", 10);
        addIfMissing(ingredients, "ING120", "Thịt heo quay", "kg", 250000, "Cơ sở uy tín", 3);

        // Group 5: Seafood (ING121 - ING150)
        addIfMissing(ingredients, "ING121", "Tôm sú", "kg", 350000, "Vũng Tàu", 5);
        addIfMissing(ingredients, "ING122", "Tôm thẻ", "kg", 220000, "Miền Tây", 10);
        addIfMissing(ingredients, "ING123", "Cua biển", "kg", 450000, "Cà Mau", 3);
        addIfMissing(ingredients, "ING124", "Cá lóc", "kg", 75000, "Miền Tây", 5);
        addIfMissing(ingredients, "ING125", "Cá diêu hồng", "kg", 65000, "Miền Tây", 5);
        addIfMissing(ingredients, "ING126", "Cá hồi Fillet", "kg", 550000, "Nhập khẩu", 2);
        addIfMissing(ingredients, "ING127", "Mực ống", "kg", 280000, "Phan Thiết", 5);
        addIfMissing(ingredients, "ING128", "Mực lá", "kg", 320000, "Phan Thiết", 3);
        addIfMissing(ingredients, "ING129", "Ngao trắng", "kg", 30000, "Chợ hải sản", 10);
        addIfMissing(ingredients, "ING130", "Sò huyết", "kg", 150000, "Chợ hải sản", 5);
        addIfMissing(ingredients, "ING131", "Ốc hương", "kg", 380000, "Chợ hải sản", 3);
        addIfMissing(ingredients, "ING132", "Cá thu", "kg", 180000, "Vũng Tàu", 3);
        addIfMissing(ingredients, "ING133", "Cá bớp", "kg", 250000, "Phan Thiết", 3);
        addIfMissing(ingredients, "ING134", "Cá ngừ", "kg", 120000, "Phú Yên", 5);
        addIfMissing(ingredients, "ING135", "Cá basa Fillet", "kg", 85000, "Miền Tây", 5);
        addIfMissing(ingredients, "ING136", "Cá cơm khô", "kg", 120000, "Phan Thiết", 2);
        addIfMissing(ingredients, "ING137", "Mực khô", "kg", 900000, "Phan Thiết", 1);
        addIfMissing(ingredients, "ING138", "Tôm khô", "kg", 800000, "Cà Mau", 1);
        addIfMissing(ingredients, "ING139", "Hàu sữa", "kg", 45000, "Long Sơn", 5);
        addIfMissing(ingredients, "ING140", "Tuộc", "kg", 160000, "Vung Tàu", 5);
        addIfMissing(ingredients, "ING141", "Cá linh", "kg", 150000, "Miền Tây", 2);
        addIfMissing(ingredients, "ING142", "Cá kèo", "kg", 140000, "Miền Tây", 3);
        addIfMissing(ingredients, "ING143", "Cá tầm", "kg", 280000, "Đà Lạt", 2);
        addIfMissing(ingredients, "ING144", "Cá trắm", "kg", 90000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING145", "Cá rô đồng", "kg", 110000, "Chợ đầu mối", 3);
        addIfMissing(ingredients, "ING146", "Lươn đồng", "kg", 220000, "Chợ đầu mối", 2);
        addIfMissing(ingredients, "ING147", "Ếch làm sẵn", "kg", 100000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING148", "Cá chạch", "kg", 180000, "Miền Tây", 2);
        addIfMissing(ingredients, "ING149", "Cá hú", "kg", 75000, "Miền Tây", 5);
        addIfMissing(ingredients, "ING150", "Chả cá thác lác", "kg", 260000, "Miền Tây", 2);

        // Group 6: Spices & Condiments (ING151 - ING180)
        addIfMissing(ingredients, "ING151", "Muối tinh", "kg", 8000, "Công ty Muối", 10);
        addIfMissing(ingredients, "ING152", "Hạt nêm", "kg", 65000, "Knorr", 5);
        addIfMissing(ingredients, "ING153", "Nước mắm Nam Ngư", "chai", 45000, "Masan", 10);
        addIfMissing(ingredients, "ING154", "Xì dầu", "chai", 18000, "Chinsu", 10);
        addIfMissing(ingredients, "ING155", "Tương ớt", "chai", 15000, "Chinsu", 10);
        addIfMissing(ingredients, "ING156", "Tương cà", "chai", 16000, "Cholimex", 5);
        addIfMissing(ingredients, "ING157", "Dầu hào", "chai", 25000, "Maggi", 5);
        addIfMissing(ingredients, "ING158", "Dầu ăn Tường An", "l", 48000, "Tường An", 20);
        addIfMissing(ingredients, "ING159", "Dầu mè", "chai", 55000, "Lee Kum Kee", 3);
        addIfMissing(ingredients, "ING160", "Ngũ vị hương", "gói", 5000, "Thiên Thành", 20);
        addIfMissing(ingredients, "ING161", "Bột quế", "gói", 12000, "Thiên Thành", 5);
        addIfMissing(ingredients, "ING162", "Tiêu đen hạt", "kg", 180000, "Đắk Lắk", 2);
        addIfMissing(ingredients, "ING163", "Đường phèn", "kg", 35000, "Quảng Ngãi", 5);
        addIfMissing(ingredients, "ING164", "Giấm gạo", "chai", 12000, "Cholimex", 5);
        addIfMissing(ingredients, "ING165", "Mật ong", "chai", 150000, "Gia Lai", 2);
        addIfMissing(ingredients, "ING166", "Mayonnaise", "chai", 35000, "Ajinomoto", 5);
        addIfMissing(ingredients, "ING167", "Sa tế", "hũ", 10000, "Cholimex", 5);
        addIfMissing(ingredients, "ING168", "Mắm tôm", "hũ", 15000, "Trung Thành", 5);
        addIfMissing(ingredients, "ING169", "Mắm nêm", "chai", 20000, "Thuận Phát", 5);
        addIfMissing(ingredients, "ING170", "Bột nghệ", "gói", 15000, "Thiên Thành", 5);
        addIfMissing(ingredients, "ING171", "Bột ớt Hàn Quốc", "kg", 160000, "Nhập khẩu", 2);
        addIfMissing(ingredients, "ING172", "Dầu điều", "chai", 20000, "Tự nấu", 2);
        addIfMissing(ingredients, "ING173", "Mù tạt xanh", "tuýp", 35000, "Nhập khẩu", 2);
        addIfMissing(ingredients, "ING174", "Bột canh", "gói", 5000, "Hải Châu", 20);
        addIfMissing(ingredients, "ING175", "Bột ngọt (Mì chính)", "kg", 55000, "Ajinomoto", 5);
        addIfMissing(ingredients, "ING176", "Nước cốt dừa", "lon", 22000, "Vietcoco", 10);
        addIfMissing(ingredients, "ING177", "Sốt BBQ", "chai", 65000, "Nhập khẩu", 3);
        addIfMissing(ingredients, "ING178", "Sốt Teriyaki", "chai", 55000, "Nhập khẩu", 3);
        addIfMissing(ingredients, "ING179", "Rượu Mai Quế Lộ", "chai", 45000, "Cơ sở uy tín", 2);
        addIfMissing(ingredients, "ING180", "Dấm tiều", "chai", 25000, "Chợ Lớn", 2);

        // Group 7: Dairy, Eggs & Others (ING181 - ING200)
        addIfMissing(ingredients, "ING181", "Sữa tươi có đường", "l", 26000, "Vinamilk", 10);
        addIfMissing(ingredients, "ING182", "Sữa đặc", "lon", 24000, "Ông Thọ", 10);
        addIfMissing(ingredients, "ING183", "Whipping cream", "hộp", 120000, "Anchor", 2);
        addIfMissing(ingredients, "ING184", "Phô mai Mozzarella", "kg", 220000, "Nhập khẩu", 2);
        addIfMissing(ingredients, "ING185", "Sữa chua không đường", "hộp", 6000, "Vinamilk", 20);
        addIfMissing(ingredients, "ING186", "Nấm hương khô", "kg", 350000, "Tây Bắc", 1);
        addIfMissing(ingredients, "ING187", "Mộc nhĩ", "kg", 180000, "Tây Bắc", 1);
        addIfMissing(ingredients, "ING188", "Nấm rơm", "kg", 90000, "Chợ đầu mối", 3);
        addIfMissing(ingredients, "ING189", "Nấm kim châm", "gói", 12000, "Hàn Quốc", 10);
        addIfMissing(ingredients, "ING190", "Đậu phụ", "miếng", 3000, "Lò đậu", 50);
        addIfMissing(ingredients, "ING191", "Rong biển khô", "gói", 35000, "Hàn Quốc", 5);
        addIfMissing(ingredients, "ING192", "Hạt điều", "kg", 250000, "Bình Phước", 2);
        addIfMissing(ingredients, "ING193", "Lạc nhân (Đậu phộng)", "kg", 60000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING194", "Mè trắng", "kg", 80000, "Chợ đầu mối", 2);
        addIfMissing(ingredients, "ING195", "Đậu xanh cà vỏ", "kg", 55000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING196", "Đậu đỏ", "kg", 45000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING197", "Dừa nạo", "kg", 30000, "Chợ đầu mối", 5);
        addIfMissing(ingredients, "ING198", "Táo tàu", "kg", 120000, "Nhập khẩu", 2);
        addIfMissing(ingredients, "ING199", "Kỷ tử", "kg", 250000, "Nhập khẩu", 1);
        addIfMissing(ingredients, "ING200", "Bột cacao", "kg", 180000, "Đắk Lắk", 2);

        if (!ingredients.isEmpty()) {
            ingredientRepository.saveAll(ingredients);
            log.info("✅ Created {} missing ingredients", ingredients.size());
        } else {
            log.info("✅ All ingredients already exist.");
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
