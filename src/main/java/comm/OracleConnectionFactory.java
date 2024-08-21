package com.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import oracle.jdbc.pool.OracleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.SQLException;

@Configuration
public class OracleConnectionFactory {

    @Bean
    public HikariDataSource hikariDataSource() throws SQLException {
        // Configure OracleDataSource
        OracleDataSource oracleDataSource = new OracleDataSource();
        oracleDataSource.setURL("jdbc:oracle:thin:@//localhost:1521/ORCLPDB1");
        oracleDataSource.setUser("your_username");
        oracleDataSource.setPassword("your_password");

        // Enable Oracle-specific features
        oracleDataSource.setImplicitCachingEnabled(true);
        oracleDataSource.setFastConnectionFailoverEnabled(true);

        // Configure HikariCP with OracleDataSource
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSource(oracleDataSource);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(5);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setPoolName("HikariPool-Oracle");

        return new HikariDataSource(hikariConfig);
    }
}