package com.zmyc.interceptor;

import com.zmyc.common.context.UserContext;
import com.zmyc.infrastructure.entity.BaseDO;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

import java.util.Map;

/**
 * MyBatis拦截器
 * 自动设置创建时间、更新时间、创建人、更新人字段
 */
@Intercepts({
    @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
})
public class AuditInterceptor implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        
        if (parameter != null) {
            if (SqlCommandType.INSERT.equals(sqlCommandType)) {
                handleInsert(parameter);
            } else if (SqlCommandType.UPDATE.equals(sqlCommandType)) {
                handleUpdate(parameter);
            }
        }
        
        return invocation.proceed();
    }
    
    private void handleInsert(Object parameter) {
        if (parameter instanceof BaseDO entity) {
            Long currentTime = System.currentTimeMillis() / 1000; // 10位时间戳
            Long currentUserId = UserContext.getCurrentUserId() == null ? 0L : UserContext.getCurrentUserId();
            
            if (entity.getCreatedDate() == null) {
                entity.setCreatedDate(currentTime);
            }
            if (entity.getLastUpdatedDate() == null) {
                entity.setLastUpdatedDate(currentTime);
            }
            if (entity.getCreatedBy() == null && currentUserId != null) {
                entity.setCreatedBy(currentUserId);
            }
            if (entity.getLastUpdatedBy() == null && currentUserId != null) {
                entity.setLastUpdatedBy(currentUserId);
            }
        } else if (parameter instanceof Map) {
            // 处理Map参数（通常是批量操作或者复杂查询）
            handleMapParameter((Map<?, ?>) parameter, true);
        }
    }
    
    private void handleUpdate(Object parameter) {
        if (parameter instanceof BaseDO) {
            BaseDO entity = (BaseDO) parameter;
            Long currentTime = System.currentTimeMillis() / 1000; // 10位时间戳
            Long currentUserId = UserContext.getCurrentUserId();
            
            entity.setLastUpdatedDate(currentTime);
            if (currentUserId != null) {
                entity.setLastUpdatedBy(currentUserId);
            }
        } else if (parameter instanceof Map) {
            // 处理Map参数
            handleMapParameter((Map<?, ?>) parameter, false);
        }
    }
    
    /**
     * 处理Map类型的参数
     */
    private void handleMapParameter(Map<?, ?> paramMap, boolean isInsert) {
        Long currentTime = System.currentTimeMillis() / 1000;
        Long currentUserId = UserContext.getCurrentUserId();
        
        for (Object value : paramMap.values()) {
            if (value instanceof BaseDO) {
                BaseDO entity = (BaseDO) value;
                if (isInsert) {
                    if (entity.getCreatedDate() == null) {
                        entity.setCreatedDate(currentTime);
                    }
                    if (entity.getCreatedBy() == null && currentUserId != null) {
                        entity.setCreatedBy(currentUserId);
                    }
                }
                if (entity.getLastUpdatedDate() == null) {
                    entity.setLastUpdatedDate(currentTime);
                }
                if (currentUserId != null) {
                    entity.setLastUpdatedBy(currentUserId);
                }
            }
        }
        
        try {
            if (isInsert) {
                if (!paramMap.containsKey("createdDate")) {
                    setMapValue(paramMap, "createdDate", currentTime);
                }
                if (!paramMap.containsKey("createdBy") && currentUserId != null) {
                    setMapValue(paramMap, "createdBy", currentUserId);
                }
            }
            setMapValue(paramMap, "lastUpdatedDate", currentTime);
            if (currentUserId != null) {
                setMapValue(paramMap, "lastUpdatedBy", currentUserId);
            }
        } catch (Exception e) {
        }
    }
    
    private void setMapValue(Map<?, ?> map, String key, Object value) {
        if (map instanceof Map) {
            ((Map<String, Object>) map).put(key, value);
        }
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
}