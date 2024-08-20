 package com.example.demo;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.pool.PoolingConnectionFactoryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import static io.r2dbc.spi.ConnectionFactoryOptions.*;

@Configuration
@EnableR2dbcRepositories
public class DatabaseConfig {

    @Bean
    public ConnectionFactory connectionFactory() {
        return ConnectionFactoryOptions.builder()
            .option(DRIVER, "oracle")
            .option(PROTOCOL, "tcp")
            .option(HOST, "localhost")
            .option(PORT, 1521)
            .option(DATABASE, "xe")
            .option(USER, "yourUsername")
            .option(PASSWORD, "yourPassword")
            .option(PoolingConnectionFactoryProvider.MAX_SIZE, 10)
            .build();
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        return new R2dbcEntityTemplate(connectionFactory);
    }
}
