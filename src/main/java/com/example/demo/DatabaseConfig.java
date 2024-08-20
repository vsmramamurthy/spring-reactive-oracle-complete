 package com.example.demo;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.pool.PoolingConnectionFactoryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.core.DatabaseClient; 	
import static io.r2dbc.spi.ConnectionFactoryOptions.*;

@Configuration
@EnableR2dbcRepositories
public class DatabaseConfig {

      	@Bean
    public ConnectionFactory connectionFactory() {
        ConnectionFactoryOptions configuration = ConnectionFactoryOptions.builder()
            .option(DRIVER, "oracle")
            .option(PROTOCOL, "tcp")
            .option(HOST, "localhost")
            .option(PORT, 1521)
            .option(DATABASE, "xe")
            .option(USER, "yourUsername")
            .option(PASSWORD, "yourPassword")
            .option(PoolingConnectionFactoryProvider.MAX_SIZE, 10)
.maxSize(20)  // Max number of connections
            .initialSize(5)  // Initial number of connections
            .maxIdleTime(Duration.ofMinutes(30))
            .acquireRetry(3)
            .build();

        //return ConnectionFactories.get(options);

	ConnectionPool pool = new ConnectionPool(configuration);
        
        pool.addListener(event -> {
            if (event.isAcquire()) {
                System.out.println("Connection acquired");
            } else if (event.isRelease()) {
                System.out.println("Connection released");
            }
        });

        return pool;
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        return new R2dbcEntityTemplate(connectionFactory);
    }

 @Bean
    public DatabaseClient databaseClient(ConnectionFactory connectionFactory) {
        return DatabaseClient.create(connectionFactory);
    } 	
}
