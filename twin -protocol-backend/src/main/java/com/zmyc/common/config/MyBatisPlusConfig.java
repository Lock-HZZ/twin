package com.zmyc.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.zmyc.interceptor.AuditInterceptor;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;

@Configuration
public class MyBatisPlusConfig {
    
    /**
     * 配置MyBatis-Plus拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 添加分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        paginationInterceptor.setDbType(DbType.MYSQL);
        paginationInterceptor.setOverflow(false);
        paginationInterceptor.setMaxLimit(500L);
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        return interceptor;
    }
    
    /**
     * 注册审计拦截器
     */
    @Bean
    public AuditInterceptor auditInterceptor() {
        return new AuditInterceptor();
    }
    
    /**
     * MyBatis-Plus 全局配置
     */
    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        
        // 数据库配置
        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(com.baomidou.mybatisplus.annotation.IdType.AUTO);
        dbConfig.setLogicDeleteField("deleted");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        globalConfig.setDbConfig(dbConfig);
        
        return globalConfig;
    }
    
    /**
     * SqlSessionFactory配置
     */
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        MybatisSqlSessionFactoryBean sessionFactory = new MybatisSqlSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        
        // 设置MyBatis配置
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(NoLoggingImpl.class);
        sessionFactory.setConfiguration(configuration);
        
        // 设置全局配置
        sessionFactory.setGlobalConfig(globalConfig());
        
        // 设置插件
        sessionFactory.setPlugins(mybatisPlusInterceptor(), auditInterceptor());
        
        // 设置Mapper文件位置
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sessionFactory.setMapperLocations(resolver.getResources("classpath*:/mapper/**/*.xml"));
        
        return sessionFactory.getObject();
    }
} 