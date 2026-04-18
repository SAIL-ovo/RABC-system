<template>
  <div class="manage-page">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">USER CENTER</p>
        <h2>用户管理</h2>
        <p class="hero-text">
          统一维护系统账号、部门归属、岗位职责、角色边界与状态信息，确保访问主体清晰可控。
        </p>
      </div>
      <div class="hero-actions">
        <div class="stat-card">
          <span class="stat-label">用户总数</span>
          <strong>{{ total }}</strong>
        </div>
        <el-button v-if="canCreateUser" type="primary" size="large" class="create-btn" @click="handleAddUser">
          <el-icon><Plus /></el-icon>
          <span>新增用户</span>
        </el-button>
      </div>
    </section>

    <el-card class="content-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h3>用户列表</h3>
            <p>支持分页搜索、部门归属、岗位归属、状态切换与角色分配。</p>
          </div>
        </div>
      </template>

      <div class="scope-banner">
        <span class="scope-banner__label">当前数据范围</span>
        <strong>{{ currentDataScopeLabel }}</strong>
        <span class="scope-banner__hint">用户列表会按当前登录人生效的数据范围自动过滤。</span>
      </div>

      <div class="search-bar">
        <el-input v-model="searchForm.username" placeholder="请输入用户名" style="width: 220px">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-input v-model="searchForm.realName" placeholder="请输入真实姓名" style="width: 220px">
          <template #prefix>
            <el-icon><User /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>
          <span>重置</span>
        </el-button>
      </div>

      <el-table :data="userList" border class="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="130" />
        <el-table-column prop="realName" label="真实姓名" min-width="130" />
        <el-table-column prop="departmentName" label="所属部门" min-width="150" show-overflow-tooltip />
        <el-table-column prop="postName" label="所属岗位" min-width="150" show-overflow-tooltip />
        <el-table-column prop="roleCount" label="角色数" width="100" align="center" />
        <el-table-column prop="roleSummary" label="角色摘要" min-width="180" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column prop="status" label="状态" width="170">
          <template #default="scope">
            <div class="status-cell">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" effect="light" round>
                {{ scope.row.status === 1 ? '启用' : '停用' }}
              </el-tag>
              <el-switch
                v-if="canToggleUserStatus"
                v-model="scope.row.status"
                :active-value="1"
                :inactive-value="0"
                @change="(val) => handleStatusChange(scope.row, val)"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column v-if="showUserActions" label="操作" width="340" fixed="right">
          <template #default="scope">
            <div class="action-group">
              <el-button v-if="canEditUser" type="primary" plain size="small" @click="handleEditUser(scope.row)">
                <el-icon><Edit /></el-icon>
                <span>编辑</span>
              </el-button>
              <el-button type="success" plain size="small" @click="handleViewProfile(scope.row)">
                <span>查看详情</span>
              </el-button>
              <el-button v-if="canDeleteUser" type="danger" plain size="small" @click="handleDeleteUser(scope.row.id)">
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
              <el-button v-if="canAssignUserRole" type="info" plain size="small" @click="handleAssignRole(scope.row)">
                <el-icon><Operation /></el-icon>
                <span>分配角色</span>
              </el-button>
              <el-button v-if="canResetUserPassword" type="warning" plain size="small" @click="handleResetPassword(scope.row)">
                <span>重置密码</span>
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
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" class="manage-dialog">
      <el-form ref="userFormRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>

        <div class="form-grid">
          <el-form-item label="真实姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="请输入真实姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入手机号" />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="请输入邮箱" />
          </el-form-item>
          <el-form-item label="所属部门" prop="departmentId">
            <el-select v-model="form.departmentId" placeholder="请选择所属部门" clearable style="width: 100%">
              <el-option :value="null" label="暂不分配部门" />
              <el-option
                v-for="item in departmentOptions"
                :key="item.id"
                :label="item.label"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="所属岗位" prop="postId">
            <el-select v-model="form.postId" placeholder="请选择所属岗位" clearable style="width: 100%">
              <el-option :value="null" label="暂不分配岗位" />
              <el-option
                v-for="item in postOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>

        <el-form-item v-if="canAssignUserRole" label="角色配置">
          <div class="role-panel">
            <div class="role-panel__toolbar">
              <span class="role-panel__tip">支持按岗位自动带出默认角色，管理员也可以在此基础上继续微调。</span>
              <el-button
                v-if="form.postId"
                type="primary"
                link
                @click="applyPostDefaultRoles(true)"
              >
                按岗位带出默认角色
              </el-button>
            </div>
            <el-checkbox-group v-model="form.roleIds" class="role-checkbox-group">
              <el-checkbox v-for="role in roleList" :key="role.id" :value="role.id">
                {{ role.name }}
              </el-checkbox>
            </el-checkbox-group>
            <el-empty v-if="!roleList.length" description="暂无可选角色" :image-size="72" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="520px" class="manage-dialog">
      <el-form label-width="90px">
        <el-form-item label="用户">
          <span>{{ selectedUser?.realName || '' }}</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="selectedRoleIds" class="role-checkbox-group">
            <el-checkbox v-for="role in roleList" :key="role.id" :value="role.id">
              {{ role.name }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignRoleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="460px" class="manage-dialog">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
        <el-form-item label="用户" prop="username">
          <el-input v-model="passwordForm.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPasswordSubmit">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="profileDrawerVisible" title="用户组织画像" size="520px">
      <div v-if="profileLoading" class="profile-loading">正在加载用户画像...</div>
      <template v-else-if="profileData">
        <div class="profile-section">
          <h4>基础信息</h4>
          <div class="profile-grid">
            <div class="profile-item">
              <span class="profile-label">用户名</span>
              <strong>{{ profileData.username }}</strong>
            </div>
            <div class="profile-item">
              <span class="profile-label">真实姓名</span>
              <strong>{{ profileData.realName }}</strong>
            </div>
            <div class="profile-item">
              <span class="profile-label">邮箱</span>
              <strong>{{ profileData.email || '未填写' }}</strong>
            </div>
            <div class="profile-item">
              <span class="profile-label">手机号</span>
              <strong>{{ profileData.phone || '未填写' }}</strong>
            </div>
            <div class="profile-item">
              <span class="profile-label">账号状态</span>
              <strong>{{ profileData.status === 1 ? '启用' : '停用' }}</strong>
            </div>
            <div class="profile-item">
              <span class="profile-label">创建时间</span>
              <strong>{{ profileData.createTime || '暂无' }}</strong>
            </div>
          </div>
        </div>

        <div class="profile-section">
          <h4>组织归属</h4>
          <div class="profile-grid">
            <div class="profile-item">
              <span class="profile-label">所属部门</span>
              <strong>{{ profileData.departmentName }}</strong>
            </div>
            <div class="profile-item">
              <span class="profile-label">部门负责人</span>
              <strong>{{ profileData.departmentLeaderName }}</strong>
            </div>
            <div class="profile-item">
              <span class="profile-label">部门电话</span>
              <strong>{{ profileData.departmentPhone || '未设置' }}</strong>
            </div>
            <div class="profile-item">
              <span class="profile-label">所属岗位</span>
              <strong>{{ profileData.postName }}</strong>
            </div>
            <div class="profile-item">
              <span class="profile-label">岗位归属部门</span>
              <strong>{{ profileData.postDepartmentName || '暂无' }}</strong>
            </div>
            <div class="profile-item">
              <span class="profile-label">岗位编码</span>
              <strong>{{ profileData.postCode || '暂无' }}</strong>
            </div>
          </div>
          <div class="profile-block">
            <span class="profile-label">岗位说明</span>
            <p>{{ profileData.postRemark || '暂无岗位说明' }}</p>
          </div>
        </div>

        <div class="profile-section">
          <h4>授权画像</h4>
          <div class="profile-grid">
            <div class="profile-item">
              <span class="profile-label">当前角色数</span>
              <strong>{{ profileData.roleCount }}</strong>
            </div>
            <div class="profile-item">
              <span class="profile-label">生效数据范围</span>
              <strong>{{ profileData.effectiveDataScopeLabel }}</strong>
            </div>
          </div>
          <div class="profile-block">
            <span class="profile-label">已分配角色</span>
            <div class="profile-tags">
              <el-tag v-for="roleName in profileData.roleNames" :key="roleName" effect="light" round>
                {{ roleName }}
              </el-tag>
              <span v-if="!profileData.roleNames?.length" class="empty-hint">未分配角色</span>
            </div>
          </div>
          <div class="profile-block">
            <span class="profile-label">岗位默认角色模板</span>
            <div class="profile-tags">
              <el-tag v-for="roleName in profileData.postDefaultRoleNames" :key="roleName" type="success" effect="light" round>
                {{ roleName }}
              </el-tag>
              <span v-if="!profileData.postDefaultRoleNames?.length" class="empty-hint">当前岗位未配置默认角色</span>
            </div>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Delete, Edit, Operation, Plus, Refresh, Search, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useStore } from 'vuex'
