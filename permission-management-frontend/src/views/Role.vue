<template>
  <div class="manage-page">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">ROLE CENTER</p>
        <h2>角色管理</h2>
        <p class="hero-text">
          统一维护角色定义、启用状态与权限归属，让角色职责边界更清晰。
        </p>
      </div>
      <div class="hero-actions">
        <div class="stat-card">
          <span class="stat-label">角色总数</span>
          <strong>{{ total }}</strong>
        </div>
        <el-button v-if="canCreateRole" type="primary" size="large" class="create-btn" @click="handleAddRole">
          <el-icon><Plus /></el-icon>
          <span>新增角色</span>
        </el-button>
      </div>
    </section>

    <el-card class="content-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h3>角色列表</h3>
            <p>支持角色搜索、状态切换与权限分配。</p>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input v-model="searchForm.name" placeholder="请输入角色名称" style="width: 220px" />
        <el-input v-model="searchForm.code" placeholder="请输入角色编码" style="width: 220px" />
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>
          <span>重置</span>
        </el-button>
      </div>

      <el-table :data="roleList" border class="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="角色名称" min-width="150" />
        <el-table-column prop="code" label="角色编码" min-width="150" />
        <el-table-column prop="dataScopeLabel" label="数据范围" min-width="150" />
        <el-table-column prop="permissionCount" label="已分配权限数" width="130" align="center" />
        <el-table-column prop="permissionSummary" label="权限摘要" min-width="220" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="220" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="status" label="状态" width="170">
          <template #default="scope">
            <div class="status-cell">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" effect="light" round>
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
              <el-switch
                v-if="canToggleRoleStatus"
                v-model="scope.row.status"
                :active-value="1"
                :inactive-value="0"
                @change="(val) => handleStatusChange(scope.row, val)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="showRoleActions" label="操作" width="240" fixed="right">
          <template #default="scope">
            <div class="action-group">
              <el-button v-if="canEditRoleAction" type="primary" plain size="small" @click="handleEditRole(scope.row)">
                <el-icon><Edit /></el-icon>
                <span>编辑</span>
              </el-button>
              <el-button v-if="canDeleteRoleAction" type="danger" plain size="small" @click="handleDeleteRole(scope.row.id)">
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
              <el-button v-if="canAssignRolePermission" type="info" plain size="small" @click="handleAssignPermission(scope.row)">
                <el-icon><Lock /></el-icon>
                <span>分配权限</span>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="loadRoleList"
          @current-change="loadRoleList"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" class="manage-dialog">
      <el-form ref="roleFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入角色名称" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入描述" />
        </el-form-item>
        <el-form-item label="数据范围" prop="dataScope">
          <el-select v-model="form.dataScope" placeholder="请选择数据范围" style="width: 100%">
            <el-option
              v-for="item in dataScopeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permissionDialogVisible" title="分配权限" width="640px" class="manage-dialog">
      <el-form>
        <el-form-item label="角色" label-width="80px">
          <span>{{ selectedRole?.name || '' }}</span>
        </el-form-item>
        <el-form-item label="权限" label-width="80px">
          <el-tree
            ref="permissionTreeRef"
            :data="permissionTree"
            node-key="id"
            show-checkbox
            default-expand-all
            :default-checked-keys="selectedPermissions"
            :props="treeProps"
            :disabled="isPermissionGroupNode"
            class="permission-tree"
          >
            <template #default="{ data }">
              <div class="permission-node">
                <span>{{ data.name }}</span>
                <el-tag v-if="data.code === 'log:view'" size="small" effect="light" type="warning">
                  日志菜单
                </el-tag>
                <el-tag v-if="isPermissionGroupNode(data)" size="small" effect="plain" type="info">
                  分组
                </el-tag>
              </div>
            </template>
          </el-tree>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignPermissionSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Lock, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import request from '@/utils/request'
import { getFirstAuthorizedRoute } from '@/router'

// 全局状态仓库，用于读取角色模块按钮权限。
const store = useStore()
const router = useRouter()

