package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.OperationLog;

/**
 * 操作日志 Mapper。
 * 当前先复用 MyBatis-Plus 的基础能力完成新增和后续查询。
 */
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
