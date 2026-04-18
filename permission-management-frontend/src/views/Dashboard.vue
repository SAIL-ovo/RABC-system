<template>
  <div class="dashboard-page">
    <section class="hero-panel">
      <div>
        <p class="eyebrow">SYSTEM OVERVIEW</p>
        <h2>权限与组织驾驶舱</h2>
        <p class="hero-text">
          把当前系统的组织模型、授权治理和审计动态集中展示出来，方便演示项目亮点，也方便管理员快速感知系统运行状态。
        </p>
      </div>
      <div class="hero-chip-group">
        <div class="hero-chip">
          <span>组织模型</span>
          <strong>部门 + 岗位 + 用户</strong>
        </div>
        <div class="hero-chip">
          <span>授权模型</span>
          <strong>RBAC + 数据权限</strong>
        </div>
      </div>
    </section>

    <section class="summary-grid">
      <article v-for="card in summaryCards" :key="card.key" class="summary-card">
        <span class="summary-label">{{ card.label }}</span>
        <strong class="summary-value">{{ card.value }}</strong>
        <p class="summary-hint">{{ card.hint }}</p>
      </article>
    </section>

    <section class="dashboard-grid">
      <el-card class="dashboard-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h3>近 7 天审计趋势</h3>
              <p>按天展示总日志量和权限变更量，适合快速演示系统活跃度。</p>
            </div>
          </div>
        </template>
        <div class="trend-list">
          <div v-for="item in auditTrend" :key="item.date" class="trend-item">
            <div class="trend-item__meta">
              <span>{{ item.date }}</span>
              <span>总日志 {{ item.total }}</span>
            </div>
            <div class="trend-bars">
              <div class="trend-bar trend-bar--total" :style="{ width: `${calcWidth(item.total, maxTrendTotal)}%` }"></div>
              <div class="trend-bar trend-bar--auth" :style="{ width: `${calcWidth(item.authChange, maxTrendTotal)}%` }"></div>
            </div>
            <div class="trend-item__legend">
              <span>权限变更 {{ item.authChange }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="dashboard-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div>
              <h3>授权治理亮点</h3>
              <p>把项目最能写进简历的能力直接拆出来。</p>
            </div>
          </div>
        </template>
        <div class="highlight-grid">
          <div class="highlight-card">
            <span class="highlight-label">累计权限变更</span>
            <strong>{{ authHighlights.authChangeCount || 0 }}</strong>
          </div>
          <div class="highlight-card">
            <span class="highlight-label">启用岗位数</span>
            <strong>{{ authHighlights.enabledPostCount || 0 }}</strong>
          </div>
          <div class="highlight-card">
            <span class="highlight-label">已配置负责人部门</span>
            <strong>{{ authHighlights.departmentLeaderCount || 0 }}</strong>
          </div>
        </div>
        <div class="scope-block">
          <span class="scope-title">已支持数据范围</span>
          <div class="scope-tags">
            <el-tag v-for="scope in authHighlights.dataScopeTypes || []" :key="scope" effect="light" round>
              {{ scope }}
            </el-tag>
          </div>
        </div>
      </el-card>
    </section>

    <el-card class="dashboard-card" shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <h3>最近动态</h3>
            <p>最近 8 条关键操作，演示时可以直接说明系统具备审计回溯能力。</p>
          </div>
        </div>
      </template>
      <div class="activity-list">
        <div v-for="item in recentLogs" :key="item.id" class="activity-item">
          <div class="activity-badge" :class="badgeClass(item.auditType)">{{ item.auditType }}</div>
          <div class="activity-content">
            <div class="activity-title">
              <strong>{{ item.operation }}</strong>
              <el-tag :type="item.status === 1 ? 'success' : 'danger'" effect="light" size="small">
                {{ item.status === 1 ? '成功' : '失败' }}
              </el-tag>
            </div>
            <div class="activity-meta">
              <span>操作人：{{ item.username || '系统' }}</span>
              <span>时间：{{ item.createTime }}</span>
            </div>
          </div>
        </div>
        <div v-if="!recentLogs.length" class="empty-state">暂无动态</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '@/utils/request'

const summary = ref({})
const recentLogs = ref([])
const auditTrend = ref([])
const authHighlights = ref({})

const summaryCards = computed(() => [
  {
    key: 'userCount',
    label: '用户总数',
    value: summary.value.userCount || 0,
    hint: '访问主体的总量'
  },
  {
    key: 'roleCount',
    label: '角色总数',
    value: summary.value.roleCount || 0,
    hint: '角色模板与授权边界'
  },
  {
    key: 'departmentCount',
    label: '部门总数',
    value: summary.value.departmentCount || 0,
    hint: '组织结构树规模'
  },
  {
    key: 'postCount',
    label: '岗位总数',
    value: summary.value.postCount || 0,
    hint: '岗位与默认角色模板'
  },
  {
    key: 'permissionCount',
    label: '权限总数',
    value: summary.value.permissionCount || 0,
    hint: '菜单、按钮、接口权限'
  },
  {
    key: 'logCount',
    label: '审计日志数',
    value: summary.value.logCount || 0,
    hint: '关键操作留痕总量'
  }
])

const maxTrendTotal = computed(() => {
  const values = auditTrend.value.map(item => item.total || 0)
  return Math.max(...values, 1)
})

/**
 * 把近 7 天趋势按最近日期排在最前面。
 * 这里在前端再做一次兜底排序，这样即使后端进程还没重启、仍返回旧顺序，
 * 首页也能稳定展示成“今天在最上面”的效果，减少联调阶段的等待成本。
 */
const sortAuditTrendByLatest = (trendList = []) => {
  return [...trendList].sort((left, right) => {
    const [leftMonth = '0', leftDay = '0'] = String(left.date || '').split('-')
    const [rightMonth = '0', rightDay = '0'] = String(right.date || '').split('-')
    const leftValue = Number(leftMonth) * 100 + Number(leftDay)
    const rightValue = Number(rightMonth) * 100 + Number(rightDay)
    return rightValue - leftValue
  })
}

const loadDashboard = async () => {
  const res = await request.get('/dashboard/overview')
  summary.value = res.data.summary || {}
  recentLogs.value = res.data.recentLogs || []
  auditTrend.value = sortAuditTrendByLatest(res.data.auditTrend || [])
  authHighlights.value = res.data.authHighlights || {}
}

const calcWidth = (value, max) => {
  if (!max) return 0
  return Math.max((value / max) * 100, value > 0 ? 8 : 0)
}

const badgeClass = (auditType) => {
  if (auditType === '权限变更') return 'activity-badge--warning'
  if (auditType === '组织变更') return 'activity-badge--success'
  if (auditType === '安全操作') return 'activity-badge--danger'
  return 'activity-badge--primary'
}

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped>
.dashboard-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 18px;
  background:
    radial-gradient(circle at top right, rgba(245, 158, 11, 0.12), transparent 30%),
    radial-gradient(circle at top left, rgba(37, 99, 235, 0.08), transparent 28%),
    linear-gradient(180deg, #f8fafc 0%, #eef4f8 100%);
  padding: 4px;
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 28px 30px;
  border-radius: 24px;
  background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 55%, #0f766e 100%);
  color: #fff;
  box-shadow: 0 22px 48px rgba(15, 23, 42, 0.2);
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.72);
}

