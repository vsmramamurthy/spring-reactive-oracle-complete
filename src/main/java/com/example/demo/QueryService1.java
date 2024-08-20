package com.example.demo;

import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class QueryService {

    private final QueryCacheService queryCacheService;
    private final DatabaseClient databaseClient;

    public QueryService(QueryCacheService queryCacheService, DatabaseClient databaseClient) {
        this.queryCacheService = queryCacheService;
        this.databaseClient = databaseClient;
    }

    public Mono<String> executeSingleQuery(String templateId, Map<String, Object> params) {
        return queryCacheService.getQueryByTemplateId(templateId)
            .flatMap(query -> {
                String finalQuery = applyParams(query, params);
                return databaseClient.sql(finalQuery)
                    .fetch()
                    .first()
                    .map(result -> result.toString());
            });
    }

    public Flux<String> executeMultipleQueries(Map<String, String> templateIdParamsMap) {
        return Flux.fromIterable(templateIdParamsMap.entrySet())
            .flatMap(entry -> executeSingleQuery(entry.getKey(), Map.of("param", entry.getValue())));
    }

    public Mono<Void> executeProcedure(String templateId, Map<String, Object> inParams, Map<String, Object> outParams) {
        return queryCacheService.getQueryByTemplateId(templateId)
            .flatMap(query -> {
                String finalQuery = applyParams(query, inParams);
                return databaseClient.sql(finalQuery)
                    .then();
            });
    }

    private String applyParams(String query, Map<String, Object> params) {
        for (Map.Entry<String, Object> param : params.entrySet()) {
            query = query.replace(":" + param.getKey(), param.getValue().toString());
        }
        return query;
    }

    public Flux<String> executeQueryWithoutParams(String templateId) {
        return queryCacheService.getQueryByTemplateId(templateId)
            .flatMapMany(query -> databaseClient.sql(query)
                .fetch()
                .all()	
 .doOnNext(result -> System.out.println("Result: " + result))	
                .doOnError(e -> {
                // Log error details
                System.err.println("Error executing query: " + e.getMessage());
            })
            .onErrorResume(e -> {
                // Fallback logic (e.g., return an empty result)
                return Flux.just("An error occurred, returning fallback value.");
            })
.map(result -> result.toString());
            );
    }


If databaseClient.sql is not fetching values from the database, there could be several reasons for this issue. Below are some steps and suggestions to troubleshoot and resolve the problem:

1. Verify the SQL Query
Ensure that the SQL query being executed is correct. The query should be well-formed and compatible with the Oracle database.

1.1. Test the Query Manually
Run the SQL query directly in your Oracle database using a tool like SQL*Plus, SQL Developer, or any other database client to ensure it returns the expected results.

2. Check the DatabaseClient Configuration
Ensure that the DatabaseClient is properly configured to connect to your Oracle database.

2.1. Verify Connection Settings
Ensure that your application.yml or application.properties has the correct database connection settings:

yaml
Copy code
spring:
  r2dbc:
    url: r2dbc:oracle://localhost:1521/xe
    username: your_username
    password: your_password
3. Debug the DatabaseClient Execution
Add logging before and after the databaseClient.sql call to ensure it’s being executed correctly.

3.1. Add Logging
Add logging to the execution pipeline to capture the query being executed and any errors that occur:

java
Copy code
public Flux<Map<String, Object>> executeQueryForList(String templateName, Map<String, Object> params) {
    return queryCacheService.getQueryByTemplateName(templateName)
        .flatMapMany(query -> {
            System.out.println("Executing query: " + query);
            DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(query);

            // Bind parameters to the query
            for (Map.Entry<String, Object> param : params.entrySet()) {
                spec = spec.bind(param.getKey(), param.getValue());
                System.out.println("Binding param: " + param.getKey() + " = " + param.getValue());
            }

            return spec.fetch().all()
                .doOnNext(row -> System.out.println("Fetched row: " + row))
                .doOnError(e -> System.err.println("Error during fetch: " + e.getMessage()));
        })
        .onErrorResume(e -> {
            System.err.println("Error executing query: " + e.getMessage());
            return Flux.empty();
        });
}
}
