package com.shopsphere.repository;

import com.shopsphere.entity.RefreshToken;
import com.shopsphere.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    java.util.List<RefreshToken> findByUser(User user);

    @Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user = :user")
    void revokeAllUserTokens(@org.springframework.data.repository.query.Param("user") User user);

    @Modifying
    int deleteByUser(User user);
}
