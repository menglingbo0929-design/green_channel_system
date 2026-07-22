package com.example.backend.service.port;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.supplement.SupplementCreateDTO;
import com.example.backend.model.dto.supplement.SupplementQueryDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.model.vo.supplement.SupplementApplicationVO;

/**
 * 成员二申请模块需要实现的线下补录适配边界。
 *
 * <p>application、申请明细和资源表归成员二所有。成员四只通过此接口发起
 * 创建和读取，绝不新建 Mapper 直接操作成员二的表。</p>
 *
 * <p>正式实现由申请域的 {@code SupplementApplicationService} 提供，包含草稿、
 * 明细、资源确认、requestId 幂等以及补录历史/详情。</p>
 */
public interface SupplementApplicationPort {

    /**
     * 创建 source=SUPPLEMENT、status=DRAFT 的申请及真实明细，并确认线下已办理资源。
     * 返回值必须携带申请 ID、编号、版本以及供页面展示的学生快照。
     */
    SupplementApplicationVO createSupplementDraft(
            SupplementCreateDTO command,
            SchoolProxyStudentVO student,
            String batchType,
            Long operatorUserId
    );

    /** 只查询 source=SUPPLEMENT 的历史记录。 */
    Page<SupplementApplicationVO> findSupplementPage(
            SupplementQueryDTO query,
            PageDTO page,
            Long operatorUserId
    );

    /** 按申请 ID 查询补录详情；非补录来源必须按不存在处理。 */
    SupplementApplicationVO findSupplementById(Long applicationId, Long operatorUserId);
}
