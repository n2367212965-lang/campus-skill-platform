package com.example.campusskillplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campusskillplatform.entity.Rating;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 评价Mapper
 * 新增：支持评价数据访问
 */
@Mapper
public interface RatingMapper extends BaseMapper<Rating> {

    @Select("SELECT * FROM rating WHERE to_user_id = #{userId} ORDER BY create_time DESC")
    List<Rating> selectByToUserId(Long userId);

    @Select("SELECT * FROM rating WHERE order_id = #{orderId}")
    List<Rating> selectByOrderId(Long orderId);

    @Select("SELECT AVG(score) FROM rating WHERE to_user_id = #{userId}")
    Double calculateAverageScore(Long userId);

    @Select("SELECT COUNT(*) FROM rating WHERE to_user_id = #{userId}")
    Integer countByToUserId(Long userId);
}