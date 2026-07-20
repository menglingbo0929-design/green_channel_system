package com.example.backend.service.port;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.model.dto.statistics.StatisticsReportQueryDTO;
import com.example.backend.model.vo.statistics.StatisticsReportRowVO;

/**
 * 成员二向成员四提供的 6.1.7 统计明细查询边界。
 *
 * <p>TODO(成员二)：用一次面向集合的分页 SQL 查询申请、学生组织、批次、欠费、礼包、
 * 补助及确认金额，严格应用成员一的数据范围；排序 key 必须映射到固定 SQL 列，禁止把
 * 前端 sortBy 原样拼接进 SQL。成员四不得建立 Mapper 直接读取成员二或成员一的表。</p>
 */
public interface StatisticsReportQueryPort {

    /**
     * 按统一筛选、分页和排序条件返回真实报表行。
     *
     * @param query 已经由成员四校验过的查询条件
     * @param currentUserId 当前学校用户，仅供实现方结合可信上下文取得数据范围
     */
    Page<StatisticsReportRowVO> queryReportPage(
            StatisticsReportQueryDTO query,
            Long currentUserId
    );
}
