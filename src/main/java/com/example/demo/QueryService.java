
package com.example.demo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Service
public class QueryService {

    private final QueryCacheService queryCacheService;
    private final JdbcTemplate jdbcTemplate;

    public QueryService(QueryCacheService queryCacheService, JdbcTemplate jdbcTemplate) {
        this.queryCacheService = queryCacheService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public Mono<String> executeSingleQuery(String templateId, Map<String, Object> params) {
        return queryCacheService.getQueryByTemplateId(templateId)
            .flatMap(query -> Mono.fromCallable(() -> {
                String finalQuery = applyParams(query, params);
                return jdbcTemplate.queryForObject(finalQuery, String.class);
            }).subscribeOn(Schedulers.boundedElastic()));
    }

    public Flux<String> executeMultipleQueries(Map<String, String> templateIdParamsMap) {
        return Flux.fromIterable(templateIdParamsMap.entrySet())
            .flatMap(entry -> executeSingleQuery(entry.getKey(), Map.of("param", entry.getValue())));
    }

    public Mono<Void> executeProcedure(String templateId, Map<String, Object> inParams, Map<String, Object> outParams) {
        return queryCacheService.getQueryByTemplateId(templateId)
            .flatMap(query -> Mono.fromRunnable(() -> {
                String finalQuery = applyParams(query, inParams);
                jdbcTemplate.execute(finalQuery);
            }).subscribeOn(Schedulers.boundedElastic()).then());
    }

    private String applyParams(String query, Map<String, Object> params) {
        for (Map.Entry<String, Object> param : params.entrySet()) {
            query = query.replace(":" + param.getKey(), param.getValue().toString());
        }
        return query;
    }
}
