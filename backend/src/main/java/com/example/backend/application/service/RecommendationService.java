package com.example.backend.application.service;

import com.example.backend.application.domain.Application;
import com.example.backend.application.domain.BatchType;
import com.example.backend.application.dto.StudentRecommendationView;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.ApplicationMapper;
import com.example.backend.application.mapper.StudentRecommendationMapper;
import com.example.backend.application.port.RecommendationGenerationService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService implements RecommendationGenerationService {
    private final ApplicationMapper applications;
    private final StudentRecommendationMapper recommendations;

    public RecommendationService(ApplicationMapper applications, StudentRecommendationMapper recommendations) {
        this.applications = applications;
        this.recommendations = recommendations;
    }

    @Override
    @Transactional
    public void generateForCompletedApplication(Long applicationId) {
        Application application = applications.findRequired(applicationId);
        if (application == null) throw new ApplicationException("APPLICATION_NOT_FOUND", HttpStatus.NOT_FOUND, "申请不存在");
        if (application.getBatchType() != BatchType.GREEN_CHANNEL) return;
        Long batchId = application.getGreenChannelBatchId();
        recommendations.upsert(application.getStudentId(), batchId, "LIVING", "已完成绿色通道申请，可继续申请生活补助");
        recommendations.upsert(application.getStudentId(), batchId, "TRAVEL", "已完成绿色通道申请，可继续申请路费补助");
    }

    public List<StudentRecommendationView> findMine(Long studentId) {
        return recommendations.findMine(studentId).stream()
                .map(item -> new StudentRecommendationView(item.id(), item.batchId(), item.recommendationType(), item.content(), item.read(), targetPath(item.recommendationType()), item.createTime()))
                .toList();
    }

    @Transactional
    public void markRead(Long id, Long studentId) {
        if (recommendations.markRead(id, studentId) != 1) throw new ApplicationException("RECOMMENDATION_NOT_FOUND", HttpStatus.NOT_FOUND, "推荐不存在");
    }

    private String targetPath(String type) { return "LIVING".equals(type) ? "/student/subsidy?type=LIVING" : "/student/subsidy?type=TRAVEL"; }
}
