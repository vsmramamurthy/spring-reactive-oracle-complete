package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/query")
public class QueryController {
    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/single")
    public Mono<ResponseEntity<String>> executeSingleQuery(
            @RequestParam String templateId, @RequestBody Map<String, Object> params) {
        return queryService.executeSingleQuery(templateId, params)
                .map(result -> ResponseEntity.ok(result))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Error executing query")));
    }

    @PostMapping("/multiple")
    public Flux<String> executeMultipleQueries(@RequestBody Map<String, String> templateIdParamsMap) {
        return queryService.executeMultipleQueries(templateIdParamsMap)
                .onErrorResume(e -> Flux.just("Error executing one of the queries"));
    }

    @PostMapping("/procedure")
    public Mono<ResponseEntity<Void>> executeProcedure(
            @RequestParam String templateId, @RequestBody Map<String, Object> inParams, @RequestParam Map<String, Object> outParams) {
        return queryService.executeProcedure(templateId, inParams, outParams)
                .map(result -> ResponseEntity.ok().build())
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).build()));
    }

    @GetMapping("/single")
    public Mono<ResponseEntity<String>> executeSingleQueryGet(
            @RequestParam String templateId, @RequestParam Map<String, Object> params) {
        return queryService.executeSingleQuery(templateId, params)
                .map(result -> ResponseEntity.ok(result))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Error executing query")));
    }

    @GetMapping("/multiple")
    public Flux<String> executeMultipleQueriesGet(@RequestParam Map<String, String> templateIdParamsMap) {
        return queryService.executeMultipleQueries(templateIdParamsMap)
                .onErrorResume(e -> Flux.just("Error executing one of the queries"));
    }

    @GetMapping("/procedure")
    public Mono<ResponseEntity<Void>> executeProcedureGet(
            @RequestParam String templateId, @RequestParam Map<String, Object> inParams, @RequestParam Map<String, Object> outParams) {
        return queryService.executeProcedure(templateId, inParams, outParams)
                .map(result -> ResponseEntity.ok().build())
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).build()));
    }
}
