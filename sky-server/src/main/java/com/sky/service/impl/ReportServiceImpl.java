package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    OrderMapper orderMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateListlist = new ArrayList<>();
        List<Double> moneyList = new ArrayList<>();
        while (!begin.equals(end)){
            //计算日期并加入集合
            localDateListlist.add(begin);

            //计算对应天的营业额
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(begin, LocalTime.MAX);
            Double moneyOfDay = orderMapper.sumByTimeAndStatus(beginTime, endTime, Orders.COMPLETED);
            moneyOfDay = moneyOfDay == null ? 0.0 : moneyOfDay;
            moneyList.add(moneyOfDay);

            begin = begin.plusDays(1);
        }
        localDateListlist.add(end);
        LocalDateTime beginTime = LocalDateTime.of(end, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MIN);
        Double moneyOfDay = orderMapper.sumByTimeAndStatus(beginTime, endTime, Orders.COMPLETED);
        moneyOfDay = moneyOfDay == null ? 0.0 : moneyOfDay;
        moneyList.add(moneyOfDay);

        String turnoversList = StringUtils.join(moneyList, ",");
        String dateList = StringUtils.join(localDateListlist, ",");

        return new TurnoverReportVO(dateList, turnoversList);
    }
}
