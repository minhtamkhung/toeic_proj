import api from './axiosInstance'

const topicApi = {
    getAll:    ()         => api.get('/topics'),
    getById:   (id)       => api.get(`/topics/${id}`),
    create:    (data)     => api.post('/topics', data),
    update:    (id, data) => api.put(`/topics/${id}`, data),
    delete:    (id)       => api.delete(`/topics/${id}`),
}

export default topicApi