package ru.netology.filestorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.netology.filestorage.entity.Token;

import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByTokenAndActiveTrue(String token);

    @Modifying
    @Query("UPDATE Token t SET t.active = false WHERE t.token = :token")
    void deactivateByToken(@Param("token") String token);
}