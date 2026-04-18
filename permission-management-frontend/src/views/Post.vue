<template>
  <div class="manage-page">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">POST MANAGEMENT</p>
        <h2>岗位管理</h2>
        <p class="hero-text">
          统一维护组织内部岗位的编码、状态、职责说明与默认角色模板，让岗位配置不仅能描述职责，还能承接后续授权联动。
        </p>
      </div>
      <div class="hero-actions">
        <div class="stat-card">
          <span class="stat-label">岗位总数</span>
          <strong>{{ total }}</strong>
        </div>
        <el-button v-if="canCreatePost" type="primary" size="large" class="create-btn" @click="handleAddPost">
          <el-icon><Plus /></el-icon>
          <span>新增岗位</span>
        </el-button>
      </div>
    </section>

    <el-card class="content-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h3>岗位列表</h3>
            <p>支持按名称、编码、状态筛选岗位，并直接查看岗位绑定用户数和默认角色模板。</p>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input v-model="searchForm.name" placeholder="请输入岗位名称" style="width: 220px">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-input v-model="searchForm.code" placeholder="请输入岗位编码" style="width: 220px">
          <template #prefix>
            <el-icon><Tickets /></el-icon>
          </template>
        </el-input>
        <el-select v-model="searchForm.status" placeholder="请选择状态" clearable style="width: 180px">
          <el-option :value="1" label="启用" />
          <el-option :value="0" label="停用" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          <span>搜索</span>
        </el-button>
        <el-button @click="handleReset">
          <el-icon><Refresh /></el-icon>
          <span>重置</span>
        </el-button>
      </div>

      <el-table :data="postList" border class="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="岗位名称" min-width="180" />
        <el-table-column prop="code" label="岗位编码" min-width="180" show-overflow-tooltip />
        <el-table-column prop="departmentName" label="所属部门" min-width="160" show-overflow-tooltip />
        <el-table-column prop="defaultRoleCount" label="默认角色数" width="110" align="center" />
        <el-table-column prop="defaultRoleSummary" label="默认角色模板" min-width="220" show-overflow-tooltip />
        <el-table-column prop="userCount" label="绑定用户数" width="110" align="center" />
        <el-table-column prop="sort" label="排序" width="90" align="center" />
        <el-table-column prop="remark" label="岗位说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="light" round>
              {{ scope.row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="showPostActions" label="操作" width="200" fixed="right">
          <template #default="scope">
            <div class="action-group">
              <el-button v-if="canEditPost" type="primary" plain size="small" @click="handleEditPost(scope.row)">
                <el-icon><Edit /></el-icon>
                <span>编辑</span>
              </el-button>
              <el-button v-if="canDeletePost" type="danger" plain size="small" @click="handleDeletePost(scope.row)">
                <el-icon><Delete /></el-icon>
                <span>删除</span>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" class="manage-dialog">
      <el-form ref="postFormRef" :model="form" :rules="rules" label-width="100px">
        <div class="form-grid">
          <el-form-item label="岗位名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入岗位名称" />
          </el-form-item>
          <el-form-item label="岗位编码" prop="code">
            <el-input v-model="form.code" placeholder="请输入岗位编码" />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="所属部门" prop="departmentId">
            <el-select v-model="form.departmentId" placeholder="请选择所属部门" style="width: 100%">
              <el-option
                v-for="item in departmentOptions"
                :key="item.id"
                :label="item.label"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="form.sort" :min="0" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="默认角色">
          <div class="role-panel">
            <div class="role-panel__tip">
              这里配置的是岗位推荐角色模板，后续做用户创建联动时可以直接按岗位带出角色建议。
            </div>
            <el-checkbox-group v-model="form.defaultRoleIds" class="role-checkbox-group">
              <el-checkbox v-for="role in roleOptions" :key="role.id" :value="role.id">
                {{ role.name }}
              </el-checkbox>
            </el-checkbox-group>
            <el-empty v-if="!roleOptions.length" description="暂无可选角色" :image-size="72" />
          </div>
        </el-form-item>
        <el-form-item label="岗位说明" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="请输入岗位职责或岗位说明"
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
import { Delete, Edit, Plus, Refresh, Search, Tickets } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useStore } from 'vuex'
import request from '@/utils/request'

const store = useStore()

const canManagePosts = computed(() => store.getters.hasPermission('post:manage'))
const canCreatePost = computed(() => store.getters.hasPermission('post:create') || canManagePosts.value)
const canEditPost = computed(() => store.getters.hasPermission('post:update') || canManagePosts.value)
const canDeletePost = computed(() => store.getters.hasPermission('post:delete') || canManagePosts.value)
const showPostActions = computed(() => canEditPost.value || canDeletePost.value)

const searchForm = reactive({
  name: '',
  code: '',
  status: null
})

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const postList = ref([])
const roleOptions = ref([])
const departmentOptions = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('新增岗位')
const postFormRef = ref(null)

// 岗位表单同时承载基础信息和默认角色模板。
// 这样管理员在维护岗位时就能一次完成“岗位定义 + 授权模板”配置，减少来回切页面的成本。
const form = reactive({
  id: '',
  name: '',
  code: '',
  departmentId: null,
  sort: 0,
  status: 1,
  remark: '',
  defaultRoleIds: []
})

const rules = {
  name: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择所属部门', trigger: 'change' }]
}

const loadPostList = async () => {
  const res = await request.get('/post/list', {
    params: {
      page: currentPage.value,
      size: pageSize.value,
      name: searchForm.name,
      code: searchForm.code,
      status: searchForm.status
    }
  })
  postList.value = res.data.records || []
  total.value = res.data.total || 0
}

const loadRoleOptions = async () => {
  const res = await request.get('/post/role-options')
  roleOptions.value = res.data || []
}

const loadDepartmentOptions = async () => {
  const res = await request.get('/department/tree')
  departmentOptions.value = flattenDepartmentOptions(res.data || [])
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
  loadPostList()
}

const handleReset = () => {
  searchForm.name = ''
  searchForm.code = ''
  searchForm.status = null
  currentPage.value = 1
  loadPostList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  loadPostList()
}

const handleCurrentChange = (current) => {
  currentPage.value = current
  loadPostList()
}

const resetForm = () => {
  Object.assign(form, {
    id: '',
    name: '',
    code: '',
    departmentId: null,
    sort: 0,
    status: 1,
    remark: '',
    defaultRoleIds: []
  })
}

const prepareRoleOptions = async () => {
  await Promise.all([
    roleOptions.value.length ? Promise.resolve() : loadRoleOptions(),
    departmentOptions.value.length ? Promise.resolve() : loadDepartmentOptions()
  ])
}

const handleAddPost = async () => {
  dialogTitle.value = '新增岗位'
  resetForm()
  await prepareRoleOptions()
  dialogVisible.value = true
}

const handleEditPost = async (row) => {
  dialogTitle.value = '编辑岗位'
  await prepareRoleOptions()
  const res = await request.get(`/post/default-roles/${row.id}`)
  Object.assign(form, {
    id: row.id,
    name: row.name,
    code: row.code,
    departmentId: row.departmentId ?? null,
    sort: row.sort ?? 0,
    status: row.status,
    remark: row.remark || '',
    defaultRoleIds: res.data || []
  })
  dialogVisible.value = true
}

const handleDeletePost = (row) => {
  const warning = row.userCount > 0
    ? `该岗位当前仍绑定 ${row.userCount} 个用户，请先调整用户岗位归属后再删除。`
    : '确定删除该岗位吗？'

  ElMessageBox.confirm(warning, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.delete(`/post/${row.id}`)
    ElMessage.success('删除成功')
    loadPostList()
  }).catch(() => null)
}

