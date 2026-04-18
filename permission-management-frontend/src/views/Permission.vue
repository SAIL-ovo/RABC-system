<template>
  <div class="permission-page">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">PERMISSION CENTER</p>
        <h2>权限管理</h2>
        <p class="hero-text">
          维护菜单、按钮与接口权限，梳理系统访问边界。左侧树用于查看层级，右上角可快速新增权限节点。
        </p>
      </div>
      <div class="hero-actions">
        <div class="stat-card">
          <span class="stat-label">权限总数</span>
          <strong>{{ permissionList.length }}</strong>
        </div>
        <div class="stat-card">
          <span class="stat-label">顶级节点</span>
          <strong>{{ permissionTree.length }}</strong>
        </div>
        <el-button v-if="canCreatePermission" type="primary" size="large" class="create-btn" @click="handleAddPermission">
          <el-icon><Plus /></el-icon>
          <span>新增权限</span>
        </el-button>
      </div>
    </section>

    <el-card class="permission-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h3>权限树</h3>
            <p>可直接查看每个权限节点的类型、状态和接口信息。</p>
          </div>
        </div>
      </template>

      <el-tree
        :data="permissionTree"
        node-key="id"
        :props="treeProps"
        default-expand-all
        class="permission-tree"
      >
        <template #default="{ data }">
          <div class="tree-node">
            <div class="node-main">
              <div class="node-title-row">
                <span class="node-title">{{ data.name }}</span>
                <el-tag v-if="data.code === 'log:view'" size="small" type="warning" effect="light" class="node-tag">
                  日志菜单
                </el-tag>
                <el-tag size="small" effect="plain" class="node-tag">
                  {{ getPermissionTypeLabel(data.type) }}
                </el-tag>
                <el-tag
                  size="small"
                  :type="data.status === 1 ? 'success' : 'danger'"
                  effect="light"
                  class="node-tag"
                >
                  {{ data.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </div>
              <div class="node-meta">
                <span>编码：{{ data.code || '-' }}</span>
                <span>路径：{{ data.url || '-' }}</span>
                <span>方法：{{ data.method || '-' }}</span>
                <span>排序：{{ data.sort ?? 0 }}</span>
              </div>
            </div>
            <div class="tree-node-actions">
              <template v-if="showPermissionActions">
                <el-button v-if="canEditPermissionAction" type="primary" plain size="small" @click.stop="handleEditPermission(data)">
                  <el-icon><Edit /></el-icon>
                  <span>编辑</span>
                </el-button>
                <el-button v-if="canDeletePermissionAction" type="danger" plain size="small" @click.stop="handleDeletePermission(data.id)">
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </el-button>
              </template>
            </div>
          </div>
        </template>
      </el-tree>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" class="permission-dialog">
        <el-form ref="permissionFormRef" :model="form" :rules="rules" label-width="100px" class="permission-form">
        <div class="form-grid">
          <el-form-item label="类型" prop="type">
            <el-select v-model="form.type" placeholder="请选择权限类型" style="width: 100%" @change="handleTypeChange">
              <el-option :value="1" label="菜单权限" />
              <el-option :value="2" label="按钮权限" />
              <el-option :value="3" label="接口权限" />
            </el-select>
          </el-form-item>
          <el-form-item label="父级权限" prop="parentId">
            <el-select v-model="form.parentId" placeholder="请选择父级权限" style="width: 100%">
              <el-option :value="0" label="顶级权限" :disabled="form.type !== 1" />
              <el-option
                v-for="item in parentPermissionOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
                :disabled="form.id && item.id === form.id"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入权限名称" />
        </el-form-item>
        <el-form-item label="编码" prop="code">
          <el-input v-model="form.code" placeholder="请输入权限编码，例如 user:manage" />
        </el-form-item>
        <el-form-item v-if="form.type !== 2" :label="form.type === 1 ? '菜单路径' : '接口路径'" prop="url">
          <el-input
            v-model="form.url"
            :placeholder="form.type === 1 ? '请输入前端菜单路径，例如 /home/user' : '请输入接口路径，例如 /api/user/list'"
          />
        </el-form-item>
        <el-form-item v-if="form.type === 3" label="请求方式" prop="method">
          <el-select v-model="form.method" placeholder="请选择请求方式" style="width: 100%">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item v-if="form.type === 1" label="图标" prop="icon">
            <el-input v-model="form.icon" placeholder="请输入图标名称" />
          </el-form-item>
        </div>
        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="启用"
            inactive-text="停用"
          />
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

// 全局状态仓库，用于读取权限模块按钮权限。
const store = useStore()

// 权限页的细粒度按钮权限。
// 现阶段保留 permission:manage 作为兜底，避免旧数据下页面直接失能。
const canManagePermissions = computed(() => store.getters.hasPermission('permission:manage'))
const canCreatePermission = computed(() => store.getters.hasPermission('permission:create') || canManagePermissions.value)
const canEditPermissionAction = computed(() => store.getters.hasPermission('permission:update') || canManagePermissions.value)
const canDeletePermissionAction = computed(() => store.getters.hasPermission('permission:delete') || canManagePermissions.value)

// 只有具备权限动作权限时才显示节点右侧操作区，纯查看用户只看树结构。
const showPermissionActions = computed(() => canEditPermissionAction.value || canDeletePermissionAction.value)

// 权限树、扁平列表以及弹窗状态。
const permissionTree = ref([])
const permissionList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增权限')
const permissionFormRef = ref(null)

// 权限新增/编辑表单。
const form = reactive({
  id: '',
  name: '',
  code: '',
  url: '',
  method: 'GET',
  parentId: 0,
  icon: '',
  sort: 0,
  type: 1,
  status: 1
})

// 权限表单校验规则。
const rules = {
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入权限编码', trigger: 'blur' }]
}

// 树组件字段映射。
const treeProps = {
  children: 'children',
  label: 'name'
}

// 只有菜单权限才能作为父级，按钮和接口节点都必须挂在菜单下面。
const parentPermissionOptions = computed(() => {
  return permissionList.value.filter((item) => item.type === 1)
})

// 加载权限树，用于主区域展示分层结构。
const loadPermissionTree = async () => {
  const res = await request.get('/permission/tree')
  permissionTree.value = res.data || []
}

// 加载扁平权限列表，用于“父级权限”下拉选择。
const loadPermissionList = async () => {
  const res = await request.get('/permission/list')
  permissionList.value = res.data || []
}

// 根据权限类型返回更直观的中文标签。
const getPermissionTypeLabel = (type) => {
  if (type === 1) return '菜单权限'
  if (type === 2) return '按钮权限'
  if (type === 3) return '接口权限'
  return '未知类型'
}

// 切换权限类型时，顺手把不适用的字段收敛掉，减少脏数据进入后端。
const handleTypeChange = (type) => {
  if (type !== 1) {
    form.parentId = form.parentId || (parentPermissionOptions.value[0]?.id ?? 0)
    form.icon = ''
  }

  if (type === 1 && !form.url) {
    form.method = 'GET'
  }
}

// 打开新增弹窗并重置权限表单。
const handleAddPermission = () => {
  dialogTitle.value = '新增权限'
  Object.assign(form, {
    id: '',
    name: '',
    code: '',
    url: '',
    method: 'GET',
    parentId: 0,
    icon: '',
    sort: 0,
    type: 1,
    status: 1
  })
  dialogVisible.value = true
}

// 编辑时把当前权限节点的数据回填到表单。
const handleEditPermission = (data) => {
  dialogTitle.value = '编辑权限'
  Object.assign(form, {
    id: data.id,
    name: data.name,
    code: data.code,
    url: data.url,
    method: data.method,
    parentId: data.parentId,
    icon: data.icon,
    sort: data.sort,
    type: data.type,
    status: data.status
  })
  dialogVisible.value = true
}

// 删除权限后同时刷新树和列表，避免前端状态过期。
const handleDeletePermission = (id) => {
  ElMessageBox.confirm('确定删除该权限吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/permission/${id}`)
    ElMessage.success('删除成功')
    loadPermissionTree()
    loadPermissionList()
  })
}

// 提交权限表单。
// 这里统一把数字字段转成 Number，避免后端收到字符串类型。
const handleSubmit = async () => {
  const valid = await permissionFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  const payload = {
    ...form,
    parentId: Number(form.parentId),
    sort: Number(form.sort),
    type: Number(form.type),
    status: Number(form.status)
  }

  if (form.id) {
    await request.put('/permission', payload)
    ElMessage.success('编辑成功')
  } else {
    await request.post('/permission', payload)
    ElMessage.success('新增成功')
  }

  dialogVisible.value = false
  loadPermissionTree()
  loadPermissionList()
}

// 页面初始化时同时准备树数据和扁平列表数据。
onMounted(() => {
  loadPermissionTree()
  loadPermissionList()
})
</script>

<style scoped>
.permission-page {
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

.permission-card {
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

.permission-tree {
  padding: 6px 4px 14px;
}

.permission-tree :deep(.el-tree-node) {
  margin: 6px 0;
}

.permission-tree :deep(.el-tree-node__content) {
  height: auto;
  padding: 0;
  align-items: stretch;
}

.permission-tree :deep(.el-tree-node__expand-icon) {
  margin-right: 8px;
}

.permission-tree :deep(.el-tree-node__children) {
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

.permission-form {
  padding-top: 6px;
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
