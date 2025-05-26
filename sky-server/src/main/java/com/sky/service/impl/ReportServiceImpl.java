package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisServer;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList = new ArrayList<>();
        List<Double> moneyList = new ArrayList<>();
        while (!begin.equals(end)){
            //计算日期并加入集合
            localDateList.add(begin);

            //计算对应天的营业额
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(begin, LocalTime.MAX);
            Double moneyOfDay = orderMapper.sumByTimeAndStatus(beginTime, endTime, Orders.COMPLETED);
            moneyOfDay = moneyOfDay == null ? 0.0 : moneyOfDay;
            moneyList.add(moneyOfDay);

            begin = begin.plusDays(1);
        }
        localDateList.add(end);
        LocalDateTime beginTime = LocalDateTime.of(end, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MIN);
        Double moneyOfDay = orderMapper.sumByTimeAndStatus(beginTime, endTime, Orders.COMPLETED);
        moneyOfDay = moneyOfDay == null ? 0.0 : moneyOfDay;
        moneyList.add(moneyOfDay);

        String turnoversList = StringUtils.join(moneyList, ",");
        String dateList = StringUtils.join(localDateList, ",");

        return new TurnoverReportVO(dateList, turnoversList);
    }

    /**
     * 用户统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> userList = new ArrayList<>();
        while (!begin.equals(end)){
            //计算日期并加入集合
            localDateList.add(begin);

            //获取当天最开始和结束时间
            LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(begin, LocalTime.MAX);

            Map<Object, Object> map = new HashMap<>();
            //计算截止当天总用户数
            map.put("endTime", endTime);
            Integer totalUsers = userMapper.countByMap(map);
            userList.add(totalUsers == null ? 0 : totalUsers);

            //计算当天新增用户数
            map.put("beginTime", beginTime);
            Integer newUsers = userMapper.countByMap(map);
            newUserList.add(newUsers == null ? 0 : newUsers);

            //更新begin值
            begin = begin.plusDays(1);
        }
        localDateList.add(end);

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(localDateList))
                .newUserList(StringUtils.join(newUserList))
                .totalUserList(StringUtils.join(userList))
                .build();
    }
}
