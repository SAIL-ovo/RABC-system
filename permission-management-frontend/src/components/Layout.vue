<template>
  <div class="layout-container">
    <el-container class="layout">
      <el-aside width="200px" class="layout-aside">
        <div class="logo">
          <h2>权限管理系统</h2>
        </div>
        <el-menu :default-active="activeMenu" class="layout-menu" router>
          <el-menu-item v-for="item in menuList" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="layout-header">
          <div class="header-right">
            <el-dropdown>
              <span class="user-info">
                <el-avatar :size="32">{{ user?.realName?.charAt(0) || 'U' }}</el-avatar>
                <span>{{ user?.realName || '用户' }}</span>
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="handleOpenPasswordDialog">
                    <el-icon><Lock /></el-icon>
                    <span>修改密码</span>
                  </el-dropdown-item>
                  <el-dropdown-item @click="handleLogout">
                    <el-icon><SwitchButton /></el-icon>
                    <span>退出登录</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="layout-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="460px" class="manage-dialog">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
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
        <el-button type="primary" @click="handleChangePassword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ArrowDown, Lock, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getFirstAuthorizedRoute } from '@/router'
import request from '@/utils/request'

// 路由实例，负责跳转登录页和默认首页。
const router = useRouter()

// 当前路由信息，用于高亮菜单和判断是否停留在 /home。
const route = useRoute()

// 全局状态仓库，用户信息、权限和菜单都从这里读取。
const store = useStore()

// 当前登录用户信息，供右上角头像和姓名展示使用。
const user = computed(() => store.state.user)

// 左侧菜单直接来自 store 中缓存的菜单配置。
const menuList = computed(() => store.getters.menuList)

// 当前激活的菜单路径，跟随路由变化自动更新。
const activeMenu = computed(() => route.path)

// 修改密码弹窗显隐和表单实例。
const passwordDialogVisible = ref(false)
const passwordFormRef = ref(null)

// 当前登录用户修改密码表单。
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 修改密码表单校验规则。
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (!value) {
          callback(new Error('请再次输入新密码'))
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

// 退出时同时清空本地登录态，并跳回登录页。
const handleLogout = () => {
  /*
   * 退出时先调用后端 logout，让服务端把当前 token 拉入 Redis 黑名单，
   * 然后再清理本地登录态。这样即使旧 token 被重放，也会马上被后端拒绝。
   */
  request.post('/auth/logout').catch(() => null).finally(() => {
    store.dispatch('logout')
    router.push('/login')
  })
}

// 打开修改密码弹窗前，先清空上一次输入内容。
const handleOpenPasswordDialog = () => {
  Object.assign(passwordForm, {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  })
  passwordDialogVisible.value = true
}

// 提交当前登录用户的改密请求。
// 修改成功后主动退出登录，确保后续使用新密码重新建立会话。
const handleChangePassword = async () => {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  await request.put('/user/change-password', {
    oldPassword: passwordForm.oldPassword,
    newPassword: passwordForm.newPassword
  })

  ElMessage.success('密码修改成功，请重新登录')
  passwordDialogVisible.value = false
  handleLogout()
}

// 根据当前权限计算默认首页。
// 菜单渲染和默认跳转共用同一套规则，后续扩展菜单时不容易出现分叉。
const resolveDefaultRoute = () => {
  return getFirstAuthorizedRoute(store.state.permissions)
}

// 页面初始化时恢复用户上下文。
// 如果用户直接访问 /home，则自动落到第一个有权限的页面。
onMounted(async () => {
  if (!localStorage.getItem('token')) {
    router.push('/login')
    return
  }

  try {
    // 每次进入布局页都主动刷新一次当前用户上下文，
    // 这样可以顺手把旧版本缓存下来的错误菜单路径修正掉。
    await store.dispatch('fetchCurrentUser')
  } catch (error) {
    store.dispatch('logout')
    router.push('/login')
    return
  }

  if (route.path === '/home' || route.path === '/home/') {
    router.replace(resolveDefaultRoute())
  }
})
</script>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.layout {
  height: 100%;
}

.layout-aside {
  background-color: #001529;
  color: #fff;
  padding-top: 20px;
  height: 100%;
}

.logo {
  text-align: center;
  margin-bottom: 30px;
}

.logo h2 {
  color: #fff;
  font-size: 18px;
  margin: 0;
}

.layout-menu {
  border-right: none;
  background-color: #001529;
}

.layout-menu :deep(.el-menu-item) {
  color: #ffffff !important;
}

.layout-menu :deep(.el-menu-item:hover),
.layout-menu :deep(.el-menu-item.is-active) {
  background-color: rgba(255, 255, 255, 0.1) !important;
  color: #ffffff !important;
}

.layout-menu :deep(.el-icon) {
  color: #ffffff !important;
}

.layout-header {
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 20px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.user-info span {
  margin-left: 10px;
}

.layout-main {
  padding: 20px;
  background-color: #f5f7fa;
  overflow-y: auto;
}
</style>
