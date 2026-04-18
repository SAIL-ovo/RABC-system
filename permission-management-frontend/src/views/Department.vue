<template>
  <div class="department-page">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">ORG CENTER</p>
        <h2>部门管理</h2>
        <p class="hero-text">
          维护部门层级、负责人、联系方式和用户归属，为后续组织模型扩展、审批流和数据权限打下结构化基础。
        </p>
      </div>
      <div class="hero-actions">
        <div class="stat-card">
          <span class="stat-label">部门总数</span>
          <strong>{{ departmentList.length }}</strong>
        </div>
        <div class="stat-card">
          <span class="stat-label">顶级部门</span>
          <strong>{{ departmentTree.length }}</strong>
        </div>
        <el-button v-if="canCreateDepartment" type="primary" size="large" class="create-btn" @click="handleAddDepartment">
          <el-icon><Plus /></el-icon>
          <span>新增部门</span>
        </el-button>
      </div>
    </section>

    <el-card class="department-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h3>部门树</h3>
            <p>支持查看部门层级、结构化负责人、联系方式、用户数量以及部门维护操作。</p>
          </div>
        </div>
      </template>

      <el-tree
        :data="departmentTree"
        node-key="id"
        :props="treeProps"
        default-expand-all
        class="department-tree"
      >
        <template #default="{ data }">
          <div class="tree-node">
            <div class="node-main">
              <div class="node-title-row">
                <span class="node-title">{{ data.name }}</span>
                <el-tag size="small" effect="plain" class="node-tag">
                  {{ data.code }}
                </el-tag>
                <el-tag size="small" effect="light" :type="data.status === 1 ? 'success' : 'danger'" class="node-tag">
                  {{ data.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </div>
              <div class="node-meta">
                <span>负责人：{{ data.leader || '未设置' }}</span>
                <span>联系电话：{{ data.phone || '未设置' }}</span>
                <span>用户数量：{{ data.userCount ?? 0 }}</span>
                <span>排序：{{ data.sort ?? 0 }}</span>
              </div>
            </div>
            <div v-if="showDepartmentActions" class="tree-node-actions">
              <el-button v-if="canEditDepartment" type="primary" plain size="small" @click.stop="handleEditDepartment(data)">
                <el-icon><Edit /></el-icon>
                <span>编辑</span>
              </el-button>
              <el-button v-if="canDeleteDepartment" type="danger" plain size="small" @click.stop="handleDeleteDepartment(data.id)">
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
            </div>
          </div>
        </template>
      </el-tree>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" class="department-dialog">
      <el-form ref="departmentFormRef" :model="form" :rules="rules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="部门名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入部门名称" />
          </el-form-item>
          <el-form-item label="部门编码" prop="code">
            <el-input v-model="form.code" placeholder="请输入部门编码" />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="上级部门" prop="parentId">
            <el-select v-model="form.parentId" placeholder="请选择上级部门" style="width: 100%">
              <el-option :value="0" label="顶级部门" />
              <el-option
                v-for="item in parentDepartmentOptions"
                :key="item.id"
                :label="item.label"
                :value="item.id"
                :disabled="form.id === item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" :min="0" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="form-grid">
          <el-form-item label="负责人" prop="leaderUserId">
            <el-select v-model="form.leaderUserId" placeholder="请选择负责人" clearable filterable style="width: 100%">
              <el-option :value="null" label="暂不设置负责人" />
              <el-option
                v-for="user in leaderOptions"
                :key="user.id"
                :label="buildLeaderOptionLabel(user)"
                :value="user.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="联系电话" prop="phone">
            <el-input v-model="form.phone" placeholder="请输入联系电话" />
          </el-form-item>
        </div>

        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Edit, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useStore } from 'vuex'
import request from '@/utils/request'

const store = useStore()

// 部门模块的细粒度按钮权限。
// 现阶段保留 department:manage 作为兼容兜底，方便旧角色平滑迁移。
const canManageDepartments = computed(() => store.getters.hasPermission('department:manage'))
const canCreateDepartment = computed(() => store.getters.hasPermission('department:create') || canManageDepartments.value)
const canEditDepartment = computed(() => store.getters.hasPermission('department:update') || canManageDepartments.value)
const canDeleteDepartment = computed(() => store.getters.hasPermission('department:delete') || canManageDepartments.value)

const showDepartmentActions = computed(() => canEditDepartment.value || canDeleteDepartment.value)

const treeProps = {
  children: 'children',
  label: 'name'
}

const departmentTree = ref([])
const departmentList = ref([])
const parentDepartmentOptions = ref([])
const leaderOptions = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增部门')
const departmentFormRef = ref(null)

// 部门新增和编辑共用同一份表单。
// parentId 为 0 表示顶级部门；leaderUserId 为空表示当前部门暂不设置负责人。
const form = reactive({
  id: '',
  name: '',
  code: '',
  parentId: 0,
  leaderUserId: null,
  phone: '',
  sort: 0,
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入部门编码', trigger: 'blur' }]
}

// 同时加载扁平部门列表和树结构。
// 树用于展示，扁平列表用于统计和编辑时回填；负责人选项单独走用户列表接口，避免手工录入负责人姓名。
const loadDepartmentData = async () => {
  const [treeRes, listRes, userRes] = await Promise.all([
    request.get('/department/tree'),
    request.get('/department/list'),
    request.get('/user/list', {
      params: {
        page: 1,
        size: 500
      }
    })
  ])

  departmentTree.value = treeRes.data || []
  departmentList.value = listRes.data || []
  parentDepartmentOptions.value = flattenDepartmentOptions(departmentTree.value)
  leaderOptions.value = userRes.data?.records || []
}

// 把树形部门拍平成带层级缩进的下拉选项。
// 这样用户在选择上级部门时可以直观看到组织层级。
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

// 负责人下拉里顺手带上用户名和所属部门，避免同名用户难分辨。
const buildLeaderOptionLabel = (user) => {
  const parts = [user.realName || user.username, user.username ? `(${user.username})` : '']
  if (user.departmentName) {
    parts.push(`- ${user.departmentName}`)
  }
  return parts.filter(Boolean).join(' ')
}

const handleAddDepartment = () => {
  dialogTitle.value = '新增部门'
  Object.assign(form, {
    id: '',
    name: '',
    code: '',
    parentId: 0,
    leaderUserId: null,
    phone: '',
    sort: 0,
    status: 1
  })
  dialogVisible.value = true
}

const handleEditDepartment = (department) => {
  dialogTitle.value = '编辑部门'
  Object.assign(form, {
    id: department.id,
    name: department.name,
    code: department.code,
    parentId: department.parentId ?? 0,
    leaderUserId: department.leaderUserId ?? null,
    phone: department.phone || '',
    sort: department.sort ?? 0,
    status: department.status ?? 1
  })
  dialogVisible.value = true
}

const handleDeleteDepartment = (id) => {
  ElMessageBox.confirm('确定删除该部门吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/department/${id}`)
    ElMessage.success('删除成功')
    loadDepartmentData()
  }).catch(() => null)
}

// 提交部门表单。
// 负责人改成结构化 userId 后，前端只传 leaderUserId，部门展示名由后端统一补齐快照。
const handleSubmit = async () => {
  const valid = await departmentFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = {
    ...form,
    parentId: Number(form.parentId || 0),
    leaderUserId: form.leaderUserId || null,
    sort: Number(form.sort || 0),
    status: Number(form.status)
  }

  if (form.id) {
    await request.put('/department', payload)
    ElMessage.success('编辑成功')
  } else {
    await request.post('/department', payload)
    ElMessage.success('新增成功')
  }

  dialogVisible.value = false
  loadDepartmentData()
}

onMounted(() => {
  loadDepartmentData()
})
</script>

<style scoped>
.department-page {
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

.department-card {
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

.department-tree {
  padding: 6px 4px 14px;
}

.department-tree :deep(.el-tree-node) {
  margin: 6px 0;
}

.department-tree :deep(.el-tree-node__content) {
  height: auto;
  padding: 0;
  align-items: stretch;
}

.department-tree :deep(.el-tree-node__expand-icon) {
  margin-right: 8px;
}

.department-tree :deep(.el-tree-node__children) {
  padding-left: 22px;
}

.tree-node {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
  box-sizing: border-box;
}

.tree-node:hover {
  transform: translateY(-1px);
  border-color: #bfdbfe;
  box-shadow: 0 10px 20px rgba(37, 99, 235, 0.08);
}

.node-main {
  min-width: 0;
  flex: 1;
}

.node-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.node-title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.node-tag {
  border-radius: 999px;
}

.node-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin-top: 8px;
  font-size: 12px;
  color: #64748b;
}

.tree-node-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

@media (max-width: 900px) {
  .hero-panel {
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .tree-node {
    align-items: flex-start;
    flex-direction: column;
  }

  .tree-node-actions {
    width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
