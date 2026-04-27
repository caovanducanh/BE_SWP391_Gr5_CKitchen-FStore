package com.example.demologin.validator;

import com.example.demologin.annotation.StrongPassword;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    
    private int minLength;
    private boolean requireUppercase;
    private boolean requireLowercase;
    private boolean requireDigit;
    private boolean requireSpecialChar;
    
    
    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.minLength = constraintAnnotation.minLength();
        this.requireUppercase = constraintAnnotation.requireUppercase();
        this.requireLowercase = constraintAnnotation.requireLowercase();
        this.requireDigit = constraintAnnotation.requireDigit();
        this.requireSpecialChar = constraintAnnotation.requireSpecialChar();
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        
        // Check minimum length
        if (password.length() < minLength) {
            updateErrorMessage(context, "Password must be at least " + minLength + " characters long");
            return false;
        }
        
        
        // Check uppercase requirement
        if (requireUppercase && !Pattern.compile("[A-Z]").matcher(password).find()) {
            updateErrorMessage(context, "Password must contain at least one uppercase letter");
            return false;
        }
        
        // Check lowercase requirement
        if (requireLowercase && !Pattern.compile("[a-z]").matcher(password).find()) {
            updateErrorMessage(context, "Password must contain at least one lowercase letter");
            return false;
        }
        
        // Check digit requirement
        if (requireDigit && !Pattern.compile("[0-9]").matcher(password).find()) {
            updateErrorMessage(context, "Password must contain at least one digit");
            return false;
        }
        
        // Check special character requirement
        if (requireSpecialChar && !Pattern.compile("[^a-zA-Z0-9]").matcher(password).find()) {
            updateErrorMessage(context, "Password must contain at least one special character");
            return false;
        }
        
        return true;
    }
    
    private void updateErrorMessage(ConstraintValidatorContext context, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
    }
}
