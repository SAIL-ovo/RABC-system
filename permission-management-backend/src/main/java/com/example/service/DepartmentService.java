package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.Department;

import java.util.List;

public interface DepartmentService extends IService<Department> {
    List<Department> buildDepartmentTree(List<Department> departments);
    List<Department> listActiveDepartments();
    boolean hasChildDepartment(Long currentId, Long targetParentId);
    List<Long> listSelfAndChildDepartmentIds(Long rootDepartmentId);
}
