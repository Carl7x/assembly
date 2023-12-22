package com.kyle.dynamic;

import com.kyle.annotation.DBRouterStrategy;
import com.kyle.DBContextHolder;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 处理Mybatis 拦截器interceptor，通过对 SQL 语句的拦截处理，修改分表信息
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DynamicMybatisPlugin implements Interceptor {
    private Pattern pattern = Pattern.compile("(from|into|update)[\\s]{1,}(\\w{1,})", Pattern.CASE_INSENSITIVE);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = MetaObject.forObject(statementHandler, SystemMetaObject.DEFAULT_OBJECT_FACTORY, SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY, new DefaultReflectorFactory());
        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");

        //MappedStatement 的 ID 是一个在 Mapper XML 文件中定义的唯一标识符，通常由 namespace 和 statement ID 组成
        String id = mappedStatement.getId();
        //获取namespace
        String className = id.substring(0, id.lastIndexOf("."));
        Class<?> aClass = Class.forName(className);
        DBRouterStrategy dbRouterStrategy = aClass.getAnnotation(DBRouterStrategy.class);
        if (dbRouterStrategy == null || !dbRouterStrategy.spiltTable()){
            return invocation.proceed();
        }

        //获取SQL
        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = boundSql.getSql();

        Matcher matcher = pattern.matcher(sql);
        String tableName = null;
        if(matcher.find()){
            tableName = matcher.group().trim();
        }
        assert tableName!= null;

        assert tableName!=null;
        //选择符合的语句进行替换
        String replaceSql = matcher.replaceAll(tableName + "_" + DBContextHolder.getTBKey());

        // 通过反射修改SQL语句
        Field field = boundSql.getClass().getDeclaredField("sql");
        field.setAccessible(true);
        field.set(boundSql, replaceSql);
        field.setAccessible(false);

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Interceptor.super.plugin(target);
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}
