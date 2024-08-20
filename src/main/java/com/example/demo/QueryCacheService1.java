package com.example.demo;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QueryCacheService {
    private final TemplateRepository templateRepository;
    private final Map<String, String> queryCache = new ConcurrentHashMap<>();

    public QueryCacheService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @PostConstruct
    public void loadQueriesIntoCache() {
        templateRepository.findAll()
            .doOnNext(template -> queryCache.put(template.getTemplateId(), template.getQuery()))
            .subscribe();
    }

    public Mono<String> getQueryByTemplateId(String templateId) {
        return Mono.justOrEmpty(queryCache.get(templateId));
    }
}
