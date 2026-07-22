package com.example.backend.service.impl;

import com.example.backend.model.dto.statistics.StatisticsFilterDTO;
import com.example.backend.model.vo.statistics.ApplicationStatisticsVO;
import com.example.backend.service.IApplicationStatisticsService;
import com.example.backend.service.port.ApplicationStatisticsQueryPort;
import com.example.backend.service.port.StatisticsAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 6.1.5、6.1.6 学校统计编排。
 *
 * <p>演示版本保持视频中的直接调用方式：先调用权限接口，再调用成员二的统计接口，
 * 不在本类增加额外异常包装或复杂边界判断。</p>
 */
@Service
@RequiredArgsConstructor
public class ApplicationStatisticsServiceImpl implements IApplicationStatisticsService {
    private final ApplicationStatisticsQueryPort statisticsQueryPort;
    private final StatisticsAccessPort statisticsAccessPort;

    @Override
    public ApplicationStatisticsVO querySummary(
            StatisticsFilterDTO filter,
            Long currentUserId
    ) {
        StatisticsFilterDTO actualFilter = filter == null
                ? new StatisticsFilterDTO()
                : filter;
        statisticsAccessPort.checkSchoolStatisticsUser(currentUserId);
        return statisticsQueryPort.queryFinalStatistics(actualFilter);
    }
}
