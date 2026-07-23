package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.service.ApprovalTransitionService;
import com.example.backend.model.domain.*;
import com.example.backend.model.dto.*;
import com.example.backend.common.exception.ApplicationException;
import com.example.backend.mapper.*;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.schoolproxy.*;
import com.example.backend.model.dto.supplement.*;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.model.vo.supplement.SupplementApplicationVO;
import com.example.backend.service.port.SchoolProxyStudentQueryPort;
import com.example.backend.service.port.SupplementApplicationPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 成员二补录写入与自动审核桥接；学生快照读取仍由成员一 Port 提供。 */
@Service
public class SupplementApplicationService implements SupplementApplicationPort {
    private final ApplicationService applications; private final ApplicationMapper mapper; private final ApplicationOperationMapper operations;
    private final ApplicationResourceMapper resources; private final ApprovalTransitionService transitionService;
    private final SchoolProxyStudentQueryPort studentQueries;
    public SupplementApplicationService(ApplicationService applications, ApplicationMapper mapper, ApplicationOperationMapper operations,
                                         ApplicationResourceMapper resources, ApprovalTransitionService transitionService,
                                         SchoolProxyStudentQueryPort studentQueries) { this.applications=applications;this.mapper=mapper;this.operations=operations;this.resources=resources;this.transitionService=transitionService;this.studentQueries=studentQueries; }
    @Override @Transactional public SupplementApplicationVO createSupplementDraft(SupplementCreateDTO command, SchoolProxyStudentVO student, String ignoredBatchType, Long operatorUserId) {
        Long previous=operations.findApplicationIdByRequestId(command.getRequestId()); if(previous!=null) return toView(requireSupplement(previous),student);
        if(student==null || student.getStudentId()==null) throw bad("SUPPLEMENT_STUDENT_REQUIRED","必须由成员一学生查询提供有效学生快照");
        ApplicationType type=parseType(command.getApplicationType()); BatchType batchType=type==ApplicationType.GREEN_CHANNEL?BatchType.GREEN_CHANNEL:BatchType.SUBSIDY;
        validateDetails(command,type);
        ApplicationStateSnapshot state=applications.createSupplementApplication(student.getStudentId(),operatorUserId,new ApplicationDraftCommand(type,batchType,command.getBatchId(),command.getRequestId(),command.getApplicationReason()));
        mapper.updateSupplementMetadata(state.applicationId(),command.getSupplementReason(),command.getHandledAt());
        if(type==ApplicationType.GREEN_CHANNEL) {
            if(!command.getArrearsItems().isEmpty()) applications.replaceArrearsItems(state.applicationId(),state.version(),command.getArrearsItems().stream().map(x->new ArrearsItemCommand(x.getFeeItemId(),x.getDeclaredAmount(),x.getArrearsReasonCode())).toList(),operatorUserId);
            state=applications.getRequiredState(state.applicationId());
            if(!command.getGiftItems().isEmpty()) applications.replaceGiftItems(state.applicationId(),state.version(),giftItems(command.getBatchId(),command.getGiftItems()),operatorUserId);
        } else applications.replaceSubsidy(state.applicationId(),state.version(),command.getSubsidyAmount(),operatorUserId);
        state=applications.getRequiredState(state.applicationId());
        transitionService.completeSupplementReview(state.applicationId(), !command.getArrearsItems().isEmpty(), state.version(), "SUPPLEMENT_COMPLETE_"+state.applicationId(),operatorUserId);
        return toView(requireSupplement(state.applicationId()),student);
    }
    @Override public Page<SupplementApplicationVO> findSupplementPage(SupplementQueryDTO query, PageDTO page, Long operatorUserId) {
        SupplementQueryDTO actualQuery = query == null ? new SupplementQueryDTO() : query;
        PageDTO actualPage = page == null ? new PageDTO() : page;
        long pageNo = actualPage.getPageNo() == null ? 1 : Math.max(actualPage.getPageNo(), 1);
        long pageSize = actualPage.getPageSize() == null ? 10 : Math.min(Math.max(actualPage.getPageSize(), 1), 100);
        SchoolProxyStudentQueryPort students = requiredStudentQueries();
        Long studentId = resolveStudentId(actualQuery.getStudentNo(), students);
        Page<SupplementApplicationVO> result = new Page<>(pageNo, pageSize);
        if (actualQuery.getStudentNo() != null && !actualQuery.getStudentNo().isBlank() && studentId == null) {
            result.setTotal(0);
            result.setRecords(List.of());
            return result;
        }

        ApplicationType applicationType = parseApplicationType(actualQuery.getApplicationType());
        ApplicationStatus status = parseSupplementStatus(actualQuery.getStatus());
        Long batchId = positiveOrNull(actualQuery.getBatchId(), "batchId");
        long total = mapper.countSupplementPage(studentId, applicationType, batchId, status);
        List<Application> applications = total == 0
                ? List.of()
                : mapper.findSupplementPage(studentId, applicationType, batchId, status, pageSize, (pageNo - 1) * pageSize);
        Map<Long, SchoolProxyStudentVO> studentSnapshots = students.findEnabledStudentsByIds(
                        applications.stream().map(Application::getStudentId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(SchoolProxyStudentVO::getStudentId, Function.identity(), (left, right) -> left));
        result.setTotal(total);
        result.setRecords(applications.stream()
                .map(application -> toView(application, studentSnapshots.get(application.getStudentId())))
                .toList());
        return result;
    }

    @Override public SupplementApplicationVO findSupplementById(Long applicationId, Long operatorUserId) {
        if (applicationId == null || applicationId <= 0) throw bad("SUPPLEMENT_APPLICATION_ID_INVALID", "applicationId 必须为正整数");
        Application application = requireSupplement(applicationId);
        SchoolProxyStudentVO student = requiredStudentQueries().findEnabledStudentById(application.getStudentId());
        return toView(application, student);
    }
    private void validateDetails(SupplementCreateDTO c,ApplicationType type){ if(type==ApplicationType.GREEN_CHANNEL){if(c.getArrearsItems().isEmpty()&&c.getGiftItems().isEmpty())throw bad("SUPPLEMENT_DETAIL_REQUIRED","绿色通道补录至少填写欠费或礼包");if(c.getSubsidyAmount()!=null)throw bad("SUPPLEMENT_SUBSIDY_INVALID","绿色通道补录不能填写补助金额");}else if(!c.getArrearsItems().isEmpty()||!c.getGiftItems().isEmpty()||c.getSubsidyAmount()==null||c.getSubsidyAmount().compareTo(BigDecimal.ZERO)<=0)throw bad("SUPPLEMENT_DETAIL_INVALID","补助补录只能填写大于零的补助金额");}
    private List<GiftApplicationItemCommand> giftItems(Long batchId,List<SchoolProxyGiftItemDTO> rows){return rows.stream().map(x->{Long id=resources.findBatchGiftItemId(batchId,x.getGiftItemId());if(id==null)throw bad("SUPPLEMENT_GIFT_ITEM_INVALID","礼包物品不属于当前批次");return new GiftApplicationItemCommand(id,x.getQuantity());}).toList();}
    private ApplicationType parseType(String value){try{return ApplicationType.valueOf(value);}catch(Exception e){throw bad("SUPPLEMENT_TYPE_INVALID","申请类型无效");}}
    private Application requireSupplement(Long id){Application a=mapper.findBySource(id,ApplicationSource.SUPPLEMENT);if(a==null)throw new ApplicationException("SUPPLEMENT_NOT_FOUND",HttpStatus.NOT_FOUND,"补录申请不存在");return a;}
    private SchoolProxyStudentQueryPort requiredStudentQueries() { return studentQueries; }
    private Long resolveStudentId(String studentNo, SchoolProxyStudentQueryPort students) { if (studentNo == null || studentNo.isBlank()) return null; SchoolProxyStudentVO student = students.findEnabledStudentByStudentNo(studentNo); return student == null ? null : student.getStudentId(); }
    private ApplicationType parseApplicationType(String value) { if (value == null || value.isBlank()) return null; try { return ApplicationType.valueOf(value); } catch (IllegalArgumentException exception) { throw bad("SUPPLEMENT_TYPE_INVALID", "applicationType 无效"); } }
    private ApplicationStatus parseSupplementStatus(String value) { if (value == null || value.isBlank()) return null; try { ApplicationStatus status = ApplicationStatus.valueOf(value); if (status != ApplicationStatus.CONFIRM_PENDING && status != ApplicationStatus.COMPLETED) throw bad("SUPPLEMENT_STATUS_INVALID", "status 仅支持 CONFIRM_PENDING 或 COMPLETED"); return status; } catch (IllegalArgumentException exception) { throw bad("SUPPLEMENT_STATUS_INVALID", "status 无效"); } }
    private Long positiveOrNull(Long value, String name) { if (value == null) return null; if (value <= 0) throw bad("SUPPLEMENT_QUERY_INVALID", name + " 必须为正整数"); return value; }
    private SupplementApplicationVO toView(Application a,SchoolProxyStudentVO s){SupplementApplicationVO v=new SupplementApplicationVO();v.setApplicationId(a.getId());v.setApplicationNo(a.getApplicationNo());v.setStudentId(a.getStudentId());if(s!=null){v.setStudentNo(s.getStudentNo());v.setStudentName(s.getStudentName());}v.setApplicationType(a.getApplicationType().name());v.setBatchType(a.getBatchType().name());v.setBatchId(a.getBatchType()==BatchType.GREEN_CHANNEL?a.getGreenChannelBatchId():a.getSubsidyBatchId());v.setSource(a.getSource().name());v.setStatus(a.getStatus().name());v.setCurrentLevel(a.getCurrentLevel().name());v.setVersion(a.getVersion());v.setContainsArrears(applications.containsArrears(a.getId()));v.setSupplementUserId(a.getCreateBy());v.setSupplementedAt(a.getSupplementedAt());v.setSupplementReason(a.getSupplementReason());return v;}
    private ApplicationException bad(String code,String message){return new ApplicationException(code,HttpStatus.BAD_REQUEST,message);}
}
