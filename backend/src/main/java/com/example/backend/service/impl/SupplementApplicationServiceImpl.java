package com.example.backend.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.supplement.SupplementCreateDTO;
import com.example.backend.model.dto.supplement.SupplementQueryDTO;
import com.example.backend.model.vo.schoolproxy.SchoolProxyStudentVO;
import com.example.backend.model.vo.supplement.SupplementApplicationVO;
import com.example.backend.service.ISupplementApplicationService;
import com.example.backend.service.port.SchoolProxyStudentQueryPort;
import com.example.backend.service.port.SupplementApplicationPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * 6.1.4 线下补录演示实现。
 *
 * <p>保持视频中的字段注入和顺序调用：查学生、创建补录草稿、生成自动审核记录、
 * 组装最终结果。请求的基础必填项继续由 DTO 注解校验，Service 不再包装异常。</p>
 */
@Service
public class SupplementApplicationServiceImpl implements ISupplementApplicationService {

    @Autowired
    private ObjectProvider<SchoolProxyStudentQueryPort> studentPortProvider;

    @Autowired
    private ObjectProvider<SupplementApplicationPort> applicationPortProvider;

    @Override
    public SchoolProxyStudentVO findStudent(String studentNo, Long operatorUserId) {
        return studentPortProvider.getObject()
                .findEnabledStudentByStudentNo(studentNo);
    }

    @Override
    public Page<SupplementApplicationVO> pageSupplements(
            SupplementQueryDTO query,
            PageDTO page,
            Long operatorUserId
    ) {
        PageDTO actualPage = page == null ? new PageDTO() : page;
        return applicationPortProvider.getObject()
                .findSupplementPage(query, actualPage, operatorUserId);
    }

    @Override
    public SupplementApplicationVO getSupplement(
            Long applicationId,
            Long operatorUserId
    ) {
        return applicationPortProvider.getObject()
                .findSupplementById(applicationId, operatorUserId);
    }

    @Override
    @Transactional
    public SupplementApplicationVO createSupplement(
            SupplementCreateDTO request,
            Long operatorUserId
    ) {
        if (request.getArrearsItems() == null) {
            request.setArrearsItems(new ArrayList<>());
        }
        if (request.getGiftItems() == null) {
            request.setGiftItems(new ArrayList<>());
        }

        SchoolProxyStudentVO student = studentPortProvider.getObject()
                .findEnabledStudentByStudentNo(request.getStudentNo());
        String batchType = "GREEN_CHANNEL".equals(request.getApplicationType())
                ? "GREEN_CHANNEL"
                : "SUBSIDY";
        /*
         * 正式的成员二 SupplementApplicationPort 已在其事务内完成资源占用、
         * 自动审核桥接和最终状态回写；成员四这里仅负责调用，不再二次调用成员三，
         * 避免同一补录申请重复推进审核状态。
         */
        return applicationPortProvider.getObject()
                .createSupplementDraft(request, student, batchType, operatorUserId);
    }
}
