package com.dmt.toeicapp.quiz.service.impl;

import com.dmt.toeicapp.common.exception.AppException;
import com.dmt.toeicapp.common.security.SecurityUtils;
import com.dmt.toeicapp.flashcard.entity.Flashcard;
import com.dmt.toeicapp.flashcard.repository.FlashcardRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuizAnswerRepository  quizAnswerRepository;
    private final FlashcardRepository   flashcardRepository;
    private final TopicRepository       topicRepository;
    private final UserRepository        userRepository;

    private static final int OPTIONS_COUNT = 4; // số đáp án mỗi câu

    @Override
    @Transactional
    public QuizAttemptSummary start(QuizStartRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // Kiểm tra topic tồn tại và user có quyền truy cập
        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> AppException.notFound(
                        "Không tìm thấy topic với id = " + request.topicId()));

        if (!topic.isSystem() && !topic.getCreatedBy().getId().equals(currentUserId)) {
            throw AppException.forbidden("Bạn không có quyền truy cập topic này");
        }

        // Lấy flashcard từ topic — random shuffle, lấy đủ số câu
        List<Flashcard> allCards = flashcardRepository
                .findByTopicId(topic.getId(), PageRequest.of(0, 200))
                .getContent();

        if (allCards.isEmpty()) {
            throw AppException.badRequest(
                    "Topic này chưa có flashcard nào", "TOPIC_NO_FLASHCARDS");
        }

        // Shuffle và lấy đúng số câu yêu cầu
        List<Flashcard> mutableCards = new ArrayList<>(allCards);
        Collections.shuffle(mutableCards);

        List<Flashcard> selectedCards = mutableCards.stream()
                .limit(request.questionCount())
                .toList();

        // Tạo QuizAttempt
        User user = userRepository.getReferenceById(currentUserId);
        QuizAttempt attempt = QuizAttempt.builder()
                .user(user)
                .topic(topic)
                .quizType(QuizAttempt.QuizType.MULTIPLE_CHOICE)
                .totalQuestions(selectedCards.size())
                .build();
        attempt = quizAttemptRepository.save(attempt);

        // Generate câu hỏi với đáp án nhiễu
        List<QuizQuestionResponse> questions = generateQuestions(selectedCards, mutableCards);

        log.info("Quiz started: attemptId={}, userId={}, topicId={}, questions={}",
                attempt.getId(), currentUserId, topic.getId(), selectedCards.size());

        return toSummary(attempt, topic, questions);
    }

    @Override
    @Transactional
    public QuizAnswerResponse answer(Long attemptId, QuizAnswerRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        QuizAttempt attempt = quizAttemptRepository
                .findByIdAndUserId(attemptId, currentUserId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy quiz attempt"));

        if (attempt.getFinishedAt() != null) {
            throw AppException.badRequest("Quiz này đã kết thúc", "QUIZ_ALREADY_FINISHED");
        }

        Flashcard flashcard = flashcardRepository
                .findByIdAndActiveTrue(request.flashcardId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy flashcard"));

        // Đáp án đúng là definition của flashcard
        String correctAnswer = flashcard.getDefinition();
        boolean isCorrect    = correctAnswer.equalsIgnoreCase(request.selectedAnswer().trim());

        // Lưu câu trả lời
        QuizAnswer quizAnswer = QuizAnswer.builder()
                .attempt(attempt)
                .flashcard(flashcard)
                .selectedAnswer(request.selectedAnswer())
                .isCorrect(isCorrect)
                .timeSpentSeconds(request.timeSpentSeconds())
                .build();
        quizAnswerRepository.save(quizAnswer);

        // Cập nhật số câu đúng realtime
        if (isCorrect) {
            attempt.setCorrectAnswers(attempt.getCorrectAnswers() + 1);
            quizAttemptRepository.save(attempt);
        }

        return new QuizAnswerResponse(
                flashcard.getId(),
                request.selectedAnswer(),
                correctAnswer,
                isCorrect
        );
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

    // Generate 4 đáp án cho mỗi câu: 1 đúng + 3 nhiễu từ các card khác
    private List<QuizQuestionResponse> generateQuestions(
            List<Flashcard> selectedCards,
            List<Flashcard> allCards) {

        return selectedCards.stream().map(card -> {
            List<String> options = new ArrayList<>();
            options.add(card.getDefinition()); // đáp án đúng

            // Lấy 3 đáp án nhiễu từ các card khác
            List<String> wrongOptions = new ArrayList<>(
                    allCards.stream()
                            .filter(c -> !c.getId().equals(card.getId()))
                            .map(Flashcard::getDefinition)
                            .distinct()
                            .toList()
            );

            Collections.shuffle(wrongOptions);

            wrongOptions.stream()
                    .limit(OPTIONS_COUNT - 1)
                    .forEach(options::add);

            // Nếu không đủ 4 đáp án thì bổ sung placeholder
            while (options.size() < OPTIONS_COUNT) {
                options.add("None of the above");
            }

            Collections.shuffle(options); // shuffle để đáp án đúng không luôn ở vị trí đầu

            return new QuizQuestionResponse(
                    card.getId(),
                    card.getWord(),
                    card.getPronunciation(),
                    options
            );
        }).toList();
    }

    private QuizAttemptSummary toSummary(QuizAttempt attempt, Topic topic,
                                         List<QuizQuestionResponse> questions) {
        return new QuizAttemptSummary(
                attempt.getId(),
                topic != null ? topic.getId() : null,
                topic != null ? topic.getName() : null,
                attempt.getQuizType().name(),
                attempt.getTotalQuestions(),
                attempt.getCorrectAnswers(),
                attempt.getScore(),
                attempt.getDurationSeconds(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                questions
        );
    }
}