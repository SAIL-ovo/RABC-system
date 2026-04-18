<template>
  <div class="log-page">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">AUDIT CENTER</p>
        <h2>操作日志</h2>
        <p class="hero-text">
          查看后台关键写操作的审计记录，快速定位是谁、在什么时候、对哪项功能做了修改，并支持导出和专项追踪。
        </p>
      </div>
      <div class="hero-actions">
        <div class="stat-card">
          <span class="stat-label">日志总数</span>
          <strong>{{ total }}</strong>
        </div>
        <el-button type="primary" plain class="export-btn" @click="handleExport">
          导出日志
        </el-button>
      </div>
    </section>

    <el-card class="content-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h3>日志列表</h3>
            <p>支持按操作人、操作名称、执行结果、审计分类和时间范围筛选，并提供权限变更专项视图。</p>
          </div>
        </div>
      </template>

      <div class="quick-filters">
        <el-radio-group v-model="searchForm.auditType" @change="handleSearch">
          <el-radio-button label="">全部日志</el-radio-button>
          <el-radio-button label="AUTH_CHANGE">权限变更</el-radio-button>
          <el-radio-button label="USER_CHANGE">用户变更</el-radio-button>
          <el-radio-button label="ORG_CHANGE">组织变更</el-radio-button>
          <el-radio-button label="SECURITY">安全操作</el-radio-button>
        </el-radio-group>
      </div>

      <div class="search-bar">
        <el-input v-model="searchForm.username" placeholder="请输入操作人" style="width: 220px" />
        <el-input v-model="searchForm.operation" placeholder="请输入操作名称" style="width: 240px" />
        <el-input v-model="searchForm.keyword" placeholder="请输入关键字 / 接口 / 参数" style="width: 260px" />
        <el-select v-model="searchForm.status" placeholder="执行结果" clearable style="width: 180px">
          <el-option :value="1" label="成功" />
          <el-option :value="0" label="失败" />
        </el-select>
        <el-date-picker
          v-model="searchForm.dateRange"
          type="daterange"
          unlink-panels
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 300px"
        />
        <el-button type="primary" @click="handleSearch">
          <span>搜索</span>
        </el-button>
        <el-button @click="handleReset">
          <span>重置</span>
        </el-button>
      </div>

      <el-table :data="logList" border class="data-table">
        <el-table-column prop="username" label="操作人" min-width="120" />
        <el-table-column prop="operation" label="操作名称" min-width="160" />
        <el-table-column label="审计分类" width="120">
          <template #default="{ row }">
            <el-tag :type="resolveAuditTypeTag(row.operation)" effect="light" round>
              {{ resolveAuditTypeLabel(row.operation) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="method" label="接口" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" min-width="140" />
        <el-table-column label="执行结果" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="light">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="180" show-overflow-tooltip />
        <el-table-column prop="createTime" label="操作时间" width="180" />
        <el-table-column label="请求参数" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="params-cell">
              <el-text class="params-text" truncated>
                {{ row.params || '-' }}
              </el-text>
              <el-button v-if="row.params" link type="primary" @click="handleViewParams(row)">
                查看详情
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

    <el-dialog v-model="paramsDialogVisible" title="请求参数详情" width="720px">
      <pre class="params-detail">{{ selectedParams || '-' }}</pre>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import request from '@/utils/request'

// 日志筛选条件。
// 这里额外补了 auditType，专门服务于“权限变更专项日志视图”和其他审计分类的快速切换。
const searchForm = reactive({
  username: '',
  operation: '',
  keyword: '',
  auditType: '',
  status: undefined,
  dateRange: []
})

// 分页和列表数据。
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const logList = ref([])

// 参数详情弹窗状态。
const paramsDialogVisible = ref(false)
const selectedParams = ref('')

const buildQueryParams = () => {
  const [startDate, endDate] = searchForm.dateRange || []
  return {
    page: currentPage.value,
    size: pageSize.value,
    username: searchForm.username,
    operation: searchForm.operation,
    keyword: searchForm.keyword,
    auditType: searchForm.auditType || undefined,
    status: searchForm.status,
    startDate,
    endDate
  }
}

// 按当前筛选条件加载操作日志分页数据。
const loadLogList = async () => {
  const res = await request.get('/operation-log/list', {
    params: buildQueryParams()
  })

  logList.value = res.data.records || []
  total.value = res.data.total || 0
}

// 搜索时回到第一页，避免停留在旧分页。
const handleSearch = () => {
  currentPage.value = 1
  loadLogList()
}

// 清空筛选条件并重新加载日志列表。
const handleReset = () => {
  searchForm.username = ''
  searchForm.operation = ''
  searchForm.keyword = ''
  searchForm.auditType = ''
  searchForm.status = undefined
  searchForm.dateRange = []
  currentPage.value = 1
  loadLogList()
}

// 导出当前筛选条件对应的日志。
// 这样管理员在页面上看到什么范围，导出的 CSV 就是什么范围，减少二次解释成本。
const handleExport = async () => {
  const res = await request.get('/operation-log/export', {
    params: buildQueryParams(),
    responseType: 'blob'
  })
  const blob = new Blob([res], { type: 'text/csv;charset=utf-8;' })
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `操作日志-${new Date().toISOString().slice(0, 10)}.csv`
  link.click()
  window.URL.revokeObjectURL(url)
}

// 切换每页数量后立刻刷新数据。
const handleSizeChange = (size) => {
  pageSize.value = size
  loadLogList()
}

// 切换页码后加载对应页日志。
const handleCurrentChange = (page) => {
  currentPage.value = page
  loadLogList()
}

// 查看一条日志的完整请求参数，避免表格里长文本被截断。
const handleViewParams = (row) => {
  selectedParams.value = row.params || ''
  paramsDialogVisible.value = true
}

const resolveAuditTypeLabel = (operation = '') => {
  if (!operation) return '其他操作'
  if (operation.includes('分配用户角色') || operation.includes('分配角色权限') || operation.includes('角色') || operation.includes('权限')) {
    return '权限变更'
  }
  if (operation.includes('部门') || operation.includes('岗位')) {
    return '组织变更'
  }
  if (operation.includes('密码') || operation.includes('登录') || operation.includes('退出')) {
    return '安全操作'
  }
  if (operation.includes('用户')) {
    return '用户变更'
  }
  return '其他操作'
}

const resolveAuditTypeTag = (operation = '') => {
  const label = resolveAuditTypeLabel(operation)
  if (label === '权限变更') return 'warning'
  if (label === '组织变更') return 'success'
  if (label === '安全操作') return 'danger'
  if (label === '用户变更') return 'primary'
  return 'info'
}

// 页面加载后立刻读取最新操作日志。
onMounted(() => {
  loadLogList()
})
</script>

<style scoped>
.log-page {
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
  background: linear-gradient(135deg, #0f172a 0%, #155e75 100%);
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

.export-btn {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.28);
  color: #fff;
}

.content-card {
  border: none;
  border-radius: 22px;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.card-header h3 {
  margin: 0;
  font-size: 22px;
  color: #0f172a;
}

.card-header p {
  margin: 8px 0 0;
  color: #64748b;
}

.quick-filters {
  margin-bottom: 16px;
}

.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
}

.data-table :deep(.el-table__cell) {
  vertical-align: top;
}

.params-cell {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.params-text {
  flex: 1;
  min-width: 0;
}

.params-cell .el-button {
  flex-shrink: 0;
}

.params-detail {
  margin: 0;
  max-height: 420px;
  overflow: auto;
  padding: 16px;
  border-radius: 14px;
  background: #0f172a;
  color: #e2e8f0;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 768px) {
  .hero-panel {
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }
}
</style>
