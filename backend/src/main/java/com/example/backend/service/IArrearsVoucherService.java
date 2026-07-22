package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.voucher.ArrearsVoucherQueryDTO;
import com.example.backend.model.vo.voucher.ArrearsVoucherVO;

/** 6.1.2 欠费单据的学校查询、学生本人查看与打印数据服务。 */
public interface IArrearsVoucherService {
    Page<ArrearsVoucherVO> pageForSchool(ArrearsVoucherQueryDTO query, PageDTO page, Long userId);
    ArrearsVoucherVO getForSchool(String voucherNo, Long userId);
    ArrearsVoucherVO getPrintData(String voucherNo, Long userId);
    ArrearsVoucherVO getForStudent(String voucherNo, Long userId);
}
