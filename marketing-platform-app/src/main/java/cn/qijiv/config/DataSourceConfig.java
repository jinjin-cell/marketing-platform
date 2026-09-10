package cn.qijiv.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.elasticsearch.xpack.sql.jdbc.EsDataSource;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * MyBatis 多数据源配置。
 *
 * <p>业务 DAO 使用 ShardingSphere 聚合后的 MySQL 数据源；Elasticsearch DAO
 * 使用独立的 X-Pack SQL JDBC 数据源。两个 Mapper 包必须分别绑定到各自的
 * SqlSessionFactory，避免 SQL 被发送到错误的数据源。</p>
 */
@Configuration
public class DataSourceConfig {

    private static final String MYSQL_MAPPER_LOCATION = "classpath:/mybatis/mapper/mysql/*.xml";
    private static final String ELASTICSEARCH_MAPPER_LOCATION = "classpath:/mybatis/mapper/elasticsearch/*.xml";
    private static final String MYBATIS_CONFIG_LOCATION = "classpath:/mybatis/config/mybatis-config.xml";

    @Configuration
    @MapperScan(
            basePackages = "cn.qijiv.infrastructure.elasticsearch",
            sqlSessionFactoryRef = "elasticsearchSqlSessionFactory"
    )
    static class ElasticsearchMyBatisConfig {

        @Bean("elasticsearchDataSource")
        public DataSource elasticsearchDataSource(
                @Value("${spring.elasticsearch.uris}") String elasticsearchUris,
                @Value("${spring.elasticsearch.username:}") String username,
                @Value("${spring.elasticsearch.password:}") String password,
                @Value("${spring.elasticsearch.jdbc.timezone:Asia/Shanghai}") String timezone,
                @Value("${spring.elasticsearch.jdbc.login-timeout-seconds:10}") int loginTimeoutSeconds
        ) throws SQLException {
            EsDataSource dataSource = new EsDataSource();
            dataSource.setUrl(toJdbcUrl(elasticsearchUris));
            dataSource.setLoginTimeout(loginTimeoutSeconds);

            Properties properties = new Properties();
            properties.setProperty("timezone", timezone);
            if (!username.trim().isEmpty()) {
                properties.setProperty("user", username);
            }
            if (!password.isEmpty()) {
                properties.setProperty("password", password);
            }
            dataSource.setProperties(properties);
            return dataSource;
        }

        @Bean("elasticsearchSqlSessionFactory")
        public SqlSessionFactory elasticsearchSqlSessionFactory(
                @Qualifier("elasticsearchDataSource") DataSource elasticsearchDataSource
        ) throws Exception {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
            factoryBean.setDataSource(elasticsearchDataSource);
            factoryBean.setConfigLocation(resolver.getResource(MYBATIS_CONFIG_LOCATION));
            factoryBean.setMapperLocations(resolver.getResources(ELASTICSEARCH_MAPPER_LOCATION));
            return factoryBean.getObject();
        }

        private String toJdbcUrl(String elasticsearchUris) {
            String firstUri = elasticsearchUris.split(",")[0].trim();
            if (firstUri.isEmpty()) {
                throw new IllegalArgumentException("spring.elasticsearch.uris must not be empty");
            }
            return firstUri.startsWith("jdbc:es://") ? firstUri : "jdbc:es://" + firstUri;
        }
    }

    @Configuration
    @MapperScan(
            basePackages = "cn.qijiv.infrastructure.dao",
            sqlSessionFactoryRef = "mysqlSqlSessionFactory"
    )
    static class MysqlMyBatisConfig {

        /**
         * 将 ShardingSphere 聚合数据源声明为应用默认 DataSource。
         *
         * <p>这里返回的是同一个对象，不会重复创建连接池；主要用于让
         * JdbcTemplate 等 Spring Boot 自动配置在存在 ES DataSource 时仍能
         * 明确选择 MySQL。</p>
         */
        @Bean("mysqlDataSource")
        @Primary
        public DataSource mysqlDataSource(
                @Qualifier("shardingDataSource") DataSource shardingDataSource
        ) {
            return shardingDataSource;
        }

        @Bean("mysqlSqlSessionFactory")
        @Primary
        public SqlSessionFactory mysqlSqlSessionFactory(
                @Qualifier("mysqlDataSource") DataSource mysqlDataSource
        ) throws Exception {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
            factoryBean.setDataSource(mysqlDataSource);
            factoryBean.setConfigLocation(resolver.getResource(MYBATIS_CONFIG_LOCATION));
            factoryBean.setMapperLocations(resolver.getResources(MYSQL_MAPPER_LOCATION));
            return factoryBean.getObject();
        }

        @Bean("transactionManager")
        @Primary
        public PlatformTransactionManager transactionManager(
                @Qualifier("mysqlDataSource") DataSource mysqlDataSource
        ) {
            return new DataSourceTransactionManager(mysqlDataSource);
        }
    }
}
