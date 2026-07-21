package com.example.backend.application.web;

import com.example.backend.application.domain.QuotaScope;
import com.example.backend.application.dto.*;
import com.example.backend.application.service.ApplicationResourceConfigService;
import com.example.backend.model.dto.CollegeOption;
import com.example.backend.model.dto.GradeOption;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 成员二资源配置 API；批次、学院、年级仅通过成员一的查询服务校验和读取。 */
@RestController
@RequestMapping(value = "/api/application-resources", produces = "application/json;charset=UTF-8")
public class ApplicationResourceConfigController {
    private final ApplicationResourceConfigService service;
    public ApplicationResourceConfigController(ApplicationResourceConfigService service) { this.service = service; }

    @GetMapping("/batch-gift-items") public List<BatchGiftItemView> batchGiftItems(@RequestParam Long batchId) { return service.findBatchGiftItems(batchId); }
    @PostMapping("/batch-gift-items") @ResponseStatus(HttpStatus.CREATED) public BatchGiftItemView createBatchGiftItem(@Valid @RequestBody BatchGiftItemCommand command) { return service.createBatchGiftItem(command); }
    @PutMapping("/batch-gift-items/{id}") public BatchGiftItemView updateBatchGiftItem(@PathVariable Long id, @Valid @RequestBody UpdateBatchGiftItemCommand command) { return service.updateBatchGiftItem(id, command); }
    @DeleteMapping("/batch-gift-items/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteBatchGiftItem(@PathVariable Long id) { service.deleteBatchGiftItem(id); }

    @GetMapping("/gift-quotas") public List<GiftQuotaView> giftQuotas(@RequestParam Long batchId, @RequestParam QuotaScope scope) { return service.findGiftQuotas(batchId, scope); }
    @PostMapping("/gift-quotas") @ResponseStatus(HttpStatus.CREATED) public GiftQuotaView createGiftQuota(@Valid @RequestBody GiftQuotaCommand command) { return service.createGiftQuota(command); }
    @PutMapping("/gift-quotas/{id}") public GiftQuotaView updateGiftQuota(@PathVariable Long id, @RequestParam QuotaScope scope, @Valid @RequestBody UpdateGiftQuotaCommand command) { return service.updateGiftQuota(id, scope, command); }
    @DeleteMapping("/gift-quotas/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteGiftQuota(@PathVariable Long id, @RequestParam QuotaScope scope) { service.deleteGiftQuota(id, scope); }

    @GetMapping("/subsidy-quotas") public List<SubsidyQuotaView> subsidyQuotas(@RequestParam Long batchId, @RequestParam QuotaScope scope) { return service.findSubsidyQuotas(batchId, scope); }
    @PostMapping("/subsidy-quotas") @ResponseStatus(HttpStatus.CREATED) public SubsidyQuotaView createSubsidyQuota(@Valid @RequestBody SubsidyQuotaCommand command) { return service.createSubsidyQuota(command); }
    @PutMapping("/subsidy-quotas/{id}") public SubsidyQuotaView updateSubsidyQuota(@PathVariable Long id, @RequestParam QuotaScope scope, @Valid @RequestBody UpdateSubsidyQuotaCommand command) { return service.updateSubsidyQuota(id, scope, command); }
    @DeleteMapping("/subsidy-quotas/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSubsidyQuota(@PathVariable Long id, @RequestParam QuotaScope scope) { service.deleteSubsidyQuota(id, scope); }

    @GetMapping("/colleges") public List<CollegeOption> colleges() { return service.listColleges(); }
    @GetMapping("/grades") public List<GradeOption> grades() { return service.listGrades(); }
}