// 提交岗位表单时，默认角色列表一并提交给后端。
// 这样岗位本身就能沉淀出一套“推荐授权模板”，后续扩用户联动时不需要再补历史数据。
const handleSubmit = async () => {
  const valid = await postFormRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload = {
    ...form,
    remark: form.remark || '',
    defaultRoleIds: form.defaultRoleIds || []
  }

  if (form.id) {
    await request.put('/post', payload)
    ElMessage.success('编辑成功')
  } else {
    await request.post('/post', payload)
    ElMessage.success('新增成功')
  }

  dialogVisible.value = false
  loadPostList()
}

onMounted(async () => {
  await loadPostList()
  if (canCreatePost.value || canEditPost.value) {
    await Promise.all([loadRoleOptions(), loadDepartmentOptions()])
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
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.1), transparent 34%),
    linear-gradient(180deg, #f7fafc 0%, #edf4f7 100%);
  padding: 4px;
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 28px;
  border-radius: 20px;
  background: linear-gradient(135deg, #0f766e 0%, #0f172a 100%);
  color: #fff;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.18);
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
}

.create-btn {
  height: 48px;
  padding: 0 22px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #f59e0b 0%, #f97316 100%);
  box-shadow: 0 10px 22px rgba(245, 158, 11, 0.28);
}

.content-card {
  border: none;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
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
  background: #f0fdfa !important;
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

.role-panel {
  width: 100%;
  padding: 14px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #f8fafc;
}

.role-panel__tip {
  margin-bottom: 12px;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.role-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
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
