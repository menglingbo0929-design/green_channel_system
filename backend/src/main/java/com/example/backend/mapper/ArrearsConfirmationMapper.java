package com.example.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.backend.model.domain.ArrearsConfirmation;

/**
 * 欠费确认模块的数据访问接口。
 *
 * <p>本 Mapper 只操作成员四拥有的 {@code arrears_confirmation} 单表。
 * 申请、学生、欠费明细均由成员一/二维护；成员四不能在此处编写跨模块查询或更新 SQL。</p>
 */
public interface ArrearsConfirmationMapper extends BaseMapper<ArrearsConfirmation> {
}
