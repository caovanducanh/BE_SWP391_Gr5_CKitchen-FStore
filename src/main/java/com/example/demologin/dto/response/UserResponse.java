package com.example.demologin.dto.response;

import com.example.demologin.entity.User;
import com.example.demologin.enums.Gender;
import com.example.demologin.enums.UserStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private LocalDate dateOfBirth;
    private String identityCard;
    private Gender gender;
    private UserStatus status;
    private LocalDateTime createdDate;
    private String storeId;
    private String storeName;
    private String storeAddress;
    private String storePhone;
    
    private String kitchenId;
    private String kitchenName;
    private String kitchenAddress;
    private String kitchenPhone;

    private String token;
    private String refreshToken;

    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(null)
                .address(null)
                .dateOfBirth(null)
                .identityCard(null)
                .gender(null)
                .status(user.getStatus())
                .createdDate(user.getCreatedAt())
                .storeId(user.getStore() != null ? user.getStore().getId() : null)
                .storeName(user.getStore() != null ? user.getStore().getName() : null)
                .storeAddress(user.getStore() != null ? user.getStore().getAddress() : null)
                .storePhone(user.getStore() != null ? user.getStore().getPhone() : null)
                .kitchenId(user.getKitchen() != null ? user.getKitchen().getId() : null)
                .kitchenName(user.getKitchen() != null ? user.getKitchen().getName() : null)
                .kitchenAddress(user.getKitchen() != null ? user.getKitchen().getAddress() : null)
                .kitchenPhone(user.getKitchen() != null ? user.getKitchen().getPhone() : null)
                .build();
    }
}
