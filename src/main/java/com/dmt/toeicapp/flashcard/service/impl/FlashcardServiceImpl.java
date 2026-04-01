package com.dmt.toeicapp.flashcard.service.impl;

import com.dmt.toeicapp.common.aop.Auditable;
import com.dmt.toeicapp.common.exception.AppException;
import com.dmt.toeicapp.common.security.SecurityUtils;
import com.dmt.toeicapp.flashcard.dto.FlashcardRequest;
import com.dmt.toeicapp.flashcard.dto.FlashcardResponse;
import com.dmt.toeicapp.flashcard.entity.Flashcard;
import com.dmt.toeicapp.flashcard.mapper.FlashcardMapper;
import com.dmt.toeicapp.flashcard.repository.FlashcardRepository;
import com.dmt.toeicapp.flashcard.service.CloudinaryService;
import com.dmt.toeicapp.flashcard.service.FlashcardService;
import com.dmt.toeicapp.topic.entity.Topic;
import com.dmt.toeicapp.topic.repository.TopicRepository;
import com.dmt.toeicapp.user.entity.User;
import com.dmt.toeicapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FlashcardServiceImpl implements FlashcardService {

    private final FlashcardRepository flashcardRepository;
    private final TopicRepository     topicRepository;
    private final UserRepository      userRepository;
    private final FlashcardMapper     flashcardMapper;
    private final CloudinaryService cloudinaryService;

    @Override
    @Transactional(readOnly = true)
    public Page<FlashcardResponse> getAccessible(Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        return flashcardRepository
                .findAccessibleByUser(userId, pageable)
                .map(flashcardMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FlashcardResponse> getByTopic(Long topicId, Pageable pageable) {
        Topic topic = findTopicAndCheckAccess(topicId);
        return flashcardRepository
                .findByTopicId(topic.getId(), pageable)
                .map(flashcardMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FlashcardResponse getById(Long id) {
        Flashcard card = findCardAndCheckAccess(id);
        return flashcardMapper.toResponse(card);
    }

    @Override
    @Auditable(action = "CREATE", entity = "FLASHCARD")
    @Transactional
    public FlashcardResponse create(FlashcardRequest request) {
        Long  currentUserId = SecurityUtils.getCurrentUserId();
        Topic topic         = findTopicAndCheckAccess(request.topicId());

        if (flashcardRepository.existsByWordAndTopicIdAndIsActiveTrue(
                request.word(), topic.getId())) {
            throw AppException.conflict(
                    "Từ '" + request.word() + "' đã tồn tại trong topic này",
                    "FLASHCARD_WORD_DUPLICATE");
        }

        User owner = userRepository.getReferenceById(currentUserId);

        Flashcard card = Flashcard.builder()
                .topic(topic)
                .createdBy(owner)
                .word(request.word())
                .pronunciation(request.pronunciation())
                .definition(request.definition())
                .exampleSentence(request.exampleSentence())
                .difficulty(parseDifficulty(request.difficulty()))
                .build();

        return flashcardMapper.toResponse(flashcardRepository.save(card));
    }

    @Override
    @Auditable(action = "UPDATE", entity = "FLASHCARD")
    @Transactional
    public FlashcardResponse update(Long id, FlashcardRequest request) {
        Flashcard card  = findCardAndCheckOwnership(id);
        Topic     topic = findTopicAndCheckAccess(request.topicId());

        boolean wordChanged = !card.getWord().equalsIgnoreCase(request.word());
        if (wordChanged && flashcardRepository.existsByWordAndTopicIdAndIsActiveTrue(
                request.word(), topic.getId())) {
            throw AppException.conflict(
                    "Từ '" + request.word() + "' đã tồn tại trong topic này",
                    "FLASHCARD_WORD_DUPLICATE");
        }

        card.setTopic(topic);
        card.setWord(request.word());
        card.setPronunciation(request.pronunciation());
        card.setDefinition(request.definition());
        card.setExampleSentence(request.exampleSentence());
        card.setDifficulty(parseDifficulty(request.difficulty()));

        return flashcardMapper.toResponse(flashcardRepository.save(card));
    }

    @Override
    @Auditable(action = "DELETE", entity = "FLASHCARD")
    @Transactional
    public void delete(Long id) {
        Flashcard card = findCardAndCheckOwnership(id);
        card.setActive(false);
        flashcardRepository.save(card);
    }

    @Override
    @Transactional
    public FlashcardResponse uploadImage(Long id, MultipartFile file) {
        Flashcard card = findCardAndCheckOwnership(id);

        // Nếu đã có ảnh cũ thì xóa trên Cloudinary trước
        if (card.getImagePublicId() != null) {
            cloudinaryService.delete(card.getImagePublicId());
        }

        CloudinaryService.UploadResult result = cloudinaryService.upload(file);
        card.setImageUrl(result.secureUrl());
        card.setImagePublicId(result.publicId());

        return flashcardMapper.toResponse(flashcardRepository.save(card));
    }

    @Override
    @Transactional
    public FlashcardResponse deleteImage(Long id) {
        Flashcard card = findCardAndCheckOwnership(id);

        if (card.getImagePublicId() != null) {
            cloudinaryService.delete(card.getImagePublicId());
            card.setImageUrl(null);
            card.setImagePublicId(null);
            flashcardRepository.save(card);
        }

        return flashcardMapper.toResponse(card);
    }

    // ── Private helpers ───────────────────────────────────────

    private Flashcard findCardAndCheckAccess(Long id) {
        Long      currentUserId = SecurityUtils.getCurrentUserId();
        Flashcard card = flashcardRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> AppException.notFound(
                        "Không tìm thấy flashcard với id = " + id));

        // Card thuộc system topic → mọi người thấy được
        // Card thuộc personal topic → chỉ owner thấy
        boolean isSystemTopic = card.getTopic().isSystem();
        boolean isOwner       = card.getCreatedBy().getId().equals(currentUserId);

        if (!isSystemTopic && !isOwner) {
            throw AppException.forbidden("Bạn không có quyền truy cập flashcard này");
        }
        return card;
    }

    private Flashcard findCardAndCheckOwnership(Long id) {
        Long    currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin       = SecurityUtils.isAdmin();

        Flashcard card = flashcardRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> AppException.notFound(
                        "Không tìm thấy flashcard với id = " + id));

        if (!isAdmin && !card.getCreatedBy().getId().equals(currentUserId)) {
            throw AppException.forbidden("Bạn không có quyền chỉnh sửa flashcard này");
        }
        return card;
    }

    private Topic findTopicAndCheckAccess(Long topicId) {
        Long  currentUserId = SecurityUtils.getCurrentUserId();
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> AppException.notFound(
                        "Không tìm thấy topic với id = " + topicId));

        if (!topic.isSystem() && !topic.getCreatedBy().getId().equals(currentUserId)) {
            throw AppException.forbidden("Bạn không có quyền truy cập topic này");
        }
        return topic;
    }

    private Flashcard.Difficulty parseDifficulty(String difficulty) {
        if (difficulty == null) return Flashcard.Difficulty.MEDIUM;
        return Flashcard.Difficulty.valueOf(difficulty);
    }
}