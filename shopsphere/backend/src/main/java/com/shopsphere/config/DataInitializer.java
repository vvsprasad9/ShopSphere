package com.shopsphere.config;

import com.shopsphere.entity.Cart;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.Wishlist;
import com.shopsphere.entity.enums.ERole;
import com.shopsphere.entity.enums.UserStatus;
import com.shopsphere.repository.CartRepository;
import com.shopsphere.repository.RoleRepository;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.repository.WishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final WishlistRepository wishlistRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@shopsphere.com}")
    private String adminEmail;

    @Value("${app.admin.password:AdminSecure@123}")
    private String adminPassword;

    @Value("${app.admin.first-name:System}")
    private String adminFirstName;

    @Value("${app.admin.last-name:Admin}")
    private String adminLastName;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            CartRepository cartRepository,
            WishlistRepository wishlistRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.wishlistRepository = wishlistRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Seed default roles if absent
        Role customerRole = initRole(ERole.ROLE_CUSTOMER);
        Role adminRole = initRole(ERole.ROLE_ADMIN);
        Role managerRole = initRole(ERole.ROLE_MANAGER);

        // Seed super admin account from environment configuration
        if (!userRepository.existsByEmail(adminEmail.trim().toLowerCase())) {
            log.info("Bootstrapping initial system admin account: {}", adminEmail);

            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(customerRole);
            adminRoles.add(adminRole);
            adminRoles.add(managerRole);

            User admin = User.builder()
                    .email(adminEmail.trim().toLowerCase())
                    .password(passwordEncoder.encode(adminPassword))
                    .firstName(adminFirstName)
                    .lastName(adminLastName)
                    .phone("+1000000000")
                    .status(UserStatus.ACTIVE)
                    .emailVerified(true)
                    .roles(adminRoles)
                    .build();

            User savedAdmin = userRepository.save(admin);
            cartRepository.save(Cart.builder().user(savedAdmin).build());
            wishlistRepository.save(Wishlist.builder().user(savedAdmin).build());
            log.info("System admin account provisioned successfully.");
        }
    }

    private Role initRole(ERole roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    log.info("Initializing role: {}", roleName);
                    return roleRepository.save(new Role(roleName));
                });
    }
}
