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
                .map(result -> result.toString())
            );
    }
}
