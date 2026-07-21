package com.example.backend.application.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.approval.api.ApprovalTransitionService;
import com.example.backend.application.domain.*;
import com.example.backend.application.dto.*;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.*;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.schoolproxy.*;
import com.example.backend.model.dto.supplement.*;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.model.vo.supplement.SupplementApplicationVO;
import com.example.backend.service.port.SupplementApplicationPort;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 成员二补录写入与自动审核桥接；学生快照读取仍由成员一 Port 提供。 */
@Service
public class SupplementApplicationService implements SupplementApplicationPort {
    private final ApplicationService applications; private final ApplicationMapper mapper; private final ApplicationOperationMapper operations;
    private final ApplicationResourceMapper resources; private final ObjectProvider<ApprovalTransitionService> transitions;
    public SupplementApplicationService(ApplicationService applications, ApplicationMapper mapper, ApplicationOperationMapper operations,
                                        ApplicationResourceMapper resources, ObjectProvider<ApprovalTransitionService> transitions) { this.applications=applications;this.mapper=mapper;this.operations=operations;this.resources=resources;this.transitions=transitions; }
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
        state=applications.getRequiredState(state.applicationId()); ApprovalTransitionService transition=transitions.getIfAvailable();
        if(transition==null) throw unavailable("缺少成员三 ApprovalTransitionService，不能生成补录自动审核记录");
        transition.completeSupplementReview(state.applicationId(), !command.getArrearsItems().isEmpty(), state.version(), "SUPPLEMENT_COMPLETE_"+state.applicationId(),operatorUserId);
        return toView(requireSupplement(state.applicationId()),student);
    }
    @Override public Page<SupplementApplicationVO> findSupplementPage(SupplementQueryDTO query, PageDTO page, Long operatorUserId) { throw unavailable("补录历史需要成员一按 studentId 批量返回学生快照；当前 Port 仅支持按学号单查"); }
    @Override public SupplementApplicationVO findSupplementById(Long applicationId, Long operatorUserId) { throw unavailable("补录详情需要成员一按 studentId 返回学生快照；当前 Port 未提供该查询"); }
    private void validateDetails(SupplementCreateDTO c,ApplicationType type){ if(type==ApplicationType.GREEN_CHANNEL){if(c.getArrearsItems().isEmpty()&&c.getGiftItems().isEmpty())throw bad("SUPPLEMENT_DETAIL_REQUIRED","绿色通道补录至少填写欠费或礼包");if(c.getSubsidyAmount()!=null)throw bad("SUPPLEMENT_SUBSIDY_INVALID","绿色通道补录不能填写补助金额");}else if(!c.getArrearsItems().isEmpty()||!c.getGiftItems().isEmpty()||c.getSubsidyAmount()==null||c.getSubsidyAmount().compareTo(BigDecimal.ZERO)<=0)throw bad("SUPPLEMENT_DETAIL_INVALID","补助补录只能填写大于零的补助金额");}
    private List<GiftApplicationItemCommand> giftItems(Long batchId,List<SchoolProxyGiftItemDTO> rows){return rows.stream().map(x->{Long id=resources.findBatchGiftItemId(batchId,x.getGiftItemId());if(id==null)throw bad("SUPPLEMENT_GIFT_ITEM_INVALID","礼包物品不属于当前批次");return new GiftApplicationItemCommand(id,x.getQuantity());}).toList();}
    private ApplicationType parseType(String value){try{return ApplicationType.valueOf(value);}catch(Exception e){throw bad("SUPPLEMENT_TYPE_INVALID","申请类型无效");}}
    private Application requireSupplement(Long id){Application a=mapper.findBySource(id,ApplicationSource.SUPPLEMENT);if(a==null)throw new ApplicationException("SUPPLEMENT_NOT_FOUND",HttpStatus.NOT_FOUND,"补录申请不存在");return a;}
    private SupplementApplicationVO toView(Application a,SchoolProxyStudentVO s){SupplementApplicationVO v=new SupplementApplicationVO();v.setApplicationId(a.getId());v.setApplicationNo(a.getApplicationNo());v.setStudentId(a.getStudentId());v.setStudentNo(s.getStudentNo());v.setStudentName(s.getStudentName());v.setApplicationType(a.getApplicationType().name());v.setBatchType(a.getBatchType().name());v.setBatchId(a.getBatchType()==BatchType.GREEN_CHANNEL?a.getGreenChannelBatchId():a.getSubsidyBatchId());v.setSource(a.getSource().name());v.setStatus(a.getStatus().name());v.setCurrentLevel(a.getCurrentLevel().name());v.setVersion(a.getVersion());v.setContainsArrears(applications.containsArrears(a.getId()));v.setSupplementUserId(a.getCreateBy());v.setSupplementedAt(a.getSupplementedAt());v.setSupplementReason(a.getSupplementReason());return v;}
    private ApplicationException bad(String code,String message){return new ApplicationException(code,HttpStatus.BAD_REQUEST,message);}private ApplicationException unavailable(String message){return new ApplicationException("DEPENDENCY_UNAVAILABLE",HttpStatus.SERVICE_UNAVAILABLE,message);}
}
