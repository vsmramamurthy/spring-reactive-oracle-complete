
package com.example.demo;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface TemplateRepository extends ReactiveCrudRepository<Template, Long> {
    Mono<Template> findByTemplateId(String templateId);
}
