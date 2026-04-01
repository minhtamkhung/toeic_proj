import api from './axiosInstance'

const quizApi = {
    start:    (data)              => api.post('/quiz/start', data),
    // data = { topicId, questionCount }

    answer:   (attemptId, data)  => api.post(`/quiz/${attemptId}/answer`, data),
    // data = { flashcardId, selectedAnswer, timeSpentSeconds }

    finish:   (attemptId)        => api.post(`/quiz/${attemptId}/finish`),
    history:  ()                 => api.get('/quiz/history'),
    review:   (attemptId)        => api.get(`/quiz/${attemptId}/review`),
}

export default quizApi