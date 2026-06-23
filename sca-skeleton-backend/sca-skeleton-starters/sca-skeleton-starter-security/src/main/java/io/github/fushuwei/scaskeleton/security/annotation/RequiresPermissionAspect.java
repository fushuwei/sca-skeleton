package io.github.fushuwei.scaskeleton.security.annotation;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 方法级权限校验切面
 * <p>
 * 拦截所有标注了 {@link RequiresPermission} 的方法（方法级）或其所在类（类级），在方法执行前委托
 * {@link RequiresPermissionChecker} 完成权限匹配；权限不足时由校验器抛出
 * {@link io.github.fushuwei.scaskeleton.core.exception.ForbiddenException}，交由
 * {@code GlobalExceptionHandler} 统一处理，返回标准 403 响应。
 * <p>
 * 合并切点 {@code @annotation || @within} 同时覆盖方法级与类级标注，并通过
 * {@link #resolveAnnotation(ProceedingJoinPoint)} 做一次性合并解析：
 * <ul>
 *   <li>方法上直接标注 {@link RequiresPermission} 时，方法级优先；</li>
 *   <li>方法上未标注但所在类标注时，使用类级注解；</li>
 *   <li>两者均无（不应进入本切面）时直接放行。</li>
 * </ul>
 * 如此保证即使方法与类同时标注，权限也只校验一次，避免重复拦截。
 *
 * @author Fu Wei
 */
@Aspect
@RequiredArgsConstructor
public class RequiresPermissionAspect {

    // 权限校验委托器，由 OAuth2ResourceServerAutoConfiguration 注册为 Bean
    private final RequiresPermissionChecker requiresPermissionChecker;

    /**
     * 权限校验环绕通知
     * <p>
     * 切点覆盖方法级与类级 {@link RequiresPermission} 标注；实际生效的注解由
     * {@link #resolveAnnotation(ProceedingJoinPoint)} 统一解析，方法级优先于类级。
     *
     * @param joinPoint 切入点
     * @return 原方法返回值
     */
    @Around("@annotation(io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission)"
        + " || @within(io.github.fushuwei.scaskeleton.security.annotation.RequiresPermission)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        RequiresPermission annotation = resolveAnnotation(joinPoint);
        if (annotation != null) {
            // 权限不足时 check 内部会抛出 AccessDeniedException，此处不吞异常
            requiresPermissionChecker.check(annotation);
        }
        return joinPoint.proceed();
    }

    /**
     * 解析实际生效的 {@link RequiresPermission} 注解：方法级优先，回退到类级
     *
     * @param joinPoint 切入点
     * @return 生效的注解实例，方法与类均未标注时返回 null
     */
    private RequiresPermission resolveAnnotation(ProceedingJoinPoint joinPoint) {
        // 优先取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequiresPermission methodAnnotation = method.getAnnotation(RequiresPermission.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        // 方法上未标注时回退到声明类上的注解
        Class<?> targetClass = joinPoint.getTarget().getClass();
        return targetClass.getAnnotation(RequiresPermission.class);
    }
}
