package com.shopsphere.repository;

import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.entity.enums.ERole;
import com.shopsphere.entity.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        customerRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(ERole.ROLE_CUSTOMER)));
    }

    @Test
    @DisplayName("Should successfully persist user and query by email with roles")
    void testSaveAndFindByEmailWithRoles() {
        User user = User.builder()
                .email("test.customer@shopsphere.com")
                .password("$2a$10$encodedpasswordhash")
                .firstName("John")
                .lastName("Doe")
                .phone("+1234567890")
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(Set.of(customerRole))
                .build();

        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getCreatedAt()).isNotNull();

        Optional<User> found = userRepository.findByEmailWithRoles("test.customer@shopsphere.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
        assertThat(found.get().getRoles()).hasSize(1);
        assertThat(found.get().getRoles().iterator().next().getName()).isEqualTo(ERole.ROLE_CUSTOMER);
    }

    @Test
    @DisplayName("Should correctly return existsByEmail boolean")
    void testExistsByEmail() {
        User user = User.builder()
                .email("unique@shopsphere.com")
                .password("hash123")
                .firstName("Jane")
                .lastName("Smith")
                .status(UserStatus.ACTIVE)
                .build();
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("unique@shopsphere.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@shopsphere.com")).isFalse();
    }
}
