package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.supplement.SupplementCreateDTO;
import com.example.backend.model.dto.supplement.SupplementQueryDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.model.vo.supplement.SupplementApplicationVO;

/** 6.1.4 线下补录业务接口，供 Controller 调用。 */
public interface ISupplementApplicationService {

    /** 按学号查询可被学校补录的学生最小信息。 */
    SchoolProxyStudentVO findStudent(String studentNo, Long operatorUserId);

    /** 分页查询学校历史补录记录。 */
    Page<SupplementApplicationVO> pageSupplements(
            SupplementQueryDTO query,
            PageDTO page,
            Long operatorUserId
    );

    /** 查询一条补录详情，只允许返回 source=SUPPLEMENT 的申请。 */
    SupplementApplicationVO getSupplement(Long applicationId, Long operatorUserId);

    /** 在同一事务中创建补录申请并完成自动审核。 */
    SupplementApplicationVO createSupplement(SupplementCreateDTO request, Long operatorUserId);
}