import request from '@/utils/request'

const store = useStore()

// 用户模块的按钮权限。
// 保留 manage 作为历史兼容兜底，避免旧角色在新版本里突然丢失页面操作能力。
const canManageUsers = computed(() => store.getters.hasPermission('user:manage'))
const canCreateUser = computed(() => store.getters.hasPermission('user:create') || canManageUsers.value)
const canEditUser = computed(() => store.getters.hasPermission('user:update') || canManageUsers.value)
const canDeleteUser = computed(() => store.getters.hasPermission('user:delete') || canManageUsers.value)
const canAssignUserRole = computed(() => store.getters.hasPermission('user:assign-role') || canManageUsers.value)
const canResetUserPassword = computed(() => store.getters.hasPermission('user:reset-password') || canManageUsers.value)
const canToggleUserStatus = computed(() => store.getters.hasPermission('user:status') || canManageUsers.value)
const canLoadOrganizationOptions = computed(() => canCreateUser.value || canEditUser.value)
const currentDataScopeLabel = computed(() => store.getters.currentDataScopeLabel || '全部数据')

// 只要具备任意一种写操作权限，就显示操作列。
const showUserActions = computed(() => (
  canEditUser.value
  || canDeleteUser.value
  || canAssignUserRole.value
  || canResetUserPassword.value
))

