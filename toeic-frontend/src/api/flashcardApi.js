import api from './axiosInstance'

const flashcardApi = {
    getAll:       (params)    => api.get('/flashcards', { params }),
    getByTopic:   (topicId, params) => api.get('/flashcards', { params: { topicId, ...params } }),
    getById:      (id)        => api.get(`/flashcards/${id}`),
    create:       (data)      => api.post('/flashcards', data),
    update:       (id, data)  => api.put(`/flashcards/${id}`, data),
    delete:       (id)        => api.delete(`/flashcards/${id}`),
    uploadImage:  (id, file)  => {
        const form = new FormData()
        form.append('file', file)
        return api.post(`/flashcards/${id}/image`, form, {
            headers: { 'Content-Type': 'multipart/form-data' }
        })
    },
    deleteImage:  (id)        => api.delete(`/flashcards/${id}/image`),
}

export default flashcardApi