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
 * <p>当前正式实现为成员四的 ApplicationStatisticsQueryPortAdapter：使用真实申请、学生组织、
 * 欠费、确认和礼包数据完成集合聚合，并按已批准的 arrears_reason_code 输出人数与金额口径。
 * 后续联调只需要前序流程产生最终状态数据，不再新建第二套统计接口。</p>
 */
public interface ApplicationStatisticsQueryPort {
    /**
     * 按已校验的筛选条件查询 APPROVED/CONFIRM_PENDING/COMPLETED 的完整统计大盘。
     *
     * @param filter 学校端筛选参数，不含前端伪造的用户权限范围
     * @return 所有数值和列表均有确定默认值的真实聚合结果
     */
    ApplicationStatisticsVO queryFinalStatistics(StatisticsFilterDTO filter);
}