// 角色页的细粒度按钮权限。
// 现阶段保留 role:manage 作为兜底，避免旧数据下页面直接失能。
const canManageRoles = computed(() => store.getters.hasPermission('role:manage'))
const canCreateRole = computed(() => store.getters.hasPermission('role:create') || canManageRoles.value)
const canEditRoleAction = computed(() => store.getters.hasPermission('role:update') || canManageRoles.value)
const canDeleteRoleAction = computed(() => store.getters.hasPermission('role:delete') || canManageRoles.value)
const canAssignRolePermission = computed(() => store.getters.hasPermission('role:assign-permission') || canManageRoles.value)
const canToggleRoleStatus = computed(() => store.getters.hasPermission('role:status') || canManageRoles.value)

// 只有具备角色动作权限时才显示操作列，纯查看权限用户只看列表即可。
const showRoleActions = computed(() => (
  canEditRoleAction.value
  || canDeleteRoleAction.value
  || canAssignRolePermission.value
))

// 列表筛选条件。
const searchForm = reactive({
  name: '',
  code: ''
})

// 分页状态与角色列表数据。
const currentPage = ref(1)
const pageSize = ref(6)
const total = ref(0)
const roleList = ref([])
const dataScopeOptions = [
  { value: 'ALL', label: '全部数据' },
  { value: 'DEPT_AND_CHILD', label: '本部门及下级部门' },
  { value: 'DEPT', label: '仅本部门' },
  { value: 'SELF', label: '仅本人' }
]

// 权限树和角色分配相关状态。
const permissionTree = ref([])
const selectedRole = ref(null)
const selectedPermissions = ref([])
const permissionTreeRef = ref(null)

// 树组件字段映射。
const treeProps = {
  children: 'children',
  label: 'name',
  disabled: 'disabled'
}

// 弹窗与表单状态。
const dialogVisible = ref(false)
const permissionDialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const roleFormRef = ref(null)

// 角色表单数据。
const form = reactive({
  id: '',
  name: '',
  code: '',
  description: '',
  dataScope: 'ALL',
  status: 1
})

// 角色表单校验规则。
const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  dataScope: [{ required: true, message: '请选择数据范围', trigger: 'change' }]
}

// 加载角色分页列表。
const loadRoleList = async () => {
  const res = await request.get('/role/list', {
    params: {
      page: currentPage.value,
      size: pageSize.value,
      name: searchForm.name,
      code: searchForm.code
    }
  })

  roleList.value = res.data.records || []
  roleList.value = roleList.value.map((item) => ({
    ...item,
    dataScopeLabel: resolveDataScopeLabel(item.dataScope)
  }))
  total.value = res.data.total || 0
}

// 数据权限第一版只开放 4 种标准范围。
// 这里统一做标签转换，避免表格、表单提示和后端枚举分散维护。
const resolveDataScopeLabel = (dataScope) => {
  return dataScopeOptions.find((item) => item.value === dataScope)?.label || '全部数据'
}

// 加载权限树，用于角色分配权限。
const loadPermissionTree = async () => {
  const res = await request.get('/permission/tree')
  permissionTree.value = markPermissionTreeNodes(res.data || [])
}

// 把包含子节点的权限视为分组节点。
// 这类节点只负责组织树结构，不允许在“角色分配权限”里直接授予，避免把 manage 这类父节点误配出去。
const isPermissionGroupNode = (permission) => Array.isArray(permission?.children) && permission.children.length > 0

// 递归补齐树节点的 disabled 状态，让 el-tree 直接禁用分组节点的勾选框。
const markPermissionTreeNodes = (nodes = []) => {
  return nodes.map((node) => {
    const children = markPermissionTreeNodes(node.children || [])
    return {
      ...node,
      children,
      disabled: children.length > 0
    }
  })
}

// 搜索角色时回到第一页重新查询。
const handleSearch = () => {
  currentPage.value = 1
  loadRoleList()
}

// 清空筛选条件并刷新列表。
const handleReset = () => {
  searchForm.name = ''
  searchForm.code = ''
  currentPage.value = 1
  loadRoleList()
}

// 打开新增角色弹窗并重置表单。
const handleAddRole = () => {
  dialogTitle.value = '新增角色'
  Object.assign(form, {
    id: '',
    name: '',
    code: '',
    description: '',
    dataScope: 'ALL',
    status: 1
  })
  dialogVisible.value = true
}

