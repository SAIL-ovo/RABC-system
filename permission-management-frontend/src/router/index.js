import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Layout from '../components/Layout.vue'
import { clearAuthCache, isTokenExpired } from '@/utils/request'

export const homeChildren = [
  {
    path: 'dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue'),
    meta: {
      requiresAuth: true,
      title: '首页概览',
      icon: 'DataBoard'
    }
  },
  {
    path: 'user',
    name: 'User',
    component: () => import('../views/User.vue'),
    meta: {
      requiresAuth: true,
      permission: 'user:manage',
      access: ['user:manage', 'user:view', 'user:create', 'user:update', 'user:delete', 'user:assign-role', 'user:reset-password', 'user:status'],
      title: '用户管理',
      icon: 'User'
    }
  },
  {
    path: 'role',
    name: 'Role',
    component: () => import('../views/Role.vue'),
    meta: {
      requiresAuth: true,
      permission: 'role:manage',
      access: ['role:manage', 'role:view', 'role:create', 'role:update', 'role:delete', 'role:assign-permission', 'role:status'],
      title: '角色管理',
      icon: 'Operation'
    }
  },
  {
    path: 'permission',
    name: 'Permission',
    component: () => import('../views/Permission.vue'),
    meta: {
      requiresAuth: true,
      permission: 'permission:manage',
      access: ['permission:manage', 'permission:view', 'permission:create', 'permission:update', 'permission:delete'],
      title: '权限管理',
      icon: 'Lock'
    }
  },
  {
    path: 'department',
    name: 'Department',
    component: () => import('../views/Department.vue'),
    meta: {
      requiresAuth: true,
      permission: 'department:manage',
      access: ['department:manage', 'department:view', 'department:create', 'department:update', 'department:delete'],
      title: '部门管理',
      icon: 'OfficeBuilding'
    }
  },
  {
    path: 'post',
    name: 'Post',
    component: () => import('../views/Post.vue'),
    meta: {
      requiresAuth: true,
      permission: 'post:manage',
      access: ['post:manage', 'post:view', 'post:create', 'post:update', 'post:delete'],
      title: '岗位管理',
      icon: 'Briefcase'
    }
  },
  {
    path: 'operation-log',
    name: 'OperationLog',
    component: () => import('../views/OperationLog.vue'),
    meta: {
      requiresAuth: true,
      permission: 'log:view',
      access: ['log:view', 'user:manage'],
      title: '操作日志',
      icon: 'Document'
    }
  }
]

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: {
      requiresAuth: false
    }
  },
  {
    path: '/home',
    name: 'Home',
    component: Layout,
    meta: {
      requiresAuth: true
    },
    children: homeChildren
  }
]

/**
 * 判断当前权限集合是否满足某个路由的访问条件。
 * `meta.permission` 保留旧写法，`meta.access` 用来支持页面级多权限兜底。
 */
const hasRouteAccess = (routeMeta = {}, permissions = []) => {
  const requiredPermissions = routeMeta.access || (routeMeta.permission ? [routeMeta.permission] : [])
  if (!requiredPermissions.length) {
    return true
  }
  return requiredPermissions.some((permission) => permissions.includes(permission))
}

/**
 * 根据权限列表生成左侧菜单。
 * 菜单依然从路由元信息推导，避免菜单和路由拆成两份配置分别维护。
 */
export const buildMenuByPermissions = (permissions = []) => {
  return homeChildren
    .filter((route) => hasRouteAccess(route.meta, permissions))
    .map((route) => ({
      path: `/home/${route.path}`,
      title: route.meta?.title || route.name,
      icon: route.meta?.icon || 'Menu',
      code: route.meta?.permission || route.name
    }))
}

/**
 * 找到当前权限下第一个可访问页面。
 * 登录后的默认跳转和权限不足时的回退都统一走这里。
 */
export const getFirstAuthorizedRoute = (permissions = []) => {
  const firstMenu = buildMenuByPermissions(permissions)[0]
  return firstMenu?.path || '/login'
}

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const permissions = JSON.parse(localStorage.getItem('permissions') || '[]')
  const hasValidToken = token && !isTokenExpired(token)

  /**
   * 浏览器保留的旧 token 可能只是“还在 localStorage 里”，并不代表它依然有效。
   * 进入路由前先做一次轻量级本地过期判断，能避免用户被错误地重定向回首页，
   * 然后首页第一个接口再因为 401 把人踢出来的割裂体验。
   */
  if (token && !hasValidToken) {
    clearAuthCache()
  }

  if (to.meta.requiresAuth && !hasValidToken) {
    next('/login')
    return
  }

  if (to.path === '/login' && hasValidToken) {
    next(getFirstAuthorizedRoute(permissions))
    return
  }

  if (!hasRouteAccess(to.meta, permissions)) {
    next(getFirstAuthorizedRoute(permissions))
    return
  }

  next()
})

export default router
