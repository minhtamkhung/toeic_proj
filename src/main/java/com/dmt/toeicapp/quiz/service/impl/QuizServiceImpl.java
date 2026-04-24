package com.dmt.toeicapp.quiz.service.impl;

import com.dmt.toeicapp.common.exception.AppException;
import com.dmt.toeicapp.common.security.SecurityUtils;
import com.dmt.toeicapp.flashcard.entity.Flashcard;
import com.dmt.toeicapp.flashcard.repository.FlashcardRepository;
import com.dmt.toeicapp.i18n.entity.FlashcardTranslation;
import com.dmt.toeicapp.i18n.repository.FlashcardTranslationRepository;
import com.dmt.toeicapp.quiz.dto.*;
import com.dmt.toeicapp.quiz.entity.QuizAnswer;
import com.dmt.toeicapp.quiz.entity.QuizAttempt;
import com.dmt.toeicapp.quiz.repository.QuizAnswerRepository;
import com.dmt.toeicapp.quiz.repository.QuizAttemptRepository;
import com.dmt.toeicapp.quiz.service.QuizService;
import com.dmt.toeicapp.topic.entity.Topic;
import com.dmt.toeicapp.topic.repository.TopicRepository;
import com.dmt.toeicapp.user.entity.User;
import com.dmt.toeicapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository  quizAnswerRepository;
    private final FlashcardRepository   flashcardRepository;
    private final TopicRepository       topicRepository;
    private final FlashcardTranslationRepository translationRepository;
    private final UserRepository        userRepository;

    private static final int OPTIONS_COUNT = 4; // số đáp án mỗi câu

    @Override
    @Transactional
    public QuizAttemptSummary start(QuizStartRequest request, String locale) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy topic"));

        List<Flashcard> allCards = flashcardRepository
                .findByTopicId(topic.getId(), PageRequest.of(0, 200)).getContent();

        if (allCards.isEmpty()) throw AppException.badRequest("Topic không có flashcard", "TOPIC_EMPTY");

        // 1. Lấy toàn bộ bản dịch của card trong topic cho locale hiện tại
        Map<Long, String> localizedDefs = translationRepository
                .findByFlashcardIdsAndLocale(allCards.stream().map(Flashcard::getId).toList(), locale)
                .stream()
                .collect(Collectors.toMap(t -> t.getFlashcard().getId(), FlashcardTranslation::getDefinition));

        List<Flashcard> mutableCards = new ArrayList<>(allCards);
        Collections.shuffle(mutableCards);
        List<Flashcard> selectedCards = mutableCards.stream().limit(request.questionCount()).toList();

        QuizAttempt attempt = quizAttemptRepository.save(QuizAttempt.builder()
                .user(userRepository.getReferenceById(currentUserId))
                .topic(topic).totalQuestions(selectedCards.size()).build());

        // 2. Tạo câu hỏi với đáp án nhiễu theo đúng locale
        List<QuizQuestionResponse> questions = selectedCards.stream().map(card -> {
            // Ưu tiên bản dịch, nếu không có thì dùng định nghĩa gốc
            String correctDef = localizedDefs.getOrDefault(card.getId(), card.getDefinition());

            List<String> options = new ArrayList<>();
            options.add(correctDef);

            List<String> distractors = allCards.stream()
                    .filter(c -> !c.getId().equals(card.getId()))
                    .map(c -> localizedDefs.getOrDefault(c.getId(), c.getDefinition()))
                    .distinct().collect(Collectors.toList());
            Collections.shuffle(distractors);
            distractors.stream().limit(3).forEach(options::add);

            while (options.size() < 4) options.add("None of the above");
            Collections.shuffle(options);

            return new QuizQuestionResponse(card.getId(), card.getWord(), card.getPronunciation(), options);
        }).toList();

        return toSummary(attempt, topic, questions);
    }

    @Override
    @Transactional
    public QuizAnswerResponse answer(Long attemptId, QuizAnswerRequest request, String locale) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        QuizAttempt attempt = quizAttemptRepository.findByIdAndUserId(attemptId, currentUserId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy lượt làm bài"));

        Flashcard flashcard = flashcardRepository.findByIdAndActiveTrue(request.flashcardId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy thẻ từ"));

        // FIX: Lấy định nghĩa đúng theo locale để đối soát
        String correctAnswer = translationRepository.findByFlashcardIdAndLocale(flashcard.getId(), locale)
                .map(FlashcardTranslation::getDefinition)
                .orElse(flashcard.getDefinition());

        // FIX: Trim cả 2 vế để tránh lỗi khoảng trắng thừa
        boolean isCorrect = correctAnswer.trim().equalsIgnoreCase(request.selectedAnswer().trim());

        quizAnswerRepository.save(QuizAnswer.builder()
                .attempt(attempt).flashcard(flashcard)
                .selectedAnswer(request.selectedAnswer()).isCorrect(isCorrect)
                .timeSpentSeconds(request.timeSpentSeconds()).build());

        if (isCorrect) {
            attempt.setCorrectAnswers(attempt.getCorrectAnswers() + 1);
            quizAttemptRepository.save(attempt);
        }

        return new QuizAnswerResponse(flashcard.getId(), request.selectedAnswer(), correctAnswer, isCorrect);
    }

    @Override
    @Transactional
    public QuizAttemptSummary finish(Long attemptId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        QuizAttempt attempt = quizAttemptRepository
                .findByIdAndUserId(attemptId, currentUserId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy quiz attempt"));

        if (attempt.getFinishedAt() != null) {
            throw AppException.badRequest("Quiz này đã kết thúc", "QUIZ_ALREADY_FINISHED");
        }

        // Tính điểm và thời gian
        OffsetDateTime now = OffsetDateTime.now();
        int score = attempt.getTotalQuestions() > 0
                ? (int) Math.round((double) attempt.getCorrectAnswers()
                / attempt.getTotalQuestions() * 100)
                : 0;
        int duration = (int) ChronoUnit.SECONDS.between(attempt.getStartedAt(), now);

        attempt.setFinishedAt(now);
        attempt.setScore(score);
        attempt.setDurationSeconds(duration);
        quizAttemptRepository.save(attempt);

        log.info("Quiz finished: attemptId={}, score={}, duration={}s",
                attemptId, score, duration);

        return toSummary(attempt, attempt.getTopic(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizAttemptSummary> getHistory() {
        Long userId = SecurityUtils.getCurrentUserId();
        return quizAttemptRepository
                .findByUserIdOrderByStartedAtDesc(userId)
                .stream()
                .map(a -> toSummary(a, a.getTopic(), null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuizReviewResponse review(Long attemptId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        QuizAttempt attempt = quizAttemptRepository
                .findByIdAndUserId(attemptId, currentUserId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy quiz attempt"));

        // Lấy chỉ câu sai
        List<QuizReviewResponse.WrongAnswerDetail> wrongAnswers =
                quizAnswerRepository.findByAttemptIdAndIsCorrectFalse(attemptId)
                        .stream()
                        .map(a -> new QuizReviewResponse.WrongAnswerDetail(
                                a.getFlashcard().getId(),
                                a.getFlashcard().getWord(),
                                a.getFlashcard().getDefinition(),
                                a.getSelectedAnswer(),
                                a.getFlashcard().getDefinition()
                        ))
                        .toList();

        return new QuizReviewResponse(
                attemptId,
                attempt.getTotalQuestions(),
                attempt.getCorrectAnswers(),
                attempt.getScore(),
                wrongAnswers
        );
    }

    // ── Private helpers ───────────────────────────────────────

    private QuizAttemptSummary toSummary(QuizAttempt a, Topic t, List<QuizQuestionResponse> q) {
        return new QuizAttemptSummary(a.getId(), t.getId(), t.getName(), a.getQuizType().name(),
                a.getTotalQuestions(), a.getCorrectAnswers(), a.getScore(), a.getDurationSeconds(),
                a.getStartedAt(), a.getFinishedAt(), q);
    }
}