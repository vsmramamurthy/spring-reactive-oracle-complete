package com.example.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class QueryExecutionService {

    private final DataSource dataSource;
    private final Map<String, String> queryCache = new HashMap<>();

    public QueryExecutionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void loadQueryCache() {
        String loadQuery = "SELECT template_id, query_string FROM db_template";
        try (Connection connection = dataSource.getConnection();
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

    public Map<String, String> getAllQueries() {
        return new HashMap<>(queryCache);
    }

    public String getSingleQuery(String templateId) {
        return queryCache.get(templateId);
    }

    public void refreshQueryCache() {
        loadQueryCache();
    }

    public Mono<String> executeSingleQuery(String templateId, Object... params) {
        return Mono.fromCallable(() -> {
            String query = queryCache.get(templateId);
            if (query == null) {
                throw new IllegalArgumentException("Invalid template ID");
            }

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                for (int i = 0; i < params.length; i++) {
                    statement.setObject(i + 1, params[i]);
                }

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getString(1); // Assuming single column result
                    }
                }
            }
            return null;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> executeMultipleQueries(String[] templateIds, Object[][] params) {
        return Mono.fromRunnable(() -> {
            try (Connection connection = dataSource.getConnection()) {
                for (int i = 0; i < templateIds.length; i++) {
                    String query = queryCache.get(templateIds[i]);
                    if (query == null) {
                        throw new IllegalArgumentException("Invalid template ID");
                    }

                    try (PreparedStatement statement = connection.prepareStatement(query)) {
                        for (int j = 0; j < params[i].length; j++) {
                            statement.setObject(j + 1, params[i][j]);
                        }
                        statement.execute();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace(); // Implement proper logging
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    public Mono<String> executeProcedure(String procedureName, Object... params) {
        return Mono.fromCallable(() -> {
            try (Connection connection = dataSource.getConnection();
                 CallableStatement callableStatement = connection.prepareCall("{call " + procedureName + "(?, ?)}")) {

                for (int i = 0; i < params.length; i++) {
                    callableStatement.setObject(i + 1, params[i]);
                }

                callableStatement.registerOutParameter(1, java.sql.Types.VARCHAR);
                callableStatement.execute();

                return callableStatement.getString(1);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
