package cn.hamm.airpower.security;

import cn.hamm.airpower.config.GlobalConfig;
import cn.hamm.airpower.result.Result;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

/**
 * <h1>全局权限拦截器抽象类</h1>
 *
 * @author Hamm
 */
public abstract class AbstractAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NotNull HttpServletRequest httpServletRequest, @NotNull HttpServletResponse httpServletResponse, @NotNull Object object) {
        // 设置允许跨域
        if (setCrossOriginHeaders(httpServletRequest, httpServletResponse)) {
            return false;
        }

        HandlerMethod handlerMethod = (HandlerMethod) object;

        //取出控制器和方法
        Class<?> clazz = handlerMethod.getBean().getClass();
        Method method = handlerMethod.getMethod();
        AccessConfig accessConfig = AccessUtil.getWhatNeedAccess(clazz, method);
        if (!accessConfig.login) {
            // 不需要登录 直接返回有权限
            return true;
        }
        //需要登录
        String accessToken = httpServletRequest.getHeader(GlobalConfig.authorizeHeader);
        Result.UNAUTHORIZED.whenEmpty(accessToken);
        Long userId = JwtUtil.getUserId(accessToken);
        JwtUtil.verify(getUserPassword(userId), accessToken);
        //需要RBAC
        if (accessConfig.authorize) {
            //验证用户是否有接口的访问权限
            return checkAccess(userId, handlerMethod);
        }
        return true;
    }

    /**
     * <h2>获取用户的密码</h2>
     *
     * @param userId 用户ID
     * @return 密码
     */
    public abstract String getUserPassword(Long userId);

    /**
     * <h2>验证指定的用户是否有指定权限标识的权限</h2>
     *
     * @param userId             用户ID
     * @param permissionIdentity 权限标识
     * @return 验证结果
     */
    public abstract boolean checkAccess(Long userId, String permissionIdentity);


    /**
     * <h2>验证指定用户是否有指定方法的访问权限</h2>
     *
     * @param userId 用户ID
     * @param method 方法
     * @return 是否有权限
     */
    private boolean checkAccess(Long userId, HandlerMethod method) {
        String permissionIdentity = method.getBeanType().getSimpleName().replaceAll("Controller", "").toLowerCase() + "_" + method.getMethod().getName();
        return checkAccess(userId, permissionIdentity);
    }


    /**
     * <h2>设置允许跨域的头</h2>
     *
     * @param httpServletResponse response对象
     */
    private boolean setCrossOriginHeaders(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        httpServletResponse.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        httpServletResponse.setHeader("Access-Control-Max-Age", "3600");
        httpServletResponse.setHeader("Access-Control-Allow-Credentials", "true");
        httpServletResponse.setHeader("Access-Control-Allow-Headers", "*");
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");


        if (HttpMethod.OPTIONS.name().equals(httpServletRequest.getMethod())) {
            httpServletResponse.setStatus(HttpStatus.OK.value());
            try {
                PrintWriter writer = httpServletResponse.getWriter();
                writer.println("Hello World");
                writer.flush();
                writer.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
