package com.skilltree.skilltreebackend.service.impl;

import com.skilltree.skilltreebackend.model.User;
import com.skilltree.skilltreebackend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * UserService implementation for managing User CRUD, authentication,
 * and validation using MongoDB.
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;


    /****************************************************
     * CREATE USER
     ****************************************************/
    @Override
    public User createUser(User user) {

        if (user == null)
            throw new IllegalArgumentException("User object cannot be null.");

        if (user.getUsername() == null || user.getUsername().isBlank())
            throw new IllegalArgumentException("Username is required.");

        if (user.getEmail() == null || user.getEmail().isBlank())
            throw new IllegalArgumentException("Email is required.");

        if (user.getPassword() == null || user.getPassword().isBlank())
            throw new IllegalArgumentException("Password is required.");

        // Normalize
        user.setEmail(user.getEmail().toLowerCase());

        // Hash password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Timestamps
        user.setCreatedAt(LocalDateTime.now());
        user.setLastLogin(null);

        return mongoTemplate.save(user);
    }


    /****************************************************
     * READ USERS
     ****************************************************/
    @Override
    public List<User> getAllUsers() {
        return mongoTemplate.findAll(User.class);
    }

    @Override
    public User getUserById(String id) {
        if (id == null || id.isBlank()) return null;
        return mongoTemplate.findById(id, User.class);
    }


    /****************************************************
     * UPDATE USER
     ****************************************************/
    @Override
    public User updateUser(String id, User updated) {
        if (id == null || id.isBlank()) return null;

        User existingUser = getUserById(id);
        if (existingUser == null)
            return null;

        if (updated.getUsername() != null)
            existingUser.setUsername(updated.getUsername());

        if (updated.getEmail() != null)
            existingUser.setEmail(updated.getEmail().toLowerCase());

        if (updated.getPassword() != null)
            existingUser.setPassword(passwordEncoder.encode(updated.getPassword()));

        if (updated.getDateOfBirth() != null)
            existingUser.setDateOfBirth(updated.getDateOfBirth());

        // Update last modified timestamp
        existingUser.setLastLogin(LocalDateTime.now());

        return mongoTemplate.save(existingUser);
    }


    /****************************************************
     * DELETE
     ****************************************************/
    @Override
    public void deleteUser(String id) {
        if (id == null || id.isBlank()) return;
        User user = getUserById(id);
        if (user != null)
            mongoTemplate.remove(user);
    }


    /****************************************************
     * FIND BY USERNAME / EMAIL
     ****************************************************/
    @Override
    public User findByUsername(String username) {
        return mongoTemplate.findOne(
                new Query(Criteria.where("username").is(username)),
                User.class
        );
    }

    @Override
    public User findByEmail(String email) {
        if (email == null || email.isBlank()) return null;

        String lowerEmail = Objects.requireNonNull(email).toLowerCase(Locale.ROOT);
        return mongoTemplate.findOne(
                new Query(Criteria.where("email").is(lowerEmail)),
                User.class
        );
    }


    /****************************************************
     * LOGIN
     ****************************************************/
    @Override
    public User login(String email, String password) {

        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Email cannot be empty.");

        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password cannot be empty.");

        User user = findByEmail(email);

        if (user == null)
            throw new RuntimeException("No user found with this email.");

        if (!passwordEncoder.matches(password, user.getPassword()))
            throw new RuntimeException("Invalid password.");

        // update last login time
        user.setLastLogin(LocalDateTime.now());
        mongoTemplate.save(user);

        return user;
    }


    /****************************************************
     * CHECK EMAIL EXISTS
     ****************************************************/
    @Override
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        String lowerEmail = email.toLowerCase();
        Query query = new Query(Criteria.where("email").is(lowerEmail));
        return mongoTemplate.exists(query, User.class);
    }
}