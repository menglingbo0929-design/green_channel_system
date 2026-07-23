package com.example.backend.service.impl;

import com.example.backend.model.domain.QuotaScope;
import com.example.backend.model.dto.*;
import com.example.backend.common.exception.ApplicationException;
import com.example.backend.mapper.ApplicationCatalogMapper;
import com.example.backend.mapper.ApplicationResourceConfigMapper;
import com.example.backend.model.dto.CollegeOption;
import com.example.backend.model.dto.GradeOption;
import com.example.backend.service.BatchQueryService;
import com.example.backend.service.OrganizationQueryService;
import java.util.List;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 礼包库存与学院/年级资源配额；所有成员一数据均经其公开查询服务读取。 */
@Service
public class ApplicationResourceConfigService {
    private final ApplicationResourceConfigMapper mapper;
    private final ApplicationCatalogMapper catalogMapper;
    private final BatchQueryService batchQueryService;
    private final OrganizationQueryService organizationQueryService;

    public ApplicationResourceConfigService(ApplicationResourceConfigMapper mapper, ApplicationCatalogMapper catalogMapper,
                                            BatchQueryService batchQueryService, OrganizationQueryService organizationQueryService) {
        this.mapper = mapper; this.catalogMapper = catalogMapper;
        this.batchQueryService = batchQueryService; this.organizationQueryService = organizationQueryService;
    }

    public List<BatchGiftItemView> findBatchGiftItems(Long batchId) { requireBatch(batchId); return mapper.findBatchGiftItems(batchId); }
    @Transactional public BatchGiftItemView createBatchGiftItem(BatchGiftItemCommand command) {
        requireBatch(command.batchId());
        if (catalogMapper.findGiftItem(command.giftItemId()) == null) throw notFound("GIFT_ITEM_NOT_FOUND", "礼包物品不存在或已删除");
        if (mapper.countBatchGiftItem(command.batchId(), command.giftItemId()) > 0) throw conflict("BATCH_GIFT_ITEM_EXISTS", "该批次已配置此礼包物品");
        mapper.insertBatchGiftItem(command.batchId(), command.giftItemId(), command.stockTotal(), command.perStudentLimit());
        return requiredBatchGiftItem(mapper.lastInsertId());
    }
    @Transactional public BatchGiftItemView updateBatchGiftItem(Long id, UpdateBatchGiftItemCommand command) {
        requiredBatchGiftItem(id);
        if (mapper.updateBatchGiftItem(id, command.stockTotal(), command.perStudentLimit(), command.version()) != 1) {
            throw conflict("BATCH_GIFT_ITEM_VERSION_CONFLICT", "库存已被占用或配置版本已变化");
        }
        return requiredBatchGiftItem(id);
    }
    @Transactional public void deleteBatchGiftItem(Long id) {
        requiredBatchGiftItem(id);
        if (mapper.countGiftApplicationItems(id) > 0) throw conflict("BATCH_GIFT_ITEM_IN_USE", "已有申请使用此礼包物品，不能删除");
        if (mapper.deleteBatchGiftItem(id) != 1) throw conflict("BATCH_GIFT_ITEM_IN_USE", "库存已被占用或已使用，不能删除");
    }

    public List<GiftQuotaView> findGiftQuotas(Long batchId, QuotaScope scope) {
        requireBatch(batchId);
        return rawGiftQuotas(batchId, scope).stream().map(v -> decorate(v, scope)).toList();
    }
    @Transactional public GiftQuotaView createGiftQuota(GiftQuotaCommand command) {
        requireBatch(command.batchId()); requireTarget(command.scope(), command.batchId(), command.targetId());
        if (countGift(command.scope(), command.batchId(), command.targetId()) > 0) throw conflict("GIFT_QUOTA_EXISTS", "该批次和对象已有礼包名额配置");
        if (command.scope() == QuotaScope.COLLEGE) mapper.insertCollegeGiftQuota(command.batchId(), command.targetId(), command.quotaTotal());
        else mapper.insertGradeGiftQuota(command.batchId(), command.targetId(), command.quotaTotal());
        return requiredGiftQuota(command.scope(), mapper.lastInsertId());
    }
    @Transactional public GiftQuotaView updateGiftQuota(Long id, QuotaScope scope, UpdateGiftQuotaCommand command) {
        requiredGiftQuota(scope, id);
        int updated = scope == QuotaScope.COLLEGE ? mapper.updateCollegeGiftQuota(id, command.quotaTotal(), command.version())
                : mapper.updateGradeGiftQuota(id, command.quotaTotal(), command.version());
        if (updated != 1) throw conflict("GIFT_QUOTA_VERSION_CONFLICT", "名额已被占用或配置版本已变化");
        return requiredGiftQuota(scope, id);
    }
    @Transactional public void deleteGiftQuota(Long id, QuotaScope scope) {
        requiredGiftQuota(scope, id);
        int deleted = scope == QuotaScope.COLLEGE ? mapper.deleteCollegeGiftQuota(id) : mapper.deleteGradeGiftQuota(id);
        if (deleted != 1) throw conflict("GIFT_QUOTA_IN_USE", "名额已被占用或已使用，不能删除");
    }

    public List<SubsidyQuotaView> findSubsidyQuotas(Long batchId, QuotaScope scope) {
        requireBatch(batchId);
        return rawSubsidyQuotas(batchId, scope).stream().map(v -> decorate(v, scope)).toList();
    }
    @Transactional public SubsidyQuotaView createSubsidyQuota(SubsidyQuotaCommand command) {
        requireBatch(command.batchId()); requireTarget(command.scope(), command.batchId(), command.targetId());
        if (countSubsidy(command.scope(), command.batchId(), command.targetId()) > 0) throw conflict("SUBSIDY_QUOTA_EXISTS", "该批次和对象已有补助额度配置");
        if (command.scope() == QuotaScope.COLLEGE) mapper.insertCollegeSubsidyQuota(command.batchId(), command.targetId(), command.quotaAmount());
        else mapper.insertGradeSubsidyQuota(command.batchId(), command.targetId(), command.quotaAmount());
        return requiredSubsidyQuota(command.scope(), mapper.lastInsertId());
    }
    @Transactional public SubsidyQuotaView updateSubsidyQuota(Long id, QuotaScope scope, UpdateSubsidyQuotaCommand command) {
        requiredSubsidyQuota(scope, id);
        int updated = scope == QuotaScope.COLLEGE ? mapper.updateCollegeSubsidyQuota(id, command.quotaAmount(), command.version())
                : mapper.updateGradeSubsidyQuota(id, command.quotaAmount(), command.version());
        if (updated != 1) throw conflict("SUBSIDY_QUOTA_VERSION_CONFLICT", "额度已被占用或配置版本已变化");
        return requiredSubsidyQuota(scope, id);
    }
    @Transactional public void deleteSubsidyQuota(Long id, QuotaScope scope) {
        requiredSubsidyQuota(scope, id);
        int deleted = scope == QuotaScope.COLLEGE ? mapper.deleteCollegeSubsidyQuota(id) : mapper.deleteGradeSubsidyQuota(id);
        if (deleted != 1) throw conflict("SUBSIDY_QUOTA_IN_USE", "额度已被占用或已使用，不能删除");
    }

    public List<CollegeOption> listColleges() { return organizationQueryService.listColleges(); }
    public List<GradeOption> listGrades() { return organizationQueryService.listGrades(); }

    private List<GiftQuotaView> rawGiftQuotas(Long batchId, QuotaScope scope) { return scope == QuotaScope.COLLEGE ? mapper.findCollegeGiftQuotas(batchId) : mapper.findGradeGiftQuotas(batchId); }
    private List<SubsidyQuotaView> rawSubsidyQuotas(Long batchId, QuotaScope scope) { return scope == QuotaScope.COLLEGE ? mapper.findCollegeSubsidyQuotas(batchId) : mapper.findGradeSubsidyQuotas(batchId); }
    private int countGift(QuotaScope scope, Long batchId, Long targetId) { return scope == QuotaScope.COLLEGE ? mapper.countCollegeGiftQuota(batchId, targetId) : mapper.countGradeGiftQuota(batchId, targetId); }
    private int countSubsidy(QuotaScope scope, Long batchId, Long targetId) { return scope == QuotaScope.COLLEGE ? mapper.countCollegeSubsidyQuota(batchId, targetId) : mapper.countGradeSubsidyQuota(batchId, targetId); }
    private BatchGiftItemView requiredBatchGiftItem(Long id) { BatchGiftItemView view=mapper.findBatchGiftItem(id); if(view==null) throw notFound("BATCH_GIFT_ITEM_NOT_FOUND", "批次礼包物品不存在或已删除"); return view; }
    private GiftQuotaView requiredGiftQuota(QuotaScope scope, Long id) { GiftQuotaView view=scope==QuotaScope.COLLEGE?mapper.findCollegeGiftQuota(id):mapper.findGradeGiftQuota(id); if(view==null) throw notFound("GIFT_QUOTA_NOT_FOUND", "礼包名额配置不存在或已删除"); return decorate(view, scope); }
    private SubsidyQuotaView requiredSubsidyQuota(QuotaScope scope, Long id) { SubsidyQuotaView view=scope==QuotaScope.COLLEGE?mapper.findCollegeSubsidyQuota(id):mapper.findGradeSubsidyQuota(id); if(view==null) throw notFound("SUBSIDY_QUOTA_NOT_FOUND", "补助额度配置不存在或已删除"); return decorate(view, scope); }
    private GiftQuotaView decorate(GiftQuotaView v, QuotaScope scope) { return new GiftQuotaView(v.id(),v.batchId(),scope,v.targetId(),targetName(scope,v.targetId()),v.quotaTotal(),v.reservedCount(),v.usedCount(),v.version()); }
    private SubsidyQuotaView decorate(SubsidyQuotaView v, QuotaScope scope) { return new SubsidyQuotaView(v.id(),v.batchId(),scope,v.targetId(),targetName(scope,v.targetId()),v.quotaAmount(),v.reservedAmount(),v.usedAmount(),v.version()); }
    private void requireBatch(Long batchId) { try { batchQueryService.getRequiredBatch(batchId); } catch (RuntimeException e) { throw new ApplicationException("BATCH_NOT_FOUND", HttpStatus.BAD_REQUEST, "批次不存在或不可用"); } }
    private void requireTarget(QuotaScope scope, Long batchId, Long targetId) {
        if (targetName(scope, targetId) == null) throw new ApplicationException("QUOTA_TARGET_NOT_FOUND", HttpStatus.BAD_REQUEST, "学院或年级不存在或已停用");
        if (scope == QuotaScope.GRADE && !batchQueryService.isGradeEligible(batchId, targetId)) {
            throw new ApplicationException("GRADE_NOT_ELIGIBLE_FOR_BATCH", HttpStatus.BAD_REQUEST, "该年级不在批次适用范围内");
        }
    }
    private String targetName(QuotaScope scope, Long targetId) {
        if (scope == QuotaScope.COLLEGE) return organizationQueryService.listColleges().stream().filter(x -> x.getId().equals(targetId)).map(x -> x.getCollegeName()).findFirst().orElse(null);
        return organizationQueryService.listGrades().stream().filter(x -> x.getId().equals(targetId)).map(x -> x.getGradeName()).findFirst().orElse(null);
    }
    private ApplicationException conflict(String code, String message) { return new ApplicationException(code, HttpStatus.CONFLICT, message); }
    private ApplicationException notFound(String code, String message) { return new ApplicationException(code, HttpStatus.NOT_FOUND, message); }
}
