package com.example.service;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QueryExecutionService {

    private final HikariDataSource hikariDataSource;
    private final Map<String, String> queryCache = new HashMap<>();

    public QueryExecutionService(HikariDataSource hikariDataSource) {
        this.hikariDataSource = hikariDataSource;
    }

    @PostConstruct
    public void loadQueryCache() {
        String loadQuery = "SELECT template_id, query_string FROM db_template";
        try (Connection connection = hikariDataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(loadQuery)) {
            
            while (resultSet.next()) {
                String templateId = resultSet.getString("template_id");
                String queryString = resultSet.getString("query_string");
                queryCache.put(templateId, queryString);
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Implement proper logging
        }
    }

    public Mono<List<List<Map<String, Object>>>> executeMultipleQueries(String[] templateIds, Object[][] params) {
        List<Mono<List<Map<String, Object>>>> queryMonos = new ArrayList<>();

        for (int i = 0; i < templateIds.length; i++) {
            String templateId = templateIds[i];
            Object[] paramSet = params[i];
            
            Mono<List<Map<String, Object>>> queryMono = executeQueryAsync(templateId, paramSet);
            queryMonos.add(queryMono);
        }

        return Flux.concat(queryMonos) // Executes queries sequentially or use Flux.merge(queryMonos) for parallel execution
                   .collectList(); // Collects all result sets into a single list
    }

    private Mono<List<Map<String, Object>>> executeQueryAsync(String templateId, Object[] params) {
        return Mono.fromCallable(() -> {
            String query = queryCache.get(templateId);
            if (query == null) {
                throw new IllegalArgumentException("Invalid template ID: " + templateId);
            }

            List<Map<String, Object>> resultSetList = new ArrayList<>();

            try (Connection connection = hikariDataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                for (int j = 0; j < params.length; j++) {
                    statement.setObject(j + 1, params[j]); // Parameters are 1-indexed
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (resultSet.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int k = 1; k <= columnCount; k++) {
                            row.put(metaData.getColumnName(k), resultSet.getObject(k));
                        }
                        resultSetList.add(row);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace(); // Implement proper logging
                throw new RuntimeException("Database query execution failed", e);
            }

            return resultSetList;
        }).subscribeOn(Schedulers.boundedElastic()); // Runs the query asynchronously
    }
}
