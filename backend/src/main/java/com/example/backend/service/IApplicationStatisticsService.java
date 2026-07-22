package com.example.backend.service;

import com.example.backend.model.dto.statistics.StatisticsFilterDTO;
import com.example.backend.model.vo.statistics.ApplicationStatisticsVO;

/** 6.1.5、6.1.6 的学校端统计服务入口。 */
public interface IApplicationStatisticsService {
    /** 校验筛选条件、校验学校权限并取得成员二的真实集合聚合结果。 */
    ApplicationStatisticsVO querySummary(StatisticsFilterDTO filter, Long currentUserId);
}