const searchForm = reactive({
  username: '',
  realName: ''
})

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const userList = ref([])
const roleList = ref([])
const departmentTree = ref([])
const departmentOptions = ref([])
const postOptions = ref([])

const dialogVisible = ref(false)
const roleDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const profileDrawerVisible = ref(false)
const profileLoading = ref(false)
const dialogTitle = ref('新增用户')
const userFormRef = ref(null)
const passwordFormRef = ref(null)

// 用户新增和编辑共用这份表单。
// 第一版组织模型里，departmentId 和 postId 都允许为空，表示暂不绑定。
const form = reactive({
  id: '',
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  departmentId: null,
  postId: null,
  roleIds: [],
  status: 1
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }]
}

const selectedUser = ref(null)
const selectedRoleIds = ref([])
const profileData = ref(null)

const passwordForm = reactive({
  id: '',
  username: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请确认密码'))
          return
        }
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

// 加载用户列表。
// 这里不额外传岗位条件，是因为第一版先把岗位能力落到维护和展示层，后续再继续扩展筛选项。
const loadUserList = async () => {
  const res = await request.get('/user/list', {
    params: {
      page: currentPage.value,
      size: pageSize.value,
      username: searchForm.username,
      realName: searchForm.realName
    }
  })
  userList.value = res.data.records || []
  total.value = res.data.total || 0
}

const loadRoleList = async () => {
  const res = await request.get('/role/list', {
    params: { page: 1, size: 200 }
  })
  roleList.value = res.data.records || []
}

// 加载部门树并拍平成下拉选项。
// 用户表单不需要复杂树组件，但仍要保留层级感，所以这里统一把树节点转成带缩进的 label。
const loadDepartmentTree = async () => {
  const res = await request.get('/department/tree')
  departmentTree.value = res.data || []
  departmentOptions.value = flattenDepartmentOptions(departmentTree.value)
}

const loadPostOptions = async (departmentId = form.departmentId) => {
  if (!departmentId) {
    postOptions.value = []
    return
  }
  const res = await request.get('/post/options', {
    params: {
      departmentId
    }
  })
  postOptions.value = res.data || []
}

const flattenDepartmentOptions = (nodes = [], level = 0) => {
  return nodes.flatMap((node) => {
    const prefix = level > 0 ? `${'--'.repeat(level)} ` : ''
    const current = {
      id: node.id,
      label: `${prefix}${node.name}`
    }
    return [current, ...flattenDepartmentOptions(node.children || [], level + 1)]
  })
}

const handleSearch = () => {
  currentPage.value = 1
  loadUserList()
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.realName = ''
  currentPage.value = 1
  loadUserList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadUserList()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  loadUserList()
}

const prepareOrganizationOptions = async () => {
  if (!canLoadOrganizationOptions.value) {
    return
  }
  const tasks = [loadDepartmentTree(), loadPostOptions()]
  if (canAssignUserRole.value) {
    tasks.push(loadRoleList())
  }
  await Promise.all(tasks)
}

const handleAddUser = async () => {
  dialogTitle.value = '新增用户'
  Object.assign(form, {
    id: '',
    username: '',
    password: '',
    realName: '',
    email: '',
    phone: '',
    departmentId: null,
    postId: null,
    roleIds: [],
    status: 1
  })
  await prepareOrganizationOptions()
  dialogVisible.value = true
}

const handleEditUser = async (row) => {
  dialogTitle.value = '编辑用户'
  await prepareOrganizationOptions()
  let roleIds = []
  if (canAssignUserRole.value) {
    const roleRes = await request.get(`/user/roles/${row.id}`)
    roleIds = roleRes.data || []
  }
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName,
    email: row.email,
    phone: row.phone,
    departmentId: row.departmentId ?? null,
    postId: row.postId ?? null,
    roleIds,
    status: row.status
  })
  dialogVisible.value = true
}

