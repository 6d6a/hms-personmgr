package ru.majordomo.hms.personmgr.config;


import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    private HikariSettings hikariSettings;

    @Autowired
    public void setHikariSettings(HikariSettings hikariSettings) {
        this.hikariSettings = hikariSettings;
    }

    @Bean(name = "partnersDataSourceProperties")
    @ConfigurationProperties(prefix="datasource.partners")
    public DataSourceProperties partnersDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "partnersDataSource")
    public DataSource partnersDataSource(@Qualifier("partnersDataSourceProperties") DataSourceProperties properties) {
        return HikariConfigWrapper(properties.initializeDataSourceBuilder().type(HikariDataSource.class).build());
    }

    @Primary
    @Bean(name = "billingDataSourceProperties")
    @ConfigurationProperties("datasource.billing")
    public DataSourceProperties billingDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "billingDataSource")
    public DataSource billingDataSource(@Qualifier("billingDataSourceProperties") DataSourceProperties properties) {
        return HikariConfigWrapper(properties.initializeDataSourceBuilder().type(HikariDataSource.class).build());
    }

    @Bean(name = "billing2DataSourceProperties")
    @ConfigurationProperties(prefix="datasource.billing2")
    public DataSourceProperties billing2DataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "billing2DataSource")
    public DataSource billing2DataSource(@Qualifier("billing2DataSourceProperties") DataSourceProperties properties) {
        return HikariConfigWrapper(properties.initializeDataSourceBuilder().type(HikariDataSource.class).build());
    }

    @Bean(name = "partnersJdbcTemplate")
    @Autowired
    public JdbcTemplate partnersJdbcTemplate(@Qualifier("partnersDataSource") DataSource partnersDataSource) {
        return new JdbcTemplate(partnersDataSource);
    }

    @Bean(name = "billing2JdbcTemplate")
    @Autowired
    public JdbcTemplate billing2JdbcTemplate(@Qualifier("billing2DataSource") DataSource billing2DataSource) {
        return new JdbcTemplate(billing2DataSource);
    }

    @Bean(name = "jdbcTemplate")
    @Primary
    @Autowired
    public JdbcTemplate jdbcTemplate(@Qualifier("billingDataSource") DataSource billingDataSource) {
        return new JdbcTemplate(billingDataSource);
    }

    @Bean(name = "partnersNamedParameterJdbcTemplate")
    @Autowired
    public NamedParameterJdbcTemplate partnersNamedParameterJdbcTemplate(@Qualifier("partnersDataSource") DataSource partnersDataSource) {
        return new NamedParameterJdbcTemplate(partnersDataSource);
    }

    @Bean(name = "billing2NamedParameterJdbcTemplate")
    @Autowired
    public NamedParameterJdbcTemplate billing2NamedParameterJdbcTemplate(@Qualifier("billing2DataSource") DataSource billing2DataSource) {
        return new NamedParameterJdbcTemplate(billing2DataSource);
    }

    @Bean(name = "namedParameterJdbcTemplate")
    @Primary
    @Autowired
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(@Qualifier("billingDataSource") DataSource billingDataSource) {
        return new NamedParameterJdbcTemplate(billingDataSource);
    }

    private HikariDataSource HikariConfigWrapper(HikariDataSource hikari) {
        hikari.setMaximumPoolSize(hikariSettings.getMaximumPoolSize());
        hikari.setConnectionTimeout(hikariSettings.getConnectionTimeout());
        hikari.setIdleTimeout(hikariSettings.getIdleTimeout());
        hikari.setMaxLifetime(hikariSettings.getMaxLifetime());

        return hikari;
    }
}
