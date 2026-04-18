package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.Department;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * 按部门批量统计用户数量。
     * 部门列表页会据此展示每个部门当前覆盖的用户规模。
     */
    @Select("<script>" +
            "SELECT department_id, COUNT(*) AS user_count " +
            "FROM sys_user " +
            "WHERE department_id IN " +
            "<foreach collection='departmentIds' item='departmentId' open='(' separator=',' close=')'>" +
            "#{departmentId}" +
            "</foreach> " +
            "AND deleted = 0 " +
            "GROUP BY department_id" +
            "</script>")
    List<Map<String, Object>> countUsersByDepartmentIds(@Param("departmentIds") List<Long> departmentIds);
}
