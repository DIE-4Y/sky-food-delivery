package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

            Map<Object, Object> map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);

            Double moneyOfDay = orderMapper.sumByMap(map);
//            Double moneyOfDay = orderMapper.sumByTimeAndStatus(beginTime, endTime, Orders.COMPLETED);
            moneyOfDay = moneyOfDay == null ? 0.0 : moneyOfDay;
            moneyList.add(moneyOfDay);

            begin = begin.plusDays(1);
        }
        localDateList.add(end);
        LocalDateTime beginTime = LocalDateTime.of(end, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MIN);

        Map<Object, Object> map = new HashMap<>();
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        map.put("status", Orders.COMPLETED);

        Double moneyOfDay = orderMapper.sumByMap(map);

//        Double moneyOfDay = orderMapper.sumByTimeAndStatus(beginTime, endTime, Orders.COMPLETED);
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

    /**
     * 订单统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> localDateList = new ArrayList<>();
        List<Integer> totalCountList = new ArrayList<>();
        List<Integer> validCountList = new ArrayList<>();
        while (!begin.equals(end)){
            localDateList.add(begin);
            begin = begin.plusDays(1);
        }
        localDateList.add(end);

        //获得每天数据
        for (LocalDate localDate : localDateList) {
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX);

            //计算当天总订单数
            Map<Object, Object> map = new HashMap<>();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            Integer totalCount = orderMapper.countByMap(map);
            totalCountList.add(totalCount == null ? 0 : totalCount);

            //计算有效订单数
            map.put("status", Orders.COMPLETED);
            Integer validCount = orderMapper.countByMap(map);
            validCountList.add(validCount == null ? 0 : validCount);
        }

        Map<Object, Object> map = new HashMap<>();
        begin = localDateList.get(0);
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        //获得总订单数
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        Integer totalOrderCount = orderMapper.countByMap(map);

        //获得总有效订单数
        map.put("status", Orders.COMPLETED);
        Integer validTotalCount = orderMapper.countByMap(map);

        Double completeRate = 0.0;
        if(totalOrderCount != 0){
            completeRate = validTotalCount*1.0/totalOrderCount;
        }

        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(localDateList, ","))
                .orderCountList(StringUtils.join(totalCountList, ","))
                .validOrderCountList(StringUtils.join(validCountList, ","))
                .totalOrderCount(totalOrderCount == null ? 0 : totalOrderCount)
                .validOrderCount(validTotalCount == null? 0 : validTotalCount)
                .orderCompletionRate(completeRate)
                .build();
    }

    /**
     * 统计销量排名前10菜品
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getTop10(beginTime, endTime);

        List<String> nameList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO
                .builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }
}
