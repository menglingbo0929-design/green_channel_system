package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.mapper.ArrearsConfirmationMapper;
import com.example.backend.model.domain.ArrearsConfirmation;
import com.example.backend.model.dto.PageDTO;
import com.example.backend.model.dto.voucher.ArrearsVoucherQueryDTO;
import com.example.backend.model.vo.voucher.ArrearsVoucherApplicantSnapshot;
import com.example.backend.model.vo.voucher.ArrearsVoucherVO;
import com.example.backend.service.IArrearsVoucherService;
import com.example.backend.service.port.ArrearsVoucherAccessPort;
import com.example.backend.service.port.ArrearsVoucherApplicantQueryPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 6.1.2 欠费单据演示实现。
 *
 * <p>直接读取成员四确认表，再通过成员二快照和成员一权限接口补齐展示信息。
 * 不在 Service 中增加自定义异常处理。</p>
 */
@Service
public class ArrearsVoucherServiceImpl implements IArrearsVoucherService {

    @Autowired
    private ArrearsConfirmationMapper confirmationMapper;

    @Autowired
    private ObjectProvider<ArrearsVoucherApplicantQueryPort> applicantPort;

    @Autowired
    private ObjectProvider<ArrearsVoucherAccessPort> accessPort;

    @Override
    public Page<ArrearsVoucherVO> pageForSchool(
            ArrearsVoucherQueryDTO query,
            PageDTO page,
            Long userId
    ) {
        access().checkSchoolUser(userId);
        int current = page == null || page.getPageNo() == null ? 1 : page.getPageNo();
        int size = page == null || page.getPageSize() == null ? 10 : page.getPageSize();

        Page<ArrearsConfirmation> source = confirmationMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<ArrearsConfirmation>()
                        .eq(ArrearsConfirmation::getDeleted, 0L)
                        .like(query != null && query.getVoucherNo() != null,
                                ArrearsConfirmation::getVoucherNo,
                                query == null ? null : query.getVoucherNo())
                        .orderByDesc(ArrearsConfirmation::getConfirmedAt)
        );
        return assemblePage(source, null);
    }

    @Override
    public ArrearsVoucherVO getForSchool(String voucherNo, Long userId) {
        access().checkSchoolUser(userId);
        return assemble(one(voucherNo), null);
    }

    @Override
    public ArrearsVoucherVO getPrintData(String voucherNo, Long userId) {
        access().checkSchoolUser(userId);
        return assemble(one(voucherNo), LocalDateTime.now());
    }

    @Override
    public ArrearsVoucherVO getForStudent(String voucherNo, Long userId) {
        ArrearsConfirmation confirmation = one(voucherNo);
        access().checkStudentOwnsApplication(userId, confirmation.getApplicationId());
        return assemble(confirmation, null);
    }

    private ArrearsConfirmation one(String voucherNo) {
        return confirmationMapper.selectOne(
                new LambdaQueryWrapper<ArrearsConfirmation>()
                        .eq(ArrearsConfirmation::getVoucherNo, voucherNo)
                        .eq(ArrearsConfirmation::getDeleted, 0L)
        );
    }

    private Page<ArrearsVoucherVO> assemblePage(
            Page<ArrearsConfirmation> source,
            LocalDateTime printTime
    ) {
        Map<Long, ArrearsVoucherApplicantSnapshot> snapshots = applicants(
                source.getRecords().stream()
                        .map(ArrearsConfirmation::getApplicationId)
                        .toList()
        );
        Map<Long, String> users = access().findUserNamesByIds(
                source.getRecords().stream()
                        .map(ArrearsConfirmation::getConfirmUserId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );

        Page<ArrearsVoucherVO> result = new Page<>(
                source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream()
                .map(item -> assemble(item, printTime, snapshots, users))
                .toList());
        return result;
    }

    private ArrearsVoucherVO assemble(
            ArrearsConfirmation confirmation,
            LocalDateTime printTime
    ) {
        Map<Long, ArrearsVoucherApplicantSnapshot> snapshots = applicants(
                List.of(confirmation.getApplicationId()));
        Map<Long, String> users = access().findUserNamesByIds(
                List.of(confirmation.getConfirmUserId()));
        return assemble(confirmation, printTime, snapshots, users);
    }

    private ArrearsVoucherVO assemble(
            ArrearsConfirmation confirmation,
            LocalDateTime printTime,
            Map<Long, ArrearsVoucherApplicantSnapshot> snapshots,
            Map<Long, String> users
    ) {
        ArrearsVoucherApplicantSnapshot snapshot = snapshots.get(
                confirmation.getApplicationId());
        ArrearsVoucherVO result = new ArrearsVoucherVO();
        result.setVoucherNo(confirmation.getVoucherNo());
        result.setApplicationId(confirmation.getApplicationId());
        result.setStudentNo(snapshot.getStudentNo());
        result.setStudentName(snapshot.getStudentName());
        result.setCollegeName(snapshot.getCollegeName());
        result.setMajorName(snapshot.getMajorName());
        result.setGradeName(snapshot.getGradeName());
        result.setClassName(snapshot.getClassName());
        result.setArrearsItems(snapshot.getArrearsItems());
        result.setAppliedAmount(confirmation.getAppliedAmount());
        result.setConfirmedAmount(confirmation.getConfirmedAmount());
        result.setConfirmedTime(confirmation.getConfirmedAt());
        result.setConfirmUserId(confirmation.getConfirmUserId());
        result.setConfirmUserName(users.get(confirmation.getConfirmUserId()));
        result.setPrintTitle("高校绿色通道欠费确认单");
        result.setPrintTime(printTime);
        return result;
    }

    private Map<Long, ArrearsVoucherApplicantSnapshot> applicants(
            Collection<Long> applicationIds
    ) {
        return applicantPort.getObject()
                .findVoucherApplicantsByApplicationIds(applicationIds);
    }

    private ArrearsVoucherAccessPort access() {
        return accessPort.getObject();
    }
}