const handleDeleteUser = (id) => {
  ElMessageBox.confirm('确定删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/user/${id}`)
    ElMessage.success('删除成功')
    loadUserList()
  })
}

const handleAssignRole = async (user) => {
  selectedUser.value = user
  roleDialogVisible.value = true
  await loadRoleList()
  const res = await request.get(`/user/roles/${user.id}`)
  selectedRoleIds.value = res.data || []
}

const handleResetPassword = (user) => {
  Object.assign(passwordForm, {
    id: user.id,
    username: user.username,
    newPassword: '',
    confirmPassword: ''
  })
  passwordDialogVisible.value = true
}

const handleViewProfile = async (user) => {
  profileDrawerVisible.value = true
  profileLoading.value = true
  try {
    const res = await request.get(`/user/profile/${user.id}`)
    profileData.value = res.data
  } finally {
    profileLoading.value = false
  }
}

// 提交用户表单。
// 部门和岗位都允许为空，所以统一转成 null，避免把空字符串写进后端。
const handleSubmit = async () => {
  const valid = await userFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = {
    ...form,
    departmentId: form.departmentId || null,
    postId: form.postId || null,
    roleIds: form.roleIds || []
  }

  if (form.id) {
    await request.put('/user', payload)
    ElMessage.success('编辑成功')
  } else {
    await request.post('/user', payload)
    ElMessage.success('新增成功')
  }

  dialogVisible.value = false
  loadUserList()
}

const handleAssignRoleSubmit = async () => {
  if (!selectedRoleIds.value.length) {
    ElMessage.error('请至少选择一个角色')
    return
  }

  await request.post('/user/assign-role', {
    userId: selectedUser.value.id,
    roleIds: selectedRoleIds.value
  })

  ElMessage.success('角色分配成功')
  roleDialogVisible.value = false
  loadUserList()
}

