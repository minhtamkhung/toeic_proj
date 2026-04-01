import api from './axiosInstance'

const progressApi = {
    getMyProgress: ()           => api.get('/progress/me'),
    getDueCards:   ()           => api.get('/progress/due'),
    review:        (data)       => api.post('/progress/review', data),
    // data = { flashcardId, quality (0-5) }
}

export default progressApi