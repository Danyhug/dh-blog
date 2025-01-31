package top.zzf4.blog.aop;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.zzf4.blog.config.BanInterceptor;
import top.zzf4.blog.constant.RedisConstant;
import top.zzf4.blog.entity.vo.SendResponseData;
import top.zzf4.blog.utils.RedisCacheUtils;
import top.zzf4.blog.utils.Tools;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class LimitAop {

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Around("@annotation(limit)")
    public Object rateLimiter(ProceedingJoinPoint joinPoint, Limit limit) throws Throwable {
        // 获取当前请求的 HttpServletRequest 对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 获取客户端 IP 地址
        String clientIp = Tools.getClientIp(request);
        // 打印或处理客户端 IP 地址
        // System.out.println("Client IP: " + clientIp);

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String methodName = methodSignature.getName();

        String key = RedisConstant.CACHE_IP + methodName + ":" + clientIp;
        Object o = redisCacheUtils.get(key);
        // 不存在ip的情况
        if (o == null) {
            // 缓存客户端 IP 地址
            // 每10秒钟限制访问接口10次
            redisCacheUtils.set(key, 1, limit.time(), TimeUnit.SECONDS);
        } else {
            // 获取当前 IP 地址的访问次数
            Integer count = (Integer) o;
            // 判断是否超过限制次数
            if (count >= limit.num()) {
                // 说明超过次数，抛出异常
                HttpServletResponse response = attributes.getResponse();
                Tools.sendResponse(new SendResponseData(403, "访问次数过多，已被封禁", response));
                return null;
            } else {
                // 累加访问次数
                redisCacheUtils.incr(key);
            }
        }

        // 继续执行目标方法
        return joinPoint.proceed();
    }
}
