package com.shopsphere.repository;

import com.shopsphere.entity.*;
import com.shopsphere.entity.enums.AddressType;
import com.shopsphere.entity.enums.OrderStatus;
import com.shopsphere.entity.enums.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderAddressSnapshotTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Should verify that Order retains immutable address snapshots even if User updates address book")
    void testImmutableAddressSnapshotInOrder() {
        // 1. Create customer
        User user = userRepository.save(User.builder()
                .email("buyer@shopsphere.com")
                .password("securehash")
                .firstName("Michael")
                .lastName("Scott")
                .status(UserStatus.ACTIVE)
                .build());

        // 2. Customer adds address to address book
        Address address = addressRepository.save(Address.builder()
                .user(user)
                .fullName("Michael Scott")
                .phoneNumber("570-555-0123")
                .addressLine1("1725 Slough Avenue")
                .city("Scranton")
                .state("PA")
                .postalCode("18503")
                .country("USA")
                .addressType(AddressType.HOME)
                .isDefault(true)
                .build());

        // 3. Customer places order capturing address snapshot
        AddressSnapshot snapshot = AddressSnapshot.fromAddress(address);

        Order order = Order.builder()
                .orderNumber("ORD-20261031-TEST01")
                .user(user)
                .shippingAddress(snapshot)
                .orderStatus(OrderStatus.CONFIRMED)
                .subtotal(new BigDecimal("150.00"))
                .totalAmount(new BigDecimal("150.00"))
                .build();

        Order savedOrder = orderRepository.save(order);
        assertThat(savedOrder.getId()).isNotNull();

        // 4. Customer moves and updates their address in address book
        address.setAddressLine1("42 Wallaby Way");
        address.setCity("Sydney");
        address.setCountry("Australia");
        addressRepository.saveAndFlush(address);

        // 5. Verify the historical order remains untouched with the original snapshot
        Optional<Order> fetchedOrder = orderRepository.findByOrderNumber("ORD-20261031-TEST01");
        assertThat(fetchedOrder).isPresent();
        AddressSnapshot historicalShipping = fetchedOrder.get().getShippingAddress();

        assertThat(historicalShipping.getAddressLine1()).isEqualTo("1725 Slough Avenue");
        assertThat(historicalShipping.getCity()).isEqualTo("Scranton");
        assertThat(historicalShipping.getCountry()).isEqualTo("USA");
    }
}
