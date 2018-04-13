package com.eis.upgrade.config;

import com.eis.common.page.PagePlugin;
import org.apache.ibatis.plugin.Interceptor;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
public class MyBatisConfig {

    @Autowired
    private DataSource druidDataSource;
    @Value("${mybatis.mapper-locations}")
    private String mapperLocation;

    @Bean
    public PagePlugin pagePlugin(){
        Properties properties = new Properties();
        properties.setProperty("dialect", "mysql");
        properties.setProperty("pageSqlId", ".*Paging");

        PagePlugin pagePlugin = new PagePlugin();
        pagePlugin.setProperties(properties);

        return pagePlugin;
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory() throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setDataSource(druidDataSource);
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        sqlSessionFactoryBean.setMapperLocations(resolver.getResources(mapperLocation));
        sqlSessionFactoryBean.setPlugins(new Interceptor[]{pagePlugin()});
        return sqlSessionFactoryBean;
    }
}