.hero-panel h2 {
  margin: 0;
  font-size: 32px;
  line-height: 1.12;
}

.hero-text {
  max-width: 700px;
  margin: 14px 0 0;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.8;
}

.hero-chip-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 220px;
}

.hero-chip {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(10px);
}

.hero-chip span {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.hero-chip strong {
  display: block;
  margin-top: 8px;
  font-size: 18px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.08);
}

.summary-label {
  display: block;
  font-size: 13px;
  color: #64748b;
}

.summary-value {
  display: block;
  margin-top: 10px;
  font-size: 30px;
  color: #0f172a;
}

.summary-hint {
  margin: 10px 0 0;
  font-size: 12px;
  color: #94a3b8;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 18px;
}

.dashboard-card {
  border: none;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
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

.trend-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.trend-item__meta,
.trend-item__legend {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #475569;
}

.trend-bars {
  margin: 10px 0 8px;
}

.trend-bar {
  height: 10px;
  border-radius: 999px;
}

.trend-bar + .trend-bar {
  margin-top: 8px;
}

.trend-bar--total {
  background: linear-gradient(90deg, #2563eb 0%, #38bdf8 100%);
}

.trend-bar--auth {
  background: linear-gradient(90deg, #f59e0b 0%, #f97316 100%);
}

.highlight-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.highlight-card {
  padding: 16px;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef6ff 100%);
  border: 1px solid #dbeafe;
}

.highlight-label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.highlight-card strong {
  display: block;
  margin-top: 10px;
  font-size: 24px;
  color: #0f172a;
}

.scope-block {
  margin-top: 18px;
  padding: 16px;
  border-radius: 16px;
  background: #f8fafc;
}

.scope-title {
  display: block;
  margin-bottom: 10px;
  color: #475569;
  font-size: 13px;
}

.scope-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.activity-item {
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 14px 0;
  border-bottom: 1px solid #e2e8f0;
}

.activity-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.activity-badge {
  min-width: 74px;
  padding: 6px 10px;
  border-radius: 999px;
  font-size: 12px;
  text-align: center;
  color: #fff;
}

.activity-badge--warning {
  background: #f59e0b;
}

.activity-badge--success {
  background: #10b981;
}

.activity-badge--danger {
  background: #ef4444;
}

.activity-badge--primary {
  background: #3b82f6;
}

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.activity-title strong {
  color: #0f172a;
}

.activity-meta {
  display: flex;
  gap: 18px;
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  flex-wrap: wrap;
}

.empty-state {
  color: #94a3b8;
  font-size: 13px;
}

@media (max-width: 1200px) {
  .summary-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-panel {
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .highlight-grid {
    grid-template-columns: 1fr;
  }

  .profile-grid,
  .activity-title {
    grid-template-columns: 1fr;
  }
}
</style>
