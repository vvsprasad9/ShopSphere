package com.shopsphere.service.impl;

import com.shopsphere.entity.IdempotencyRecord;
import com.shopsphere.exception.BadRequestException;
import com.shopsphere.repository.IdempotencyRecordRepository;
import com.shopsphere.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> getRecord(String key, Long userId) {
        if (key == null || key.trim().isEmpty()) {
            return Optional.empty();
        }
        Optional<IdempotencyRecord> recordOpt = idempotencyRecordRepository.findByIdempotencyKey(key.trim());
        if (recordOpt.isPresent()) {
            IdempotencyRecord record = recordOpt.get();
            if (record.isExpired()) {
                log.info("Idempotency record for key {} has expired", key);
                return Optional.empty();
            }
            if (userId != null && record.getUserId() != null && !userId.equals(record.getUserId())) {
                log.warn("Idempotency key {} was created by user {} but accessed by user {}",
                        key, record.getUserId(), userId);
                throw new BadRequestException("Idempotency key conflict across users");
            }
            return Optional.of(record);
        }
        return Optional.empty();
    }

    @Override
    @Transactional
    public IdempotencyRecord recordPending(String key, Long userId, String requestPath, String requestHash, int ttlMinutes) {
        String cleanKey = key.trim();
        Optional<IdempotencyRecord> existing = idempotencyRecordRepository.findByIdempotencyKey(cleanKey);
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.isExpired()) {
                return record;
            }
            idempotencyRecordRepository.delete(record);
        }

        IdempotencyRecord newRecord = IdempotencyRecord.builder()
                .idempotencyKey(cleanKey)
                .userId(userId)
                .requestPath(requestPath)
                .requestHash(requestHash)
                .expiresAt(LocalDateTime.now().plusMinutes(ttlMinutes > 0 ? ttlMinutes : 60))
                .build();

        return idempotencyRecordRepository.save(newRecord);
    }

    @Override
    @Transactional
    public void completeRecord(String key, int statusCode, String responseJson) {
        if (key == null || key.trim().isEmpty()) {
            return;
        }
        idempotencyRecordRepository.findByIdempotencyKey(key.trim()).ifPresent(record -> {
            record.setStatusCode(statusCode);
            record.setResponseBody(responseJson);
            idempotencyRecordRepository.save(record);
            log.info("Completed idempotency record for key {}", key);
        });
    }

    @Override
    @Transactional
    public void deleteRecord(String key) {
        if (key != null && !key.trim().isEmpty()) {
            idempotencyRecordRepository.findByIdempotencyKey(key.trim())
                    .ifPresent(idempotencyRecordRepository::delete);
        }
    }
}
