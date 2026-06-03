package com.example.campusskillplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campusskillplatform.entity.Dispute;
import org.apache.ibatis.annotations.Mapper;

/**
 * 纠纷Mapper
 * 新增：支持纠纷数据访问
 */
@Mapper
public interface DisputeMapper extends BaseMapper<Dispute> {
}