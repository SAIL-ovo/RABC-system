import { createStore } from 'vuex'
import request from '@/utils/request'
import { buildMenuByPermissions } from '@/router'

const cachedPermissions = JSON.parse(localStorage.getItem('permissions') || '[]')
const cachedMenu = JSON.parse(localStorage.getItem('menu') || '[]')
const cachedRoleDetails = JSON.parse(localStorage.getItem('roleDetails') || '[]')
const cachedDataScope = localStorage.getItem('currentDataScope') || ''
const cachedDataScopeLabel = localStorage.getItem('currentDataScopeLabel') || ''

/**
 * 把后端返回的权限菜单树拍平成前端菜单数组。
 * 当前布局只渲染一级菜单，所以这里优先取顶级菜单；后续要做多级菜单时可以继续扩展。
 */
const normalizeMenus = (menus = [], permissions = cachedPermissions) => {
  const localMenus = buildMenuByPermissions(permissions)
  const localMenuMap = new Map()

  localMenus.forEach((item) => {
    if (!localMenuMap.has(item.code)) {
      localMenuMap.set(item.code, item)
    }
  })

  if (!Array.isArray(menus) || !menus.length) {
    return localMenus
  }

  const backendMenuMap = new Map()

  menus.forEach((item) => {
    if (item?.code && !backendMenuMap.has(item.code)) {
      backendMenuMap.set(item.code, item)
    }
  })

  /**
   * 左侧菜单顺序以本地路由配置为准，后端菜单数据只负责补充标题、图标等展示信息。
   * 这样菜单展示顺序就能和我们在 router 里维护的页面顺序保持一致，
   * 不会再被数据库里的 sort 字段“反向覆盖”掉。
   */
  return localMenus.map((item) => {
    const backendMenu = backendMenuMap.get(item.code)
    return {
      path: item.path,
      title: backendMenu?.name || item.title,
      icon: backendMenu?.icon || item.icon || 'Menu',
      code: item.code
    }
  })
}

const normalizedCachedMenu = normalizeMenus(cachedMenu, cachedPermissions)

export default createStore({
  state: {
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null'),
    roleDetails: cachedRoleDetails,
    currentDataScope: cachedDataScope,
    currentDataScopeLabel: cachedDataScopeLabel,
    permissions: cachedPermissions,
    menu: normalizedCachedMenu.length ? normalizedCachedMenu : buildMenuByPermissions(cachedPermissions)
  },
  mutations: {
    setToken(state, token) {
      state.token = token
      localStorage.setItem('token', token)
    },
    setUser(state, user) {
      state.user = user
      localStorage.setItem('user', JSON.stringify(user))
    },
    setRoleDetails(state, roleDetails) {
      state.roleDetails = roleDetails || []
      localStorage.setItem('roleDetails', JSON.stringify(state.roleDetails))
    },
    setCurrentDataScope(state, currentDataScope) {
      state.currentDataScope = currentDataScope || ''
      localStorage.setItem('currentDataScope', state.currentDataScope)
    },
    setCurrentDataScopeLabel(state, currentDataScopeLabel) {
      state.currentDataScopeLabel = currentDataScopeLabel || ''
      localStorage.setItem('currentDataScopeLabel', state.currentDataScopeLabel)
    },
    setPermissions(state, permissions) {
      state.permissions = permissions || []
      localStorage.setItem('permissions', JSON.stringify(state.permissions))
    },
    setMenu(state, menu) {
      state.menu = menu || []
      localStorage.setItem('menu', JSON.stringify(state.menu))
    },
    logout(state) {
      state.token = ''
      state.user = null
      state.roleDetails = []
      state.currentDataScope = ''
      state.currentDataScopeLabel = ''
      state.permissions = []
      state.menu = []
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      localStorage.removeItem('roleDetails')
      localStorage.removeItem('currentDataScope')
      localStorage.removeItem('currentDataScopeLabel')
      localStorage.removeItem('permissions')
      localStorage.removeItem('menu')
    }
  },
  actions: {
    /**
     * 登录时同步缓存用户、角色、数据范围、权限和菜单。
     * 这样用户一进入系统，前端就能直接感知当前生效的数据范围，而不需要等二次请求再推断。
     */
    login({ commit }, payload) {
      const {
        token,
        user,
        roleDetails = [],
        currentDataScope = '',
        currentDataScopeLabel = '',
        permissions = [],
        menus = []
      } = payload || {}

      commit('setToken', token)
      commit('setUser', user)
      commit('setRoleDetails', roleDetails)
      commit('setCurrentDataScope', currentDataScope)
      commit('setCurrentDataScopeLabel', currentDataScopeLabel)
      commit('setPermissions', permissions)
      commit('setMenu', normalizeMenus(menus, permissions))
    },

    /**
     * 页面刷新后从后端恢复当前登录人的完整上下文。
     * 除了基础登录态，这里还会把角色详情和当前生效的数据范围描述重新同步回来。
     */
    async fetchCurrentUser({ commit, state }) {
      if (!state.token) {
        return null
      }

      const res = await request.get('/auth/me')
      commit('setUser', res.data.user)
      commit('setRoleDetails', res.data.roleDetails || [])
      commit('setCurrentDataScope', res.data.currentDataScope || '')
      commit('setCurrentDataScopeLabel', res.data.currentDataScopeLabel || '')
      commit('setPermissions', res.data.permissions || [])
      commit('setMenu', normalizeMenus(res.data.menus || [], res.data.permissions || []))
      return res.data
    },

    logout({ commit }) {
      commit('logout')
    }
  },
  getters: {
    isLoggedIn: (state) => !!state.token,
    currentUser: (state) => state.user,
    currentRoleDetails: (state) => state.roleDetails,
    currentDataScopeLabel: (state) => state.currentDataScopeLabel,
    menuList: (state) => state.menu,
    permissionSet: (state) => new Set(state.permissions || []),
    hasPermission: (state, getters) => (permission) => {
      if (!permission) {
        return true
      }
      return getters.permissionSet.has(permission)
    }
  }
})
