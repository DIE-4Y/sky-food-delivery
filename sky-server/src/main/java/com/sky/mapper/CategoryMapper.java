package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CategoryMapper {

    /**
     * 插入数据
     *
     * @param category
     */
    @Insert("insert into category(type, name, sort, status, create_time, update_time, create_user, update_user)" +
            " VALUES" +
            " (#{type}, #{name}, #{sort}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    @AutoFill(value = OperationType.INSERT)
    void insert(Category category);

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据id删除分类
     * @param id
     */
    @Delete("delete from category where id = #{id}")
    void deleteById(Long id);

    /**
     * 根据id修改分类
     * @param category
     */
    @AutoFill(OperationType.UPDATE)
    void update(Category category);

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    List<Category> list(Integer type);

    /**
     * 根据id获取类别信息
     * @param id
     * @return
     */
    @Select("select * from category where id=#{id};")
    Category getByDishId(Long id);
}


//package com.sky.mapper;
//
//import com.github.pagehelper.Page;
//import com.sky.dto.CategoryDTO;
//import com.sky.dto.CategoryPageQueryDTO;
//import com.sky.entity.Category;
//import org.apache.ibatis.annotations.Delete;
//import org.apache.ibatis.annotations.Insert;
//import org.apache.ibatis.annotations.Mapper;
//import org.apache.ibatis.annotations.Select;
//
//import java.util.List;
//
//@Mapper
//public interface CategoryMapper {
//
//    /**
//     * 更新分类信息
//     * @param category
//     */
//    void update(Category category);
//
//    /**
//     * 新增分类
//     * @param category
//     */
//    @Insert("insert into category(type, name, sort, status, create_time, update_time, create_user, update_user) " +
//            "VALUES(#{type}, #{name}, #{sort}, #{status}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser}) ")
//    void insert(Category category);
//
//    /**
//     * 分页查询
//     * @param categoryPageQueryDTO
//     * @return
//     */
//    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);
//
//    List<Category> list(String type);
//
//    /**
//     * 根据分类id删除记录
//     * @param id
//     */
//    @Delete("delete from category where id=#{id}")
//    void deleteById(Long id);
//}
