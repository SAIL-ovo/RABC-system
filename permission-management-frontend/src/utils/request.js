import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 5000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

let isHandlingUnauthorized = false

/**
 * 统一清理前端登录态缓存。
 * 这里单独抽成公共方法，方便请求拦截器、路由守卫和后续登录恢复逻辑共用同一套清理动作，
 * 避免出现“token 清掉了，但菜单和用户信息还残留”的不一致状态。
 */
export const clearAuthCache = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  localStorage.removeItem('roleDetails')
  localStorage.removeItem('currentDataScope')
  localStorage.removeItem('currentDataScopeLabel')
  localStorage.removeItem('permissions')
  localStorage.removeItem('menu')
}

/**
 * 判断 JWT 是否已经过期或结构不合法。
 * 路由守卫只看“本地有没有 token”是不够的，浏览器隔一段时间再打开页面时，
 * 很可能仍然带着一个早已失效的旧 token，导致前端误判为“已登录”并直接跳回首页。
 */
export const isTokenExpired = (token) => {
  if (!token) {
    return true
  }

  try {
    const payload = token.split('.')[1]
    if (!payload) {
      return true
    }

    const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/')
    const decodedPayload = JSON.parse(window.atob(normalizedPayload))
    if (!decodedPayload.exp) {
      return false
    }

    return decodedPayload.exp * 1000 <= Date.now()
  } catch (_error) {
    return true
  }
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.msg || res.message || '请求失败')
      return Promise.reject(res)
    }
    return res
  },
  (error) => {
    const status = error.response?.status

    if (status === 401) {
      // 这里不要再直接引用 store。
      // request 是很多基础模块都会依赖的底层请求实例，一旦再反向 import store，
      // 很容易和 store -> request -> router 形成循环依赖，导致登录页白屏。
      clearAuthCache()

      if (!isHandlingUnauthorized) {
        isHandlingUnauthorized = true
        ElMessageBox.confirm('登录状态已失效，请重新登录', '提示', {
          confirmButtonText: '确定',
          type: 'warning'
        }).then(() => {
          router.push('/login')
        }).catch(() => {
          router.push('/login')
        }).finally(() => {
          isHandlingUnauthorized = false
        })
      }
    } else if (status === 403) {
      ElMessage.error('没有访问权限')
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '服务器异常')
    }

    return Promise.reject(error)
  }
)

export default request