// 按岗位带出默认角色。
// 默认用于“选完岗位后的第一时间推荐角色模板”，如果管理员想重新覆盖，也可以手动点击按钮再次应用。
const applyPostDefaultRoles = async (showMessage = false) => {
  if (!form.postId) {
    form.roleIds = []
    if (showMessage) {
      ElMessage.warning('请先选择所属岗位')
    }
    return
  }

  const res = await request.get(`/post/default-roles/${form.postId}`)
  form.roleIds = res.data || []
  if (showMessage) {
    if (form.roleIds.length) {
      ElMessage.success('已按岗位带出默认角色')
    } else {
      ElMessage.info('当前岗位暂未配置默认角色')
    }
  }
}

const handleResetPasswordSubmit = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  await request.put('/user/reset-password', {
    id: passwordForm.id,
    newPassword: passwordForm.newPassword
  })

  ElMessage.success('密码重置成功')
  passwordDialogVisible.value = false
}

const handleStatusChange = async (row, newStatus) => {
  const oldStatus = row.status === 1 ? 0 : 1
  try {
    await request.put('/user/status', {
      id: row.id,
      status: Number(newStatus)
    })
    ElMessage.success('状态更新成功')
    await loadUserList()
  } catch (error) {
    row.status = oldStatus
  }
}

onMounted(async () => {
  await loadUserList()
  if (canLoadOrganizationOptions.value) {
    await prepareOrganizationOptions()
  }
  if (canAssignUserRole.value) {
    await loadRoleList()
  }
})

// 部门变化后立刻重算岗位下拉。
// 如果当前岗位已经不属于新部门，就主动清空，避免用户无意中提交跨部门岗位。
watch(() => form.departmentId, async (newDepartmentId, oldDepartmentId) => {
  if (!dialogVisible.value) {
    return
  }
  await loadPostOptions(newDepartmentId)
  if (!newDepartmentId) {
    form.postId = null
    return
  }
  if (newDepartmentId !== oldDepartmentId) {
    const currentPostStillValid = postOptions.value.some(item => item.id === form.postId)
    if (!currentPostStillValid) {
      form.postId = null
      form.roleIds = []
    }
  }
})

// 岗位变化时自动带出岗位默认角色。
// 这样新增用户时只需要先选部门、再选岗位，就能快速得到一套推荐授权模板。
watch(() => form.postId, async (newPostId, oldPostId) => {
  if (!dialogVisible.value) {
    return
  }
  if (!newPostId) {
    form.roleIds = []
    return
  }
  if (newPostId !== oldPostId) {
    await applyPostDefaultRoles(false)
  }
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

.scope-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: linear-gradient(135deg, #eff6ff 0%, #f8fbff 100%);
  color: #1e3a8a;
  flex-wrap: wrap;
}

.scope-banner__label {
  font-size: 12px;
  color: #64748b;
}

.scope-banner strong {
  font-size: 15px;
}

.scope-banner__hint {
  font-size: 13px;
  color: #475569;
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

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.role-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
}

.role-panel {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: linear-gradient(135deg, #f8fbff 0%, #eff6ff 100%);
}

.role-panel__toolbar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.role-panel__tip {
  font-size: 13px;
  line-height: 1.7;
  color: #475569;
}

.profile-loading {
  padding: 24px 0;
  color: #64748b;
}

.profile-section {
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.profile-section:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

.profile-section h4 {
  margin: 0 0 14px;
  font-size: 16px;
  color: #0f172a;
}

.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.profile-item {
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.profile-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: #64748b;
}

.profile-item strong {
  color: #0f172a;
  font-size: 14px;
}

.profile-block {
  margin-top: 14px;
  padding: 14px;
  border-radius: 12px;
  background: #f8fafc;
}

.profile-block p {
  margin: 0;
  color: #334155;
  line-height: 1.7;
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.empty-hint {
  color: #94a3b8;
  font-size: 13px;
}

@media (max-width: 900px) {
  .hero-panel {
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
