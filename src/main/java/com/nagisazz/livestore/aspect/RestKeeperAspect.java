package com.nagisazz.livestore.aspect;

import com.nagisazz.livestore.annotation.RestKeeper;
import com.nagisazz.livestore.constants.Constants;
import com.nagisazz.livestore.util.RestUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class RestKeeperAspect {

    @Pointcut("@annotation(restKeeper)")
    public void restKeeperPoint(RestKeeper restKeeper) {

    }

    @Around("restKeeperPoint(restKeeper)")
    public Object logAspect(ProceedingJoinPoint joinPoint, RestKeeper restKeeper) throws Throwable {
        Method method = getMethod(joinPoint);
        log.info("调用接口：{}，调用方IP：{}", method.getName(), RestUtil.getIp());
        if (RestUtil.getToken() == null || !RestUtil.getToken().equals(Constants.TOKEN) ||
                (System.currentTimeMillis() - Long.valueOf(Constants.TOKEN)) / 1000 / 60 > 5) {
            log.error("TOKEN失效，接口调用失败");
            return "TOKEN失效";
        }
        return joinPoint.proceed();
    }

    private Method getMethod(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = methodSignature.getMethod();
        return joinPoint.getTarget().getClass().getMethod(targetMethod.getName(), targetMethod.getParameterTypes());
    }
}
