package com.shopsphere.service;

import com.shopsphere.entity.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyService {

    Optional<IdempotencyRecord> getRecord(String key, Long userId);

    IdempotencyRecord recordPending(String key, Long userId, String requestPath, String requestHash, int ttlMinutes);

    void completeRecord(String key, int statusCode, String responseJson);

    void deleteRecord(String key);
}
