package com.dmt.toeicapp.topic.service.impl;

import com.dmt.toeicapp.common.exception.AppException;
import com.dmt.toeicapp.common.security.SecurityUtils;
import com.dmt.toeicapp.topic.dto.TopicRequest;
import com.dmt.toeicapp.topic.dto.TopicResponse;
import com.dmt.toeicapp.topic.entity.Topic;
import com.dmt.toeicapp.topic.mapper.TopicMapper;
import com.dmt.toeicapp.topic.repository.TopicRepository;
import com.dmt.toeicapp.topic.service.TopicService;
import com.dmt.toeicapp.user.entity.User;
import com.dmt.toeicapp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {

    private final TopicRepository topicRepository;
    private final UserRepository  userRepository;
    private final TopicMapper     topicMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TopicResponse> getAccessible() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return topicRepository.findAccessibleByUser(currentUserId)
                .stream()
                .map(topicMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TopicResponse getById(Long id) {
        Topic topic = findAndCheckAccess(id);
        return topicMapper.toResponse(topic);
    }

    @Override
    @Transactional
    public TopicResponse create(TopicRequest request) {
        Long    currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin       = SecurityUtils.isAdmin();

        // Kiểm tra tên trùng trong personal topics
        if (!isAdmin && topicRepository.existsByNameAndCreatedById(request.name(), currentUserId)) {
            throw AppException.conflict(
                    "Bạn đã có topic tên '" + request.name() + "' rồi",
                    "TOPIC_NAME_DUPLICATE");
        }

        User owner = userRepository.getReferenceById(currentUserId);

        Topic topic = Topic.builder()
                .name(request.name())
                .description(request.description())
                .iconUrl(request.iconUrl())
                .system(isAdmin)   // ADMIN tạo → system topic, USER tạo → personal
                .createdBy(owner)
                .build();

        return topicMapper.toResponse(topicRepository.save(topic));
    }

    @Override
    @Transactional
    public TopicResponse update(Long id, TopicRequest request) {
        Topic topic = findAndCheckOwnership(id);

        topic.setName(request.name());
        topic.setDescription(request.description());
        topic.setIconUrl(request.iconUrl());

        return topicMapper.toResponse(topicRepository.save(topic));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Topic topic = findAndCheckOwnership(id);
        topicRepository.delete(topic);
    }

    // ── Private helpers ───────────────────────────────────────

    // Tìm topic + kiểm tra quyền xem (system hoặc là của mình)
    private Topic findAndCheckAccess(Long id) {
        Long  currentUserId = SecurityUtils.getCurrentUserId();
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy topic với id = " + id));

        if (!topic.isSystem() && !topic.getCreatedBy().getId().equals(currentUserId)) {
            throw AppException.forbidden("Bạn không có quyền truy cập topic này");
        }
        return topic;
    }

    // Tìm topic + kiểm tra quyền sửa/xóa (chỉ owner hoặc ADMIN)
    private Topic findAndCheckOwnership(Long id) {
        Long    currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin       = SecurityUtils.isAdmin();

        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy topic với id = " + id));

        if (!isAdmin && !topic.getCreatedBy().getId().equals(currentUserId)) {
            throw AppException.forbidden("Bạn không có quyền chỉnh sửa topic này");
        }
        return topic;
    }
}