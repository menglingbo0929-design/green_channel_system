package com.example.backend.approval.service;

import com.example.backend.approval.api.ApprovalDashboard;
import com.example.backend.approval.api.ApprovalDecisionCount;
import com.example.backend.approval.api.ApprovalDetailView;
import com.example.backend.approval.api.ApprovalListItem;
import com.example.backend.approval.api.ApprovalListQuery;
import com.example.backend.approval.api.ApprovalPage;
import com.example.backend.approval.api.ApprovalRecordSnapshot;
import com.example.backend.approval.api.ApprovalWorkbenchQueryService;
import com.example.backend.approval.domain.ApplicationStatus;
import com.example.backend.approval.domain.ApplicationType;
import com.example.backend.approval.domain.ApprovalAction;
import com.example.backend.approval.domain.ApprovalErrorCode;
import com.example.backend.approval.domain.ApprovalException;
import com.example.backend.approval.persistence.entity.ApprovalRecordEntity;
import com.example.backend.approval.persistence.mapper.ApprovalRecordMapper;
import com.example.backend.approval.persistence.type.ApprovalRecordLevel;
import com.example.backend.approval.port.ApprovalApplicationDetail;
import com.example.backend.approval.port.ApprovalApplicationQueryPort;
import com.example.backend.approval.port.ApprovalApplicationSnapshot;
import com.example.backend.approval.port.ApprovalDashboardData;
import com.example.backend.approval.port.ApprovalWorkScope;
import com.example.backend.approval.port.ApplicationStateQueryService;
import com.example.backend.approval.port.ApplicationStateSnapshot;
import com.example.backend.approval.port.LoginUser;
import com.example.backend.service.StudentScopeService;
import com.example.backend.approval.port.UserRole;
import java.util.List;
import java.util.Objects;

/** Coordinates member-three records with member-two application read models. */
public class DefaultApprovalWorkbenchQueryService implements ApprovalWorkbenchQueryService {

    private final ApplicationStateQueryService stateQueryService;
    private final StudentScopeService studentScopeService;
    private final ApprovalApplicationQueryPort applicationQueryPort;
    private final ApprovalRecordMapper approvalRecordMapper;

    public DefaultApprovalWorkbenchQueryService(
            ApplicationStateQueryService stateQueryService,
            StudentScopeService studentScopeService,
            ApprovalApplicationQueryPort applicationQueryPort,
            ApprovalRecordMapper approvalRecordMapper
    ) {
        this.stateQueryService = stateQueryService;
        this.studentScopeService = studentScopeService;
        this.applicationQueryPort = applicationQueryPort;
        this.approvalRecordMapper = approvalRecordMapper;
    }

    @Override
    public ApprovalPage<ApprovalListItem> pagePending(LoginUser user, ApprovalListQuery query) {
        ApprovalWorkScope scope = reviewScope(user, query);
        ApplicationStatus pendingStatus = pendingStatus(scope.reviewLevel());
        if (query.status() != null && query.status() != pendingStatus) {
            throw new IllegalArgumentException("pending 查询不允许跨审核层级筛选状态");
        }
        return mapPage(applicationQueryPort.pagePending(scope, query.withStatus(pendingStatus)), scope.reviewLevel());
    }

    @Override
    public ApprovalPage<ApprovalListItem> pageProcessed(LoginUser user, ApprovalListQuery query) {
        ApprovalWorkScope scope = reviewScope(user, query);
        List<Long> applicationIds = approvalRecordMapper.listProcessedApplicationIds(scope.reviewLevel(), scope.userId());
        if (applicationIds.isEmpty()) return ApprovalPage.empty(query.page(), query.size());
        return mapPage(applicationQueryPort.pageByApplicationIds(scope, query, applicationIds), scope.reviewLevel());
    }

    @Override
    public ApprovalDetailView getDetail(LoginUser user, Long applicationId) {
        requirePositive(applicationId, "applicationId");
        ApplicationStateSnapshot state = stateQueryService.getRequiredState(applicationId);
        assertCanRead(user, state);
        ApprovalApplicationDetail detail = applicationQueryPort.getRequiredApprovalDetail(applicationId);
        List<ApprovalRecordSnapshot> records = approvalRecordMapper.listByApplicationId(applicationId).stream()
                .map(this::toRecordSnapshot)
                .toList();
        return new ApprovalDetailView(
                detail.application(), detail.arrearsDetail(), detail.giftDetail(), detail.subsidyDetail(),
                detail.attachments(), records, editableFields(user, state), allowedActions(user, state),
                detail.version() == null ? state.version() : detail.version()
        );
    }

    @Override
    public ApprovalDashboard getDashboard(LoginUser user, ApprovalListQuery query) {
        ApprovalWorkScope scope = reviewScope(user, query);
        ApprovalDashboardData data = applicationQueryPort.getDashboard(scope, query);
        List<Long> scopedIds = applicationQueryPort.listScopedApplicationIds(scope, query);
        List<ApprovalDecisionCount> decisions = scopedIds.isEmpty()
                ? List.of()
                : approvalRecordMapper.countDecisions(scope.reviewLevel(), scope.userId(), scopedIds).stream()
                        .map(item -> new ApprovalDecisionCount(item.getAction(), item.getCount()))
                        .toList();
        return new ApprovalDashboard(data.pendingByLevel(), decisions, data.pendingByCollege(), data.flowFunnel());
    }

