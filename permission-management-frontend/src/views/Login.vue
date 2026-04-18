<template>
  <div class="login-shell">
    <div class="background-orbit orbit-a"></div>
    <div class="background-orbit orbit-b"></div>
    <div class="grain-mask"></div>

    <section class="login-stage">
      <aside class="brand-panel">
        <div class="brand-copy">
          <p class="brand-kicker">RABC ACCESS CONTROL</p>
          <h1>让权限边界清晰、可查、可控。</h1>
          <p class="brand-desc">
            面向后台管理场景的统一权限中台，覆盖用户、角色、权限和菜单链路，帮助团队把授权过程从“靠约定”变成“可治理”。
          </p>
        </div>

        <div class="status-board">
          <div class="status-card">
            <span class="status-label">管理对象</span>
            <strong>用户 · 角色 · 权限</strong>
            <p>统一入口维护访问主体与授权关系。</p>
          </div>
          <div class="status-card">
            <span class="status-label">控制方式</span>
            <strong>菜单 + 接口双层校验</strong>
            <p>前后端权限联动，减少展示与接口脱节。</p>
          </div>
          <div class="status-card accent">
            <span class="status-label">当前目标</span>
            <strong>稳定、清晰、可审计</strong>
            <p>从演示型后台逐步推进到可交付系统。</p>
          </div>
        </div>
      </aside>

      <section class="login-panel">
        <div class="panel-frame">
          <div class="panel-topline"></div>
          <div class="panel-head">
            <p class="panel-tag">SIGN IN</p>
            <h2>登录权限管理系统</h2>
            <p class="panel-subtitle">请输入账号密码，进入控制台继续管理授权规则。</p>
          </div>

          <el-form ref="formRef" :model="loginForm" :rules="loginRules" class="login-form">
            <el-form-item prop="username" class="form-row">
              <label class="field-label">用户名</label>
              <el-input v-model="loginForm.username" placeholder="请输入用户名" class="editorial-input" @keyup.enter="handleLogin">
                <template #prefix>
                  <span class="input-prefix">ID</span>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password" class="form-row">
              <label class="field-label">密码</label>
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                class="editorial-input"
                show-password
                @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <span class="input-prefix">KEY</span>
                </template>
              </el-input>
            </el-form-item>

            <div class="panel-footnote">
              <span class="signal-dot"></span>
              <p>登录后会同步恢复当前用户、权限码与菜单上下文。</p>
            </div>

            <el-button type="primary" class="login-btn" @click="handleLogin" :loading="loading">
              进入控制台
            </el-button>
          </el-form>
        </div>
      </section>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getFirstAuthorizedRoute } from '@/router'

// 路由实例，登录成功后负责跳转到第一个有权限的页面。
const router = useRouter()

// 全局状态仓库，用于缓存 token、用户信息、权限和菜单。
const store = useStore()

// 登录按钮的加载态，防止重复提交。
const loading = ref(false)

// 表单实例，用于触发表单校验。
const formRef = ref()

// 登录表单数据。
const loginForm = reactive({
  username: '',
  password: ''
})

// 登录页校验规则。
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 登录成功后同时缓存用户、权限和菜单信息。
// 这样进入系统后，左侧菜单就能直接按后端配置展示，不需要前端再次推导。
const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  loading.value = true
  try {
    const res = await request.post('/auth/login', loginForm)
    store.dispatch('login', {
      token: res.token,
      user: res.user,
      roleDetails: res.roleDetails || [],
      currentDataScope: res.currentDataScope || '',
      currentDataScopeLabel: res.currentDataScopeLabel || '',
      permissions: res.permissions || [],
      menus: res.menus || []
    })
    ElMessage.success('登录成功')
    router.push(getFirstAuthorizedRoute(res.permissions || []))
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
:global(body) {
  margin: 0;
}

.login-shell {
  --bg: #f4efe7;
  --ink: #16202b;
  --muted: #617082;
  --line: rgba(22, 32, 43, 0.12);
  --panel: rgba(255, 250, 244, 0.84);
  --accent: #d16f3d;
  --accent-deep: #a64d24;
  --shadow: 0 30px 80px rgba(36, 41, 47, 0.18);
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(209, 111, 61, 0.08), transparent 38%),
    linear-gradient(180deg, #f7f1e8 0%, #ece5da 100%);
}

.background-orbit {
  position: absolute;
  border-radius: 999px;
  filter: blur(8px);
  opacity: 0.55;
  animation: drift 16s ease-in-out infinite;
}

.orbit-a {
  top: -120px;
  right: -80px;
  width: 420px;
  height: 420px;
  background: radial-gradient(circle, rgba(209, 111, 61, 0.28) 0%, rgba(209, 111, 61, 0) 72%);
}

.orbit-b {
  left: -160px;
  bottom: -140px;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(25, 50, 78, 0.18) 0%, rgba(25, 50, 78, 0) 70%);
  animation-delay: -5s;
}

.grain-mask {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1px),
    linear-gradient(90deg, rgba(22, 32, 43, 0.04) 1px, transparent 1px);
  background-size: 24px 24px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.82), rgba(0, 0, 0, 0.35));
  pointer-events: none;
}

.login-stage {
  position: relative;
  z-index: 1;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.2fr 0.9fr;
  gap: 32px;
  align-items: center;
  padding: 40px clamp(20px, 4vw, 56px);
}

.brand-panel {
  display: flex;
  flex-direction: column;
  gap: 40px;
  padding-right: 24px;
  animation: rise-in 0.9s ease-out both;
}

.brand-kicker,
.panel-tag,
.status-label {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
}

.brand-kicker {
  color: rgba(22, 32, 43, 0.58);
}

.brand-copy h1 {
  margin: 18px 0 16px;
  max-width: 700px;
  color: var(--ink);
  font-size: clamp(42px, 6vw, 78px);
  line-height: 0.96;
  font-weight: 700;
  letter-spacing: -0.05em;
  font-family: "Noto Serif SC", "Songti SC", "STSong", serif;
}

.brand-desc {
  max-width: 620px;
  margin: 0;
  color: var(--muted);
  font-size: 17px;
  line-height: 1.85;
}

.status-board {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.status-card {
  padding: 22px 20px;
  border: 1px solid var(--line);
  border-radius: 24px;
  background: rgba(255, 253, 249, 0.66);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(10px);
}

.status-card strong {
  display: block;
  margin-top: 12px;
  color: var(--ink);
  font-size: 22px;
  line-height: 1.25;
  font-family: "Noto Serif SC", "Songti SC", "STSong", serif;
}

.status-card p {
  margin: 12px 0 0;
  color: var(--muted);
  line-height: 1.7;
  font-size: 14px;
}

.status-card.accent {
  background: linear-gradient(180deg, rgba(209, 111, 61, 0.12), rgba(255, 252, 247, 0.82));
  border-color: rgba(209, 111, 61, 0.22);
}

.login-panel {
  display: flex;
  justify-content: center;
  animation: rise-in 0.9s ease-out 0.1s both;
}

.panel-frame {
  width: min(460px, 100%);
  padding: 26px;
  border: 1px solid rgba(22, 32, 43, 0.1);
  border-radius: 30px;
  background: var(--panel);
  box-shadow: var(--shadow);
  backdrop-filter: blur(18px);
}

.panel-topline {
  width: 88px;
  height: 4px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent), rgba(209, 111, 61, 0.12));
}

.panel-head {
  margin-top: 22px;
}

.panel-tag {
  color: rgba(22, 32, 43, 0.56);
}

.panel-head h2 {
  margin: 16px 0 10px;
  color: var(--ink);
  font-size: 34px;
  line-height: 1.08;
  font-family: "Noto Serif SC", "Songti SC", "STSong", serif;
}

.panel-subtitle {
  margin: 0;
  color: var(--muted);
  line-height: 1.75;
}

.login-form {
  margin-top: 28px;
}

.form-row {
  margin-bottom: 22px;
}

.field-label {
  display: block;
  margin-bottom: 10px;
  color: var(--ink);
  font-size: 13px;
  letter-spacing: 0.08em;
}

.input-prefix {
  color: rgba(22, 32, 43, 0.46);
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.panel-footnote {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 10px 0 22px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.6;
}

.panel-footnote p {
  margin: 0;
}

.signal-dot {
  width: 10px;
  height: 10px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: radial-gradient(circle, #ef8c55 0%, #d16f3d 62%, rgba(209, 111, 61, 0.22) 100%);
  box-shadow: 0 0 0 7px rgba(209, 111, 61, 0.1);
}

.login-btn {
  width: 100%;
  height: 52px;
  border: none;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--accent) 0%, var(--accent-deep) 100%);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.08em;
  box-shadow: 0 18px 36px rgba(166, 77, 36, 0.28);
}

.login-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 22px 42px rgba(166, 77, 36, 0.32);
}

.login-shell :deep(.editorial-input .el-input__wrapper) {
  min-height: 50px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: inset 0 0 0 1px rgba(22, 32, 43, 0.08);
}

.login-shell :deep(.editorial-input .el-input__wrapper.is-focus) {
  box-shadow:
    inset 0 0 0 1px rgba(209, 111, 61, 0.5),
    0 0 0 4px rgba(209, 111, 61, 0.1);
}

.login-shell :deep(.editorial-input .el-input__inner) {
  color: var(--ink);
}

.login-shell :deep(.el-form-item__error) {
  padding-top: 6px;
}

@keyframes drift {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(0, 22px, 0) scale(1.04);
  }
}

@keyframes rise-in {
  from {
    opacity: 0;
    transform: translateY(24px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1080px) {
  .login-stage {
    grid-template-columns: 1fr;
    gap: 28px;
  }

  .brand-panel {
    padding-right: 0;
  }

  .status-board {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .login-stage {
    padding: 18px;
  }

  .panel-frame {
    padding: 22px 18px;
    border-radius: 24px;
  }

  .brand-copy h1 {
    font-size: 40px;
  }

  .brand-desc {
    font-size: 15px;
  }
}
</style>
