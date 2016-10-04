package ru.majordomo.hms.personmgr;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
    @Bean(name = "partnersDataSource")
    @ConfigurationProperties(prefix="datasource.partners")
    public DataSource partnersDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "billingDataSource")
    @Primary
    @ConfigurationProperties(prefix="datasource.billing")
    public DataSource billingDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "partnersJdbcTemplate")
    @Autowired
    public JdbcTemplate partnersJdbcTemplate(@Qualifier("partnersDataSource") DataSource partnersDataSource) {
        return new JdbcTemplate(partnersDataSource);
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

    @Bean(name = "namedParameterJdbcTemplate")
    @Primary
    @Autowired
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(@Qualifier("billingDataSource") DataSource billingDataSource) {
        return new NamedParameterJdbcTemplate(billingDataSource);
    }
}
