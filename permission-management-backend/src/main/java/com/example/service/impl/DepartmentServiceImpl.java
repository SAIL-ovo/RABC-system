package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.Department;
import com.example.mapper.DepartmentMapper;
import com.example.service.DepartmentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    /**
     * 把扁平部门列表组装成树结构。
     * 前端部门管理页和用户部门选择器都会共用这份树结果。
     */
    @Override
    public List<Department> buildDepartmentTree(List<Department> departments) {
        List<Department> sortedDepartments = departments.stream()
                .sorted(Comparator.comparing(Department::getSort, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());

        sortedDepartments.forEach(item -> item.setChildren(new ArrayList<>()));
        List<Department> tree = new ArrayList<>();

        for (Department department : sortedDepartments) {
            if (department.getParentId() == null || department.getParentId() == 0) {
                tree.add(department);
                continue;
            }

            Department parent = sortedDepartments.stream()
                    .filter(item -> item.getId().equals(department.getParentId()))
                    .findFirst()
                    .orElse(null);
            if (parent != null) {
                parent.getChildren().add(department);
            }
        }

        return tree;
    }

    /**
     * 查询当前启用中的部门列表。
     * 用户绑定部门时只允许选择未删除、启用中的部门节点。
     */
    @Override
    public List<Department> listActiveDepartments() {
        return lambdaQuery()
                .eq(Department::getDeleted, 0)
                .eq(Department::getStatus, 1)
                .orderByAsc(Department::getSort)
                .orderByAsc(Department::getId)
                .list();
    }

    /**
     * 判断目标父节点是否落在当前部门的子树中。
     * 这个检查用于阻止把部门树改成循环引用结构。
     */
    @Override
    public boolean hasChildDepartment(Long currentId, Long targetParentId) {
        Long cursor = targetParentId;
        while (cursor != null && cursor != 0) {
            if (cursor.equals(currentId)) {
                return true;
            }
            Department department = getById(cursor);
            if (department == null) {
                return false;
            }
            cursor = department.getParentId();
        }
        return false;
    }

    /**
     * 查询指定部门自身及所有下级部门 ID。
     * 数据权限第一版会用这份结果来做“本部门及下级部门”的用户可见范围过滤。
     * 这里直接基于当前部门表做内存遍历，逻辑简单、对当前项目规模也足够稳定。
     */
    @Override
    public List<Long> listSelfAndChildDepartmentIds(Long rootDepartmentId) {
        if (rootDepartmentId == null || rootDepartmentId == 0) {
            return List.of();
        }

        List<Department> departments = lambdaQuery()
                .eq(Department::getDeleted, 0)
                .list();
        if (departments.isEmpty()) {
            return List.of();
        }

        Set<Long> collectedDepartmentIds = new LinkedHashSet<>();
        collectedDepartmentIds.add(rootDepartmentId);

        boolean changed = true;
        while (changed) {
            changed = false;
            for (Department department : departments) {
                if (department.getId() == null || department.getParentId() == null) {
                    continue;
                }
                if (collectedDepartmentIds.contains(department.getParentId()) && collectedDepartmentIds.add(department.getId())) {
                    changed = true;
                }
            }
        }

        return new ArrayList<>(collectedDepartmentIds);
    }
}
