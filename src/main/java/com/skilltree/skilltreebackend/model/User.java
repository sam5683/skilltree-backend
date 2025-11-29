package com.skilltree.skilltreebackend.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;  // For explicit JSON binding

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a User entity stored in the MongoDB 'users' collection.
 * Uses Lombok for reducing boilerplate and ensures unique username/email.
 * Includes validation and timestamps for tracking.
 */
@Data
@NoArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;

    @NotBlank(message = "Username is required")
    @Indexed(unique = true)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Indexed(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    // REMOVED @JsonIgnore here – allows binding from incoming JSON requests (deserialization)
    // Controller will null it before responses
    private String password;

    @Transient // This field won't be saved to MongoDB
    @JsonProperty("passwordConfirm")  // Explicit binding for incoming JSON (deserialization)
    // No @JsonIgnore – allows request binding; ignored in responses via controller
    private String passwordConfirm;
    
    @NotBlank(message = "Full name is required")
    private String fullName;

    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private boolean isActive = true;

    /**
     * Constructor with essential fields, auto-sets createdAt.
     * @param username User's unique username
     * @param email User's unique email (case-insensitive)
     * @param password User's password (to be hashed)
     * @param dateOfBirth User's birth date
     */
    /**
     * Constructor with essential fields, auto-sets createdAt.
     */
    public User(String username, String email, String password, String fullName, LocalDate dateOfBirth) {
        this.username = username;
        this.email = email.toLowerCase();
        this.password = password;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }

    /**
     * Checks if password and confirmation match
     */
    @Transient
    public boolean isPasswordMatching() {
        return password != null && password.equals(passwordConfirm);
    }
} 
    
