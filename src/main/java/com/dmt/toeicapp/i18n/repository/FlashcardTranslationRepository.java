package com.dmt.toeicapp.i18n.repository;

import com.dmt.toeicapp.i18n.entity.FlashcardTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FlashcardTranslationRepository extends JpaRepository<FlashcardTranslation, Long> {

    // Lấy bản dịch theo flashcard + locale — dùng khi hiển thị 1 card
    Optional<FlashcardTranslation> findByFlashcardIdAndLocale(Long flashcardId, String locale);

    // Lấy tất cả bản dịch của 1 flashcard — dùng khi admin quản lý
    List<FlashcardTranslation> findByFlashcardId(Long flashcardId);

    // Lấy tất cả bản dịch của nhiều flashcard cùng lúc — tránh N+1 query
    // Dùng khi load danh sách flashcard của 1 topic
    @Query("""
            SELECT t FROM FlashcardTranslation t
            WHERE t.flashcard.id IN :flashcardIds
              AND t.locale = :locale
            """)
    List<FlashcardTranslation> findByFlashcardIdsAndLocale(
            @Param("flashcardIds") List<Long> flashcardIds,
            @Param("locale") String locale
    );

    // Kiểm tra bản dịch đã tồn tại chưa
    boolean existsByFlashcardIdAndLocale(Long flashcardId, String locale);

    // Xóa tất cả bản dịch khi xóa flashcard (soft delete không cần cái này)
    void deleteByFlashcardId(Long flashcardId);
}