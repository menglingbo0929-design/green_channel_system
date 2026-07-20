package com.example.backend.application.service;

import com.example.backend.application.dto.CatalogItemCommand;
import com.example.backend.application.dto.CatalogItemView;
import com.example.backend.application.dto.FeeAmountOptionCommand;
import com.example.backend.application.dto.FeeAmountOptionView;
import com.example.backend.application.exception.ApplicationException;
import com.example.backend.application.mapper.ApplicationCatalogMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 成员二自有的费用和礼包基础配置业务服务。 */
@Service
public class ApplicationCatalogService {
    private final ApplicationCatalogMapper mapper;

    public ApplicationCatalogService(ApplicationCatalogMapper mapper) {
        this.mapper = mapper;
    }

    public List<CatalogItemView> findFeeItems(boolean includeDisabled) {
        return mapper.findFeeItems(includeDisabled);
    }

    @Transactional
    public CatalogItemView createFeeItem(CatalogItemCommand command) {
        validateUniqueFeeItemName(command.name(), null);
        mapper.insertFeeItem(normalizeName(command.name()), command.enabled());
        return requiredFeeItem(mapper.lastInsertId());
    }

    @Transactional
    public CatalogItemView updateFeeItem(Long id, CatalogItemCommand command) {
        requiredFeeItem(id);
        validateUniqueFeeItemName(command.name(), id);
        if (mapper.updateFeeItem(id, normalizeName(command.name()), command.enabled()) != 1) {
            throw notFound("FEE_ITEM_NOT_FOUND", "欠费项目不存在");
        }
        return requiredFeeItem(id);
    }

    @Transactional
    public void deleteFeeItem(Long id) {
        requiredFeeItem(id);
        if (mapper.countActiveArrearsByFeeItemId(id) > 0) {
            throw conflict("FEE_ITEM_IN_USE", "已有申请使用该欠费项目，不能删除");
        }
        if (mapper.deleteFeeItem(id) != 1) throw notFound("FEE_ITEM_NOT_FOUND", "欠费项目不存在");
    }

    public List<FeeAmountOptionView> findFeeAmountOptions(Long feeItemId, boolean includeDisabled) {
        if (feeItemId != null) requiredFeeItem(feeItemId);
        return mapper.findFeeAmountOptions(feeItemId, includeDisabled);
    }

    @Transactional
    public FeeAmountOptionView createFeeAmountOption(FeeAmountOptionCommand command) {
        requiredEnabledFeeItem(command.feeItemId());
        validateUniqueFeeAmount(command.feeItemId(), command.amount(), null);
        mapper.insertFeeAmountOption(command.feeItemId(), command.amount(), command.enabled());
        return requiredFeeAmountOption(mapper.lastInsertId());
    }

    @Transactional
    public FeeAmountOptionView updateFeeAmountOption(Long id, FeeAmountOptionCommand command) {
        requiredFeeAmountOption(id);
        requiredEnabledFeeItem(command.feeItemId());
        validateUniqueFeeAmount(command.feeItemId(), command.amount(), id);
        if (mapper.updateFeeAmountOption(id, command.feeItemId(), command.amount(), command.enabled()) != 1) {
            throw notFound("FEE_AMOUNT_OPTION_NOT_FOUND", "金额档位不存在");
        }
        return requiredFeeAmountOption(id);
    }

    @Transactional
    public void deleteFeeAmountOption(Long id) {
        requiredFeeAmountOption(id);
        if (mapper.deleteFeeAmountOption(id) != 1) throw notFound("FEE_AMOUNT_OPTION_NOT_FOUND", "金额档位不存在");
    }

    public List<CatalogItemView> findGiftItems(boolean includeDisabled) {
        return mapper.findGiftItems(includeDisabled);
    }

    @Transactional
    public CatalogItemView createGiftItem(CatalogItemCommand command) {
        validateUniqueGiftItemName(command.name(), null);
        mapper.insertGiftItem(normalizeName(command.name()), command.enabled());
        return requiredGiftItem(mapper.lastInsertId());
    }

    @Transactional
    public CatalogItemView updateGiftItem(Long id, CatalogItemCommand command) {
        requiredGiftItem(id);
        validateUniqueGiftItemName(command.name(), id);
        if (mapper.updateGiftItem(id, normalizeName(command.name()), command.enabled()) != 1) {
            throw notFound("GIFT_ITEM_NOT_FOUND", "礼包物品不存在");
        }
        return requiredGiftItem(id);
    }

    @Transactional
    public void deleteGiftItem(Long id) {
        requiredGiftItem(id);
        if (mapper.countActiveBatchGiftItemsByGiftItemId(id) > 0) {
            throw conflict("GIFT_ITEM_IN_USE", "已有批次配置使用该礼包物品，不能删除");
        }
        if (mapper.deleteGiftItem(id) != 1) throw notFound("GIFT_ITEM_NOT_FOUND", "礼包物品不存在");
    }

    private CatalogItemView requiredFeeItem(Long id) {
        CatalogItemView item = mapper.findFeeItem(id);
        if (item == null) throw notFound("FEE_ITEM_NOT_FOUND", "欠费项目不存在");
        return item;
    }

    private void requiredEnabledFeeItem(Long id) {
        if (!requiredFeeItem(id).enabled()) {
            throw new ApplicationException("FEE_ITEM_DISABLED", HttpStatus.BAD_REQUEST, "欠费项目已停用，不能配置金额档位");
        }
    }

    private FeeAmountOptionView requiredFeeAmountOption(Long id) {
        FeeAmountOptionView option = mapper.findFeeAmountOption(id);
        if (option == null) throw notFound("FEE_AMOUNT_OPTION_NOT_FOUND", "金额档位不存在");
        return option;
    }

    private CatalogItemView requiredGiftItem(Long id) {
        CatalogItemView item = mapper.findGiftItem(id);
        if (item == null) throw notFound("GIFT_ITEM_NOT_FOUND", "礼包物品不存在");
        return item;
    }

    private void validateUniqueFeeItemName(String name, Long excludedId) {
        if (mapper.countFeeItemsByName(normalizeName(name), excludedId) > 0) {
            throw conflict("FEE_ITEM_NAME_EXISTS", "欠费项目名称已存在");
        }
    }

    private void validateUniqueGiftItemName(String name, Long excludedId) {
        if (mapper.countGiftItemsByName(normalizeName(name), excludedId) > 0) {
            throw conflict("GIFT_ITEM_NAME_EXISTS", "礼包物品名称已存在");
        }
    }

    private void validateUniqueFeeAmount(Long feeItemId, BigDecimal amount, Long excludedId) {
        if (mapper.countFeeAmountOptions(feeItemId, amount, excludedId) > 0) {
            throw conflict("FEE_AMOUNT_OPTION_EXISTS", "该欠费项目的金额档位已存在");
        }
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private ApplicationException conflict(String code, String message) {
        return new ApplicationException(code, HttpStatus.CONFLICT, message);
    }

    private ApplicationException notFound(String code, String message) {
        return new ApplicationException(code, HttpStatus.NOT_FOUND, message);
    }
}
