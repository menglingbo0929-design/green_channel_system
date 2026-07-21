package com.example.backend.application.service;

import com.example.backend.application.domain.*;
import com.example.backend.application.dto.*;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.ApplicationMapper;
import com.example.backend.application.mapper.ApplicationOperationMapper;
import com.example.backend.application.mapper.ApplicationResourceMapper;
import com.example.backend.model.dto.schoolproxy.*;
import com.example.backend.model.vo.schoolproxy.*;
import com.example.backend.service.port.*;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 成员二学校代申请：主表与明细由本模块写入，学生查找和审核流转经跨成员 Port。 */
@Service
public class SchoolProxyApplicationService implements SchoolProxyApplicationPort {
    private final ApplicationService applications; private final ApplicationMapper mapper;
    private final ApplicationOperationMapper operations; private final ApplicationResourceMapper resources;
    private final ObjectProvider<SchoolProxyStudentQueryPort> studentQueries;
    public SchoolProxyApplicationService(ApplicationService applications, ApplicationMapper mapper, ApplicationOperationMapper operations,
                                         ApplicationResourceMapper resources, ObjectProvider<SchoolProxyStudentQueryPort> studentQueries) {
        this.applications=applications; this.mapper=mapper; this.operations=operations; this.resources=resources;
        this.studentQueries=studentQueries;
    }
    @Override @Transactional public SchoolProxyApplicationVO createDraft(SchoolProxyDraftDTO command, Long operatorUserId) {
        Long previous=operations.findApplicationIdByRequestId(command.getRequestId());
        if(previous!=null) return toView(requireProxy(previous));
        if (!"GREEN_CHANNEL".equals(command.getBatchType())) throw bad("SCHOOL_PROXY_BATCH_TYPE_INVALID", "学校代申请仅支持绿色通道批次");
        SchoolProxyStudentQueryPort query=studentQueries.getIfAvailable();
        if(query==null) throw unavailable("缺少成员一 SchoolProxyStudentQueryPort 实现，不能按学号建立代申请");
        SchoolProxyStudentVO student=query.findEnabledStudentByStudentNo(command.getStudentNo());
        if(student==null || student.getStudentId()==null) throw new ApplicationException("SCHOOL_PROXY_STUDENT_NOT_FOUND", HttpStatus.NOT_FOUND, "学生不存在或已停用");
        ApplicationStateSnapshot state=applications.createSchoolProxyApplication(student.getStudentId(), operatorUserId,
                new ApplicationDraftCommand(ApplicationType.GREEN_CHANNEL, BatchType.GREEN_CHANNEL, command.getBatchId(), command.getRequestId(), command.getApplicationReason()));
        if(!command.getArrearsItems().isEmpty()) {
            applications.replaceArrearsItems(state.applicationId(), state.version(), command.getArrearsItems().stream()
                    .map(x -> new ArrearsItemCommand(x.getFeeItemId(),x.getDeclaredAmount(),x.getArrearsReasonCode())).toList(), operatorUserId);
        }
        state=applications.getRequiredState(state.applicationId());
        if(!command.getGiftItems().isEmpty()) applications.replaceGiftItems(state.applicationId(), state.version(), toGiftItems(command.getBatchId(), command.getGiftItems()), operatorUserId);
        return toView(requireProxy(state.applicationId()));
    }
    @Override public void uploadAttachment(Long applicationId, org.springframework.web.multipart.MultipartFile file, String requestId, Long operatorUserId) {
        throw new ApplicationException("ATTACHMENT_STORAGE_UNAVAILABLE", HttpStatus.NOT_IMPLEMENTED, "附件对象存储、允许类型和大小限制尚未确定，不能安全写入附件");
    }
    @Override @Transactional public SchoolProxyApplicationVO submit(Long applicationId, Integer expectedVersion, String requestId, Long operatorUserId) {
        requireProxy(applicationId);
        throw new ApplicationException("SCHOOL_PROXY_SUBMISSION_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE,
                "正式提交必须先完成附件存储和学院/年级资源预占；当前缺少存储方案及成员一学生组织 ID，不能安全推进审核状态");
    }
    private List<GiftApplicationItemCommand> toGiftItems(Long batchId, List<SchoolProxyGiftItemDTO> items) {
        return items.stream().map(item -> { Long id=resources.findBatchGiftItemId(batchId,item.getGiftItemId());
            if(id==null) throw bad("SCHOOL_PROXY_GIFT_ITEM_INVALID", "礼包物品不属于当前批次"); return new GiftApplicationItemCommand(id,item.getQuantity()); }).toList();
    }
    private Application requireProxy(Long id) { Application a=mapper.findBySource(id,ApplicationSource.SCHOOL_PROXY); if(a==null) throw new ApplicationException("SCHOOL_PROXY_APPLICATION_NOT_FOUND",HttpStatus.NOT_FOUND,"学校代申请不存在"); return a; }
    private SchoolProxyApplicationVO toView(Application a) { SchoolProxyApplicationVO v=new SchoolProxyApplicationVO(); v.setApplicationId(a.getId());v.setApplicationNo(a.getApplicationNo());v.setSource(a.getSource().name());v.setStatus(a.getStatus().name());v.setVersion(a.getVersion());return v; }
    private ApplicationException bad(String code,String message){return new ApplicationException(code,HttpStatus.BAD_REQUEST,message);} private ApplicationException unavailable(String message){return new ApplicationException("DEPENDENCY_UNAVAILABLE",HttpStatus.SERVICE_UNAVAILABLE,message);}
}
