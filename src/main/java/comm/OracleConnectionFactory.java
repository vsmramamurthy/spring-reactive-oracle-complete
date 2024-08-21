package com.example.config;

import com.zaxxer.hikari.HikariDataSource;
import oracle.jdbc.pool.OracleDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class OracleConnectionFactory {

    @Bean
    public DataSource dataSource() throws SQLException {
        OracleDataSource oracleDataSource = new OracleDataSource();
        oracleDataSource.setURL("jdbc:oracle:thin:@//localhost:1521/ORCLPDB1");
        oracleDataSource.setUser("your_username");
        oracleDataSource.setPassword("your_password");

        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setDataSource(oracleDataSource);
        hikariDataSource.setMaximumPoolSize(10);

        return hikariDataSource;
    }
}