// 把当前角色信息带入编辑弹窗。
const handleEditRole = (row) => {
  dialogTitle.value = '编辑角色'
  Object.assign(form, {
    id: row.id,
    name: row.name,
    code: row.code,
    description: row.description,
    dataScope: row.dataScope || 'ALL',
    status: row.status
  })
  dialogVisible.value = true
}

// 删除前先让用户确认，避免误删角色。
const handleDeleteRole = (id) => {
  ElMessageBox.confirm('确定删除该角色吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/role/${id}`)
    ElMessage.success('删除成功')
    loadRoleList()
  })
}

// 打开权限分配弹窗时，同时加载权限树和角色已选权限。
const handleAssignPermission = async (role) => {
  selectedRole.value = role
  await loadPermissionTree()
  const res = await request.get(`/role/permissions/${role.id}`)
  selectedPermissions.value = res.data || []
  permissionDialogVisible.value = true

  await nextTick()
  permissionTreeRef.value?.setCheckedKeys(selectedPermissions.value)
}

// 提交角色的权限分配结果。
// 这里只提交用户真正勾选的节点，不再把半选父节点一并提交。
// 否则像 user:view 这类子权限会把 user:manage 这种父级菜单权限误带上。
const handleAssignPermissionSubmit = async () => {
  const checkedKeys = permissionTreeRef.value?.getCheckedKeys(false) || []
  const permissionIds = [...new Set(checkedKeys)]

  await request.post('/role/assign-permission', {
    roleId: selectedRole.value.id,
    permissionIds
  })

  // 如果当前登录用户命中了这个角色，需要立刻刷新本地权限和菜单上下文。
  const currentUserContext = await store.dispatch('fetchCurrentUser').catch(() => null)
  ElMessage.success('权限更新成功')
  permissionDialogVisible.value = false

  // 当前页如果因为角色权限变化已经不可访问，就自动回退到仍有权限的首个页面。
  if (currentUserContext?.permissions?.length) {
    const accessiblePaths = new Set((store.getters.menuList || []).map((item) => item.path))
    if (!accessiblePaths.has(router.currentRoute.value.path)) {
      router.replace(getFirstAuthorizedRoute(currentUserContext.permissions))
    }
  }
}

// 提交角色新增或编辑表单。
const handleSubmit = async () => {
  const valid = await roleFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (form.id) {
    await request.put('/role', { ...form, status: Number(form.status) })
    ElMessage.success('编辑成功')
  } else {
    await request.post('/role', { ...form, status: Number(form.status) })
    ElMessage.success('新增成功')
  }

  dialogVisible.value = false
  loadRoleList()
}

// 切换角色状态失败时，需要把开关恢复回原值。
const handleStatusChange = async (row, newStatus) => {
  const oldStatus = row.status === 1 ? 0 : 1
  try {
    await request.put('/role/status', {
      id: row.id,
      status: Number(newStatus)
    })
    ElMessage.success('状态更新成功')
    loadRoleList()
  } catch (error) {
    row.status = oldStatus
  }
}

// 页面加载后先读取角色列表。
onMounted(() => {
  loadRoleList()
})
</script>

<style scoped>
.manage-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.08), transparent 32%),
    linear-gradient(180deg, #f7f9fc 0%, #eef3f8 100%);
  padding: 4px;
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 28px;
  border-radius: 20px;
  background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 100%);
  color: #fff;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.22);
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.hero-panel h2 {
  margin: 0;
  font-size: 30px;
  line-height: 1.1;
  color: #fff;
}

.hero-text {
  max-width: 620px;
  margin: 14px 0 0;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.stat-card {
  min-width: 126px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(8px);
}

.stat-label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
  margin-bottom: 8px;
}

.stat-card strong {
  font-size: 24px;
  line-height: 1;
  color: #fff;
}

.create-btn {
  height: 48px;
  padding: 0 22px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #f97316 0%, #fb7185 100%);
  box-shadow: 0 10px 22px rgba(249, 115, 22, 0.3);
}

.content-card {
  border: none;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.card-header h3 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.card-header p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 13px;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.data-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #334155;
}

.data-table :deep(.el-table__row:hover td) {
  background: #f8fbff !important;
}

.status-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.action-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
}

.permission-tree {
  width: 100%;
  max-height: 420px;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 12px;
  background: #f8fbff;
}

.permission-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 900px) {
  .hero-panel {
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
</style>
