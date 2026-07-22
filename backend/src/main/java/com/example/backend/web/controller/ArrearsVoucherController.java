package com.example.backend.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.voucher.ArrearsVoucherQueryDTO;
import com.example.backend.model.vo.voucher.ArrearsVoucherVO;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.IArrearsVoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/** 6.1.2 单据接口：学校列表、预览、打印数据和学生本人查看。 */
@RestController
public class ArrearsVoucherController {

    @Autowired
    private IArrearsVoucherService service;
    @Autowired
    private ICurrentUserProvider currentUserProvider;

    @GetMapping("/api/arrears-vouchers")
    public JsonResponse<Page<ArrearsVoucherVO>> page(ArrearsVoucherQueryDTO query, PageDTO page){return JsonResponse.success(service.pageForSchool(query,page,currentUserId()));}
    @GetMapping("/api/arrears-vouchers/{voucherNo}")
    public JsonResponse<ArrearsVoucherVO> detail(@PathVariable String voucherNo){return JsonResponse.success(service.getForSchool(voucherNo,currentUserId()));}
    @GetMapping("/api/arrears-vouchers/{voucherNo}/print")
    public JsonResponse<ArrearsVoucherVO> print(@PathVariable String voucherNo){return JsonResponse.success(service.getPrintData(voucherNo,currentUserId()));}
    @GetMapping("/api/student/arrears-vouchers/{voucherNo}")
    public JsonResponse<ArrearsVoucherVO> mine(@PathVariable String voucherNo){return JsonResponse.success(service.getForStudent(voucherNo,currentUserId()));}

    private Long currentUserId(){return currentUserProvider.getRequiredUser().getUserId();}
}
