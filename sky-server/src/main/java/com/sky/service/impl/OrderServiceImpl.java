package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WebSocketServer webSocketServer;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Value("sky.baidu.ak")
    private String ak;
    @Value("sky.shop.address")
    private String shopAddress;
    private String baiduUrl = "https://api.map.baidu.com/geocoding/v3/";
    private String baiduDistanceUrl ="https://api.map.baidu.com/directionlite/v1/";

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO orderSubmit(OrdersSubmitDTO ordersSubmitDTO) {
        //检查地址是否为空
        Long addressBookId = ordersSubmitDTO.getAddressBookId();
        AddressBook addressBook = addressBookMapper.getById(addressBookId);
        if (addressBook == null){
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //检查购物车是否为空
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if(shoppingCartList == null || shoppingCartList.isEmpty()){
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        
        //判断是否超出5公里--由于未购买服务，ak不能使用
//        String origin = getCoordinate(shopAddress);
//        String dest = getCoordinate(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
//        Integer distance = getDistance(origin, dest);
//        if (distance > 5000){
//            throw new OrderBusinessException("超出配送范围");
//        }

        //保存订单
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setUserId(userId);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setPayStatus(Orders.UN_PAID);
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setAddress(addressBook.getDetail());
        orderMapper.insert(orders);

        //保存订单明细
        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetails.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetails);

        //清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        //返回数据
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .orderAmount(orders.getAmount())
                .orderNumber(orders.getNumber())
                .orderTime(orders.getOrderTime())
                .id(orders.getId())
                .build();

        return orderSubmitVO;
    }

    /**
     * 获取经纬度
     * @param dest
     * @return lat,len的字符串
     */
    public String getCoordinate(String dest){
        Map params = new LinkedHashMap<String, String>();
        params.put("address", dest);
        params.put("output", "json");
        params.put("ak", ak);
        String address = HttpClientUtil.doGet(baiduUrl, params);
        JSONObject jsonObject = JSONObject.parseObject(address);
        if (jsonObject.getInteger("status") != 0){ //解析失败--无法配送
            throw new OrderBusinessException(dest + "坐标解析失败");
        }
        //获取经纬度
        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lnt = location.getString("lnt");
        String lat = location.getString("lat");
        String coordinate = lat+","+lnt;
        return coordinate;
    }

    /**
     * 获取两地距离
     * @param origin
     * @param destination
     * @return
     */
    public Integer getDistance(String origin, String destination){
        Map<String, String> params = new LinkedHashMap<>();
        params.put("ak", ak);
        params.put("origin", origin);
        params.put("destination", destination);
        String result = HttpClientUtil.doGet(baiduDistanceUrl, params);
        JSONObject jsonObject = JSONObject.parseObject(result);
        if(jsonObject.getInteger("status")!=0){
            throw new OrderBusinessException("配送路线规划失败");
        }

        return jsonObject.getJSONObject("result").getJSONObject("routes").getInteger("distance");
    }

    /**
     * 历史订单分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult page(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Orders orders = new Orders();
        if (ordersPageQueryDTO.getStatus() != null){
            orders.setStatus(ordersPageQueryDTO.getStatus());
        }
        orders.setUserId(BaseContext.getCurrentId());

        Page<Orders> page = orderMapper.pageQuery(orders);

        //查询订单细
        List<OrderVO> orderVOS = new ArrayList<>();
        if (page != null && page.getTotal() > 0) {
            for (Orders od : page) {
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(od.getId());
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(od, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                orderVOS.add(orderVO);
            }

        }
        return new PageResult(page.getTotal(), orderVOS);
    }

    /**
     * 查询订单详情
     * @param id
     * @return
     */
    @Override
    public OrderVO getOrderDetail(Long id) {

        Orders orders = orderMapper.getById(id);
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        /**
         * 由于不是企业账户不能调用微信支付功能--注释代码
         */
        //调用微信支付接口，生成预支付交易单
        /*JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );*/
        JSONObject jsonObject = new JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    /**
     * 直接成功,修改订单状态
     * @param ordersPaymentDTO
     */
    public void paySuccess(OrdersPaymentDTO ordersPaymentDTO){
        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);

        //支付成功 群发消息
        Map<Object, Object> map = new HashMap<>();
        map.put("type", 1);//1是来单提醒 2是用户催单
        map.put("orderId", ordersDB.getId());
        map.put("content","订单号{}"+ordersDB.getNumber());

        //转为json数据再转发
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
    }

    /**
     * 用户取消订单
     * @param id
     * @return
     */
    @Override
    public void userCancelOrder(Long id) {
        //获取订单信息
        Orders orders = orderMapper.getById(id);

        //如果已接单则无法用户取消订单
        if(orders.getStatus() > Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //用户支付则返回金额--由于不能调用微信支付就只修改状态
        if(orders.getPayStatus() == Orders.PAID){
            orders.setPayMethod(Orders.REFUND);
        }

        //设置订单状态
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(MessageConstant.USER_CANCEL);
        orderMapper.update(orders);
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    public void adminCancelOrder(OrdersCancelDTO ordersCancelDTO) {
        //获取订单信息
        Orders orders = orderMapper.getById(ordersCancelDTO.getId());

        //用户已支付则退款
        if(orders.getPayStatus() == Orders.PAID){
            orders.setPayStatus(Orders.REFUND);
        }

        //设置订单状态
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }


    /**
     * 再来一单
     * @param id
     * @return
     */
    @Override
    @Transactional
//    public void repetition(Long id) {   //直接下单版本
//        Orders orders = orderMapper.getById(id);
//        AddressBook addressBook = addressBookMapper.getById(orders.getAddressBookId());
//        //保存订单
//        Orders newOrder = Orders.builder()
//                .addressBookId(orders.getAddressBookId())
//                .remark(orders.getRemark())
//                .payMethod(orders.getPayMethod())
//                .deliveryStatus(orders.getDeliveryStatus())
//                .tablewareNumber(orders.getTablewareNumber())
//                .tablewareStatus(orders.getTablewareStatus())
//                .packAmount(orders.getPackAmount())
//                .amount(orders.getAmount())
//                .orderTime(LocalDateTime.now())
//                .userId(BaseContext.getCurrentId())
//                .status(Orders.PENDING_PAYMENT)
//                .payStatus(Orders.UN_PAID)
//                .phone(orders.getPhone())
//                .consignee(orders.getConsignee())
//                .number(String.valueOf(System.currentTimeMillis()))
//                .address(addressBook.getDetail())
//                .build();
//        orderMapper.insert(newOrder);
//        Long newOrderId = newOrder.getId();
//        //保存订单明细
//        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
//        for (OrderDetail od : orderDetailList) {
//            od.setOrderId(newOrderId);
//            od.setId(null);
//        }
//        orderDetailMapper.insertBatch(orderDetailList);
//    }
    public void repetition(Long id) {   //将数据复制到购物车版本
        Long userId = BaseContext.getCurrentId();
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        List<ShoppingCart> shoppingCarts = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (OrderDetail od : orderDetailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(od, shoppingCart);
            shoppingCart.setId(null);
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(now);
            shoppingCarts.add(shoppingCart);
        }
        shoppingCartMapper.insertBatch(shoppingCarts);
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult list(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Orders orders = new Orders();
        if (ordersPageQueryDTO.getStatus() != null){
            orders.setStatus(ordersPageQueryDTO.getStatus());
        }

        Page<Orders> page = orderMapper.pageQuery(orders);

        //查询订单细节
        List<OrderVO> orderVOS = new ArrayList<>();
        if (page != null && page.getTotal() > 0) {
            for (Orders od : page.getResult()) {
                List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(od.getId());
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(od, orderVO);
                orderVO.setOrderDetailList(orderDetailList);
                //封装菜品信息
                String orderDishes = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishes);
                orderVOS.add(orderVO);
            }

        }
        return new PageResult(page.getTotal(), orderVOS);
    }

    /**
     * 根据订单id获取菜品信息字符串
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders) {
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }

    /**
     * 各个状态的订单数量统计
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        Integer tbc = orderMapper.getCountByStatus(Orders.TO_BE_CONFIRMED);
        Integer c = orderMapper.getCountByStatus(Orders.CONFIRMED);
        Integer dir = orderMapper.getCountByStatus(Orders.DELIVERY_IN_PROGRESS);
        return new OrderStatisticsVO(tbc, c, dir);
    }

    /**
     * 接单
     * @param ordersConfirmDTO
     * @return
     */
    @Override
    public void confirm( OrdersConfirmDTO ordersConfirmDTO) {
//        Orders orders = orderMapper.getById(ordersConfirmDTO.getId());
//        orders.setStatus(Orders.CONFIRMED);
        //代码优化--减少查询数据库次数
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     * @param ordersRejectionDTO
     * @return
     */
    @Override
    public void reject(OrdersRejectionDTO ordersRejectionDTO) {
        //处于待接单才能拒单
        Orders orders = orderMapper.getById(ordersRejectionDTO.getId());
        if(!Objects.equals(orders.getStatus(), Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        //用户已支付则自动退款
        if (orders.getPayStatus() == Orders.PAID){
            orders.setStatus(Orders.REFUND);
        }
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     * @param id
     */
    @Override
    public void delevery(Long id) {
        Orders orders = orderMapper.getById(id);
        //只有处于已接单状态才能派送
        if (orders.getStatus() != Orders.CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        //预计送达时间 递送状态 未设置
        orderMapper.update(orders);
    }

    /**
     * 完成订单
     * @param id
     * @return
     */
    @Override
    public void complete(Long id) {
        Orders orders = orderMapper.getById(id);

        //只有状态为DELIVERY_IN_PROGRESS才能完成订单
        if(orders.getStatus() != Orders.DELIVERY_IN_PROGRESS){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        orders.setDeliveryTime(LocalDateTime.now());
        orders.setStatus(Orders.COMPLETED);
        orderMapper.update(orders);
    }

    /**
     * 催单
     * @param id
     * @return
     */
    @Override
    public void reminder(Long id) {
        Orders orderDB = orderMapper.getById(id);
        if(orderDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        Map<Object, Object> map = new HashMap<>();
        map.put("type", 2);//1是来单提醒 2是用户催单
        map.put("orderId", id);
        map.put("content","订单号{}"+ orderDB.getNumber());

        webSocketServer.sendToAllClient(JSON.toJSONString(map));
    }
}
