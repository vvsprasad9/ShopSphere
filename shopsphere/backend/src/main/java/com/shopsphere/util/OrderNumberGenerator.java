package com.shopsphere.util;

import com.shopsphere.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNumberGenerator {

    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int RANDOM_PART_LENGTH = 8;
    private static final int MAX_COLLISION_RETRIES = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final SecureRandom secureRandom = new SecureRandom();

    private final OrderRepository orderRepository;

    public String generateUniqueOrderNumber() {
        String datePrefix = LocalDate.now().format(DATE_FORMAT);

        for (int attempt = 1; attempt <= MAX_COLLISION_RETRIES; attempt++) {
            StringBuilder sb = new StringBuilder("ORD-")
                    .append(datePrefix)
                    .append("-");

            for (int i = 0; i < RANDOM_PART_LENGTH; i++) {
                int index = secureRandom.nextInt(ALPHANUMERIC.length());
                sb.append(ALPHANUMERIC.charAt(index));
            }

            String candidate = sb.toString();
            if (orderRepository.findByOrderNumber(candidate).isEmpty()) {
                return candidate;
            }
            log.warn("Order number collision for: {} on attempt {}", candidate, attempt);
        }

        // Fallback using high-resolution timestamp
        return "ORD-" + datePrefix + "-" + System.nanoTime();
    }
}
