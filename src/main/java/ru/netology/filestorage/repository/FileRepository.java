package ru.netology.filestorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.netology.filestorage.entity.File;
import ru.netology.filestorage.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByUserOrderByUploadedAtDesc(User user);
    Optional<File> findByUserAndFilename(User user, String filename);
    boolean existsByUserAndFilename(User user, String filename);

    @Modifying
    @Query("DELETE FROM File f WHERE f.user = :user AND f.filename = :filename")
    void deleteByUserAndFilename(@Param("user") User user, @Param("filename") String filename);


    @Query("SELECT f FROM File f WHERE f.user = :user ORDER BY f.uploadedAt DESC")
    List<File> findUserFilesWithPagination(@Param("user") User user, org.springframework.data.domain.Pageable pageable);
}