package com.example.backend.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.JsonResponse;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.voucher.ArrearsVoucherQueryDTO;
import com.example.backend.model.vo.voucher.ArrearsVoucherVO;
import com.example.backend.security.ICurrentUserProvider;
import com.example.backend.service.IArrearsVoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
//
/** 单据接口：学校列表、预览、打印数据和学生本人查看。 */
//主要解决学校端业务处理中的欠费确认部分
@RestController
@RequiredArgsConstructor
public class ArrearsVoucherController {
    private final IArrearsVoucherService service;
    private final ICurrentUserProvider currentUserProvider;
    //负责是欠费确认页面中表的展示和分页
    @GetMapping("/api/arrears-vouchers")
    public JsonResponse<Page<ArrearsVoucherVO>> page(ArrearsVoucherQueryDTO query, PageDTO page){return JsonResponse.success(service.pageForSchool(query,page,currentUserId()));}
    //学校审核的详情页弹窗
    @GetMapping("/api/arrears-vouchers/{voucherNo}")
    public JsonResponse<ArrearsVoucherVO> detail(@PathVariable String voucherNo){return JsonResponse.success(service.getForSchool(voucherNo,currentUserId()));}
    //业务处理——单据，用于依据单据编号输入之后打印单据功能
    @GetMapping("/api/arrears-vouchers/{voucherNo}/print")
    public JsonResponse<ArrearsVoucherVO> print(@PathVariable String voucherNo){return JsonResponse.success(service.getPrintData(voucherNo,currentUserId()));}
    //学生查看自己的欠费确认单
    @GetMapping("/api/student/arrears-vouchers/{voucherNo}")
    public JsonResponse<ArrearsVoucherVO> mine(@PathVariable String voucherNo){return JsonResponse.success(service.getForStudent(voucherNo,currentUserId()));}
    //从JWT登录信息中获得用户的ID，用于判断当前用户是谁
    private Long currentUserId(){return currentUserProvider.getRequiredUser().getUserId();}
}
