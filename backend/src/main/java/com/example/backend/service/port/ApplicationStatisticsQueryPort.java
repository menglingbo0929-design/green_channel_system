package com.example.backend.service.port;

import com.example.backend.model.dto.statistics.StatisticsFilterDTO;
import com.example.backend.model.vo.statistics.ApplicationStatisticsVO;

/**
 * 成员二向成员四提供的最终状态统计适配边界。
 *
 * <p>正式业务能力对应文档中的 ApplicationStatisticsQueryService。实现方必须使用一次
 * 面向集合的只读聚合查询处理申请、组织、欠费、礼包、批次以及确认金额；禁止按申请逐条回调。
 * 本接口不授权成员四创建 application 或明细表 Mapper。</p>
 *
 * <p>TODO(成员二)：实现一次集合聚合查询，并补齐已批准的欠费原因字段后提供 Spring Bean；
 * TODO(成员四)：成员二实现合入后使用真实批次数据完成统计口径联调。</p>
 */
public interface ApplicationStatisticsQueryPort {
    /**
     * 按已校验的筛选条件查询 APPROVED/COMPLETED 的完整统计大盘。
     *
     * @param filter 学校端筛选参数，不含前端伪造的用户权限范围
     * @return 所有数值和列表均有确定默认值的真实聚合结果
     */
    ApplicationStatisticsVO queryFinalStatistics(StatisticsFilterDTO filter);
}
