package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.supplement.SupplementCreateDTO;
import com.example.backend.model.dto.supplement.SupplementQueryDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.model.vo.supplement.SupplementApplicationVO;

/**
 * 学校端线下补录申请服务。
 */
public interface ISupplementApplicationService {

    SchoolProxyStudentVO findStudent(String studentNo, Long operatorUserId);

    Page<SupplementApplicationVO> pageSupplements(
            SupplementQueryDTO query,
            PageDTO page,
            Long operatorUserId
    );

    SupplementApplicationVO getSupplement(Long applicationId, Long operatorUserId);

    SupplementApplicationVO createSupplement(
            SupplementCreateDTO request,
            Long operatorUserId
    );
}
