package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO 新增员工信息
     * @return
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 员工信息分页查询
     * @param pageQueryDTO
     * @return
     */
    PageResult pageQuery(EmployeePageQueryDTO pageQueryDTO);

    /**
     * 修改用户状态
     * @param status
     * @param id
     * @return
     */
    void alterStatus(Integer status, Long id);

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    Employee findById(Long id);

    /**
     * 修改员工信息
     * @param employeeD
     * @return
     */
    void updateInfo(EmployeeDTO employeeD);

    /**
     * 修改密码
     * @param passwordEditDTO
     * @return
     */
    void editPassword(PasswordEditDTO passwordEditDTO);
}
