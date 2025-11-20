package com.shopping.service;

import com.shopping.dto.RegisterRequest;
import com.shopping.entity.Cart;
import com.shopping.entity.User;
import com.shopping.repository.CartRepository;
import com.shopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest request) {
        // 1. Email duplicate check
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new RuntimeException("Email already exists: " + request.email());
        }

        // 2. Create User
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPhone(request.phone());

        // 3. Set Role: First user = ADMIN
        long userCount = userRepository.count();
        user.setRole(userCount == 0 ? "ADMIN" : "USER");

        // 4. Encode password
        user.setPassword(passwordEncoder.encode(request.password()));

        // 5. SAVE USER FIRST (IMPORTANT!)
        user = userRepository.save(user);

        // 6. Now create Cart
        Cart cart = new Cart();
        cart.setUser(user);  // Now user has ID
        cartRepository.save(cart);

        // 7. Link back (optional, for bidirectional)
        user.setCart(cart);

        return user;
    }
}