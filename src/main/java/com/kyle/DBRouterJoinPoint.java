package com.kyle;

import com.kyle.annotation.DBRouter;
import com.kyle.strategy.IDBRouterStrategy;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.apache.commons.beanutils.BeanUtils;


@Slf4j
@Aspect
public class DBRouterJoinPoint {

    private DBRouterConfig dbRouterConfig;

    private IDBRouterStrategy dbRouterStrategy;

    public DBRouterJoinPoint(DBRouterConfig dbRouterConfig, IDBRouterStrategy dbRouterStrategy) {
        this.dbRouterConfig = dbRouterConfig;
        this.dbRouterStrategy = dbRouterStrategy;
    }

    //切点，在这个注解进行aop
    @Pointcut("@annotation(com.kyle.annotation.DBRouter)")
    public void aopPoint() {
    }

    /**
     * 所有需要分库分表的操作，都需要使用自定义注解进行拦截，拦截后读取方法中的入参字段，根据字段进行路由操作。
     * 1. dbRouter.key() 确定根据哪个字段进行路由
     * 2. getAttrValue 根据数据库路由字段，从入参中读取出对应的值。比如路由 key 是 uId，那么就从入参对象 Obj 中获取到 uId 的值。
     * 3. dbRouterStrategy.doRouter(dbKeyAttr) 路由策略根据具体的路由值进行处理
     * 4. 路由处理完成后放行。 jp.proceed();
     * 5. 最后 dbRouterStrategy 需要执行 clear 因为这里用到了 ThreadLocal 需要手动清空。关于 ThreadLocal 内存泄漏介绍 https://t.zsxq.com/027QF2fae
     */
    @Around("aopPoint() && @annotation(dbRouter)")
    public Object doRouter(ProceedingJoinPoint joinPoint, DBRouter dbRouter) throws Throwable {
        String dbKey = dbRouter.key();
        if (StringUtils.isBlank(dbKey) && StringUtils.isBlank(dbRouterConfig.getRouterKey())) {
            throw new RuntimeException("annotation DBRouter key is null！");
        }
        dbKey = StringUtils.isBlank(dbKey) ? dbKey : dbRouterConfig.getRouterKey();
        // 获取方法参数  joinPoint.getArgs()
        //这一步是为了获取路由属性的值，以供哈希计算出idx
        String attrValue = getAttrValue(dbKey, joinPoint.getArgs());
        // 路由策略
        dbRouterStrategy.doRouter(attrValue);
        // 返回结果
        try {
            return joinPoint.proceed();
        } finally {
            dbRouterStrategy.clear();
        }
    }

    public String getAttrValue(String attr, Object[] args) {
        if (args.length == 1) {
            Object arg = args[0];
            if (arg instanceof String) {
                return arg.toString();
            }
        }
        String filedValue = null;
        for (Object arg : args) {
            //先进行赋值操作 然后再判断当前参数是否为空 如果不为空则返回当前参数
            try {
                filedValue = BeanUtils.getProperty(arg, attr);
                //String filedValue = (String) arg.getClass().getDeclaredField(attr).get(arg);
                if (StringUtils.isBlank(filedValue)) {
                    break;
                }
            } catch (Exception e) {
                log.error("获取路由属性值失败 attr：{}", attr, e);
            }
        }
        return filedValue;
    }
}
