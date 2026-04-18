package com.example.controller;

import com.example.entity.Role;
import com.example.entity.User;
import com.example.service.PermissionService;
import com.example.service.RoleService;
import com.example.service.TokenBlacklistService;
import com.example.service.UserService;
import com.example.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserService userService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    /**
     * 处理登录请求。
     * 登录成功后除了返回 token 和权限码，还会把角色详情、当前生效的数据范围一并返回给前端。
     * 这样前端进入系统后可以直接展示“当前列表为什么只看到这些数据”，减少调试和排查成本。
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User loginUser) {
        Map<String, Object> result = new HashMap<>();
        if (loginUser == null || loginUser.getUsername() == null || loginUser.getUsername().isBlank()
                || loginUser.getPassword() == null || loginUser.getPassword().isBlank()) {
            result.put("code", 400);
            result.put("msg", "用户名和密码不能为空");
            return result;
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginUser.getUsername(), loginUser.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            org.springframework.security.core.userdetails.User userDetails =
                    (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
            User dbUser = userService.findByUsername(userDetails.getUsername());
            if (dbUser == null) {
                result.put("code", 401);
                result.put("msg", "用户名或密码错误");
                return result;
            }

            String token = jwtUtils.generateToken(userDetails.getUsername(), dbUser.getId());
            List<Role> roleDetails = roleService.getRolesByUserId(dbUser.getId());
            String currentDataScope = roleService.resolveHighestDataScope(roleDetails);

            result.put("code", 200);
            result.put("msg", "success");
            result.put("token", token);
            result.put("user", dbUser);
            result.put("roles", userService.getUserRoles(dbUser.getId()));
            result.put("roleDetails", roleDetails);
            result.put("currentDataScope", currentDataScope);
            result.put("currentDataScopeLabel", Role.getDataScopeLabel(currentDataScope));
            result.put("permissions", userService.getUserPermissions(dbUser.getId()));
            result.put("menus", permissionService.getMenuPermissionsByUserId(dbUser.getId()));
            return result;
        } catch (BadCredentialsException | AuthenticationServiceException exception) {
            result.put("code", 401);
            result.put("msg", "用户名或密码错误");
            return result;
        } catch (DisabledException exception) {
            result.put("code", 403);
            result.put("msg", "账号已被禁用");
            return result;
        } catch (Exception exception) {
            result.put("code", 500);
            result.put("msg", "登录失败，请检查用户数据或密码配置");
            return result;
        }
    }

    /**
     * 退出登录时把当前 JWT 放进 Redis 黑名单。
     * 这样即使旧 token 还没过期，只要再次访问后端，也会在过滤器阶段被立即拦截。
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            if (!token.isEmpty() && jwtUtils.validateToken(token)) {
                tokenBlacklistService.blacklistToken(token);
            }
        }

        result.put("code", 200);
        result.put("msg", "success");
        return result;
    }

    /**
     * 获取当前登录用户信息。
     * 页面刷新后，前端通过这个接口恢复用户、角色、权限、菜单，以及当前真正生效的数据范围描述。
     */
    @GetMapping("/me")
    public Map<String, Object> me() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Map<String, Object> result = new HashMap<>();
        if (authentication == null || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            result.put("code", 401);
            result.put("msg", "Unauthorized");
            result.put("data", null);
            return result;
        }

        User user = userService.findByUsername(authentication.getName());
        List<Role> roleDetails = roleService.getRolesByUserId(user.getId());
        String currentDataScope = roleService.resolveHighestDataScope(roleDetails);

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("roles", userService.getUserRoles(user.getId()));
        data.put("roleDetails", roleDetails);
        data.put("currentDataScope", currentDataScope);
        data.put("currentDataScopeLabel", Role.getDataScopeLabel(currentDataScope));
        data.put("permissions", userService.getUserPermissions(user.getId()));
        data.put("menus", permissionService.getMenuPermissionsByUserId(user.getId()));

        result.put("code", 200);
        result.put("msg", "success");
        result.put("data", data);
        return result;
    }
}
