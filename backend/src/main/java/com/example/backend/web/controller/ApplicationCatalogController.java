package com.example.backend.web.controller;

import com.example.backend.model.dto.CatalogItemCommand;
import com.example.backend.model.dto.CatalogItemView;
import com.example.backend.model.dto.FeeAmountOptionCommand;
import com.example.backend.model.dto.FeeAmountOptionView;
import com.example.backend.model.dto.GiftItemCommand;
import com.example.backend.model.dto.GiftItemView;
import com.example.backend.service.impl.ApplicationCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成员二自有配置接口。管理员鉴权将在成员一的可信身份上下文合入后统一接入。
 */
@RestController
@RequestMapping(value = "/api", produces = "application/json;charset=UTF-8")
public class ApplicationCatalogController {
    private final ApplicationCatalogService service;

    public ApplicationCatalogController(ApplicationCatalogService service) {
        this.service = service;
    }

    @GetMapping("/fee-items")
    public List<CatalogItemView> feeItems(@RequestParam(defaultValue = "false") boolean includeDisabled) {
        return service.findFeeItems(includeDisabled);
    }

    @PostMapping("/fee-items")
    @ResponseStatus(HttpStatus.CREATED)
    public CatalogItemView createFeeItem(@Valid @RequestBody CatalogItemCommand command) {
        return service.createFeeItem(command);
    }

    @PutMapping("/fee-items/{id}")
    public CatalogItemView updateFeeItem(@PathVariable Long id, @Valid @RequestBody CatalogItemCommand command) {
        return service.updateFeeItem(id, command);
    }

    @DeleteMapping("/fee-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFeeItem(@PathVariable Long id) {
        service.deleteFeeItem(id);
    }

    @PostMapping("/fee-items/{id}/merge")
    public CatalogItemView mergeFeeItem(@PathVariable Long id, @RequestParam Long targetId) {
        return service.mergeFeeItem(id, targetId);
    }

    @GetMapping("/fee-amount-options")
    public List<FeeAmountOptionView> feeAmountOptions(@RequestParam(required = false) Long feeItemId,
                                                        @RequestParam(defaultValue = "false") boolean includeDisabled) {
        return service.findFeeAmountOptions(feeItemId, includeDisabled);
    }

    @PostMapping("/fee-amount-options")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeAmountOptionView createFeeAmountOption(@Valid @RequestBody FeeAmountOptionCommand command) {
        return service.createFeeAmountOption(command);
    }

    @PutMapping("/fee-amount-options/{id}")
    public FeeAmountOptionView updateFeeAmountOption(@PathVariable Long id,
                                                      @Valid @RequestBody FeeAmountOptionCommand command) {
        return service.updateFeeAmountOption(id, command);
    }

    @DeleteMapping("/fee-amount-options/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFeeAmountOption(@PathVariable Long id) {
        service.deleteFeeAmountOption(id);
    }

    @GetMapping("/gift-items")
    public List<GiftItemView> giftItems(@RequestParam(defaultValue = "false") boolean includeDisabled) {
        return service.findGiftItems(includeDisabled);
    }

    @PostMapping("/gift-items")
    @ResponseStatus(HttpStatus.CREATED)
    public GiftItemView createGiftItem(@Valid @RequestBody GiftItemCommand command) {
        return service.createGiftItem(command);
    }

    @PutMapping("/gift-items/{id}")
    public GiftItemView updateGiftItem(@PathVariable Long id, @Valid @RequestBody GiftItemCommand command) {
        return service.updateGiftItem(id, command);
    }

    @DeleteMapping("/gift-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGiftItem(@PathVariable Long id) {
        service.deleteGiftItem(id);
    }
}