    private ApprovalPage<ApprovalListItem> mapPage(ApprovalPage<ApprovalApplicationSnapshot> source, ApprovalRecordLevel level) {
        List<ApprovalListItem> records = source.records().stream().map(item -> toListItem(item, level)).toList();
        return new ApprovalPage<>(records, source.total(), source.page(), source.size());
    }

    private ApprovalListItem toListItem(ApprovalApplicationSnapshot item, ApprovalRecordLevel level) {
        ApprovalAction latestDecision = approvalRecordMapper.findLatestDecision(
                item.applicationId(), item.reviewRound(), level
        ).map(ApprovalRecordEntity::getAction).orElse(null);
        return new ApprovalListItem(
                item.applicationId(), item.applicationNo(), item.applicationType(), typeName(item.applicationType()),
                item.batchType(), item.batchId(), item.studentId(), item.studentNo(), item.studentName(),
                item.collegeId(), item.collegeName(), item.gradeName(), item.declaredAmount(), item.status(), statusName(item.status()),
                item.currentLevel(), latestDecision, item.submitTime(), item.version()
        );
    }

    private ApprovalWorkScope reviewScope(LoginUser user, ApprovalListQuery query) {
        requireReviewer(user);
        if (query.collegeId() != null && user.role() != UserRole.SCHOOL) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "仅学校角色可按学院筛选");
        }
        return new ApprovalWorkScope(user.userId(), user.role(), user.collegeId(), ApprovalRecordLevel.valueOf(user.role().name()));
    }

    private void assertCanRead(LoginUser user, ApplicationStateSnapshot state) {
        if (user == null || user.userId() == null) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "未获取到当前登录用户");
        }
        if (user.role() == UserRole.SCHOOL) return;
        if (user.role() == UserRole.STUDENT) {
            if (Objects.equals(user.studentId(), state.studentId())) return;
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "只能查看本人申请");
        }
        boolean allowed = user.role() == UserRole.COUNSELOR
                ? studentScopeService.isCounselorResponsibleFor(user.userId(), state.studentId())
                : user.collegeId() != null && studentScopeService.isStudentInCollege(state.studentId(), user.collegeId());
        if (!allowed) throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前用户不在该申请的数据范围内");
    }

    private void requireReviewer(LoginUser user) {
        if (user == null || user.userId() == null || user.role() == UserRole.STUDENT) {
            throw new ApprovalException(ApprovalErrorCode.APPROVAL_FORBIDDEN_SCOPE, "当前角色不能访问审核工作台");
        }
    }

    private ApplicationStatus pendingStatus(ApprovalRecordLevel level) {
        return switch (level) {
            case COUNSELOR -> ApplicationStatus.COUNSELOR_PENDING;
            case COLLEGE -> ApplicationStatus.COLLEGE_PENDING;
            case SCHOOL -> ApplicationStatus.SCHOOL_PENDING;
            default -> throw new IllegalArgumentException("不支持的审核层级");
        };
    }

    private List<String> editableFields(LoginUser user, ApplicationStateSnapshot state) {
        return ApprovalEditableFieldPolicy.editableFields(user, state);
    }

    private List<ApprovalAction> allowedActions(LoginUser user, ApplicationStateSnapshot state) {
        return switch (user.role()) {
            case COUNSELOR -> state.status() == ApplicationStatus.COUNSELOR_PENDING ? reviewActions() : List.of();
            case COLLEGE -> state.status() == ApplicationStatus.COLLEGE_PENDING ? reviewActions() : List.of();
            case SCHOOL -> state.status() == ApplicationStatus.SCHOOL_PENDING ? reviewActions() : List.of();
            case STUDENT -> List.of();
        };
    }

    private List<ApprovalAction> reviewActions() {
        return List.of(ApprovalAction.APPROVE, ApprovalAction.RETURN, ApprovalAction.REJECT);
    }

    private ApprovalRecordSnapshot toRecordSnapshot(ApprovalRecordEntity entity) {
        return new ApprovalRecordSnapshot(
                entity.getId(), entity.getApplicationId(), entity.getReviewRound(), entity.getApprovalLevel(),
                entity.getApproverId(), entity.getApproverNameSnapshot(), entity.getAction(), entity.getComment(),
                entity.getOldStatus(), entity.getNewStatus(), entity.getModifiedFields(), entity.getRequestId(),
                entity.getCreateTime()
        );
    }

    private String typeName(ApplicationType type) {
        return switch (type) {
            case GREEN_CHANNEL -> "绿色通道";
            case LIVING_SUBSIDY -> "生活补助";
            case TRAVEL_SUBSIDY -> "交通补助";
        };
    }

    private String statusName(ApplicationStatus status) {
        return switch (status) {
            case DRAFT -> "草稿";
            case COUNSELOR_PENDING -> "待辅导员审核";
            case COUNSELOR_RETURNED -> "辅导员退回";
            case COLLEGE_PENDING -> "待学院审核";
            case COLLEGE_RETURNED -> "学院退回";
            case SCHOOL_PENDING -> "待学校审核";
            case SCHOOL_RETURNED -> "学校退回";
            case REJECTED -> "已驳回";
            case APPROVED -> "已通过";
            case CONFIRM_PENDING -> "待欠费确认";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
        };
    }

    private void requirePositive(Long value, String name) {
        if (value == null || value <= 0) throw new IllegalArgumentException(name + " must be positive");
    }
}
