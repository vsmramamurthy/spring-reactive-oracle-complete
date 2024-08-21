package com.example.controller;

import com.example.service.QueryExecutionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class QueryController {

    private final QueryExecutionService queryExecutionService;

    public QueryController(QueryExecutionService queryExecutionService) {
        this.queryExecutionService = queryExecutionService;
    }

    @GetMapping("/queries")
    public Map<String, String> getAllQueries() {
        return queryExecutionService.getAllQueries();
    }

    @GetMapping("/query")
    public String getSingleQuery(@RequestParam String templateId) {
        return queryExecutionService.getSingleQuery(templateId);
    }

    @PostMapping("/refreshQueries")
    public void refreshQueryCache() {
        queryExecutionService.refreshQueryCache();
    }


   @PostMapping("/executeSingle")
    public Mono<String> executeSingleQuery(@RequestParam String templateId) {
        return queryExecutionService.executeSingleQuery(templateId);
    }
	
    @PostMapping("/executeSingle")
    public Mono<String> executeSingleQuery(@RequestParam String templateId, @RequestParam Object[] params) {
        return queryExecutionService.executeSingleQuery(templateId, params);
    }
	
	@GetMapping("/executeSingle")
    public Flux<Map<String, Object>> executeSingleQuery(
            @RequestParam String templateId,
            @RequestParam(defaultValue = "50") int fetchSize) {
        return queryExecutionService.executeSingleQuery(templateId, fetchSize);
    }

    @PostMapping("/executeMultiple")
    public Mono<Void> executeMultipleQueries(@RequestParam String[] templateIds, @RequestParam Object[][] params) {
        return queryExecutionService.executeMultipleQueries(templateIds, params);
    }

    @PostMapping("/executeProcedure")
    public Mono<String> executeProcedure(@RequestParam String procedureName, @RequestParam Object[] params) {
        return queryExecutionService.executeProcedure(procedureName, params);
    }
	
	@PostMapping("/executeMultiple")
public Mono<List<List<Map<String, Object>>>> executeMultipleQueries(
        @RequestBody Map<String, Object> request) {

    // Safely retrieve the templateIds
    Object templateIdsObj = request.get("templateIds");
 ObjectMapper objectMapper = new ObjectMapper();
            List<String> list = objectMapper.readValue((String) templateIdsObj, new TypeReference<List<String>>() {});
	String[] stringArray = list.toArray(new String[0]);
    List<List<Object>> paramsList = (List<List<Object>>) request.get("params");

   Object[][] params = convertListTo2DArray(paramsList);

	Object[][] array = new Object[paramsList.size()][];

        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i).toArray(new Object[0]);  // Convert each list to an Object array
        }

        
			

    return queryExecutionService.executeMultipleQueries(templateIds, array);
}


@PostMapping("/executeMultiple")
    public Mono<List<List<Map<String, Object>>>> executeMultipleQueries(
            @RequestBody Map<String, Object> request) {

        // Extract templateIds
        Object templateIdsObj = request.get("templateIds");
      if (!(templateIdsObj instanceof List)) {
            throw new IllegalArgumentException("templateIds should be a list");
        }

        List<?> templateIdsList = (List<?>) templateIdsObj;
        String[] templateIds = templateIdsList.stream()
                                              .map(Object::toString) // Convert each element to a string
                                              .toArray(String[]::new);

        // Extract params
        Object paramsObj = request.get("params");
        Object[][] params;

        if (paramsObj instanceof List) {
            List<?> paramsList = (List<?>) paramsObj;
            params = paramsList.stream()
                               .map(item -> {
                                   if (item instanceof List) {
                                       List<?> innerList = (List<?>) item;
                                       return innerList.toArray(new Object[0]);
                                   } else {
                                       throw new IllegalArgumentException("params must be a list of lists");
                                   }
                               })
                               .toArray(Object[][]::new);
        } else {
            throw new IllegalArgumentException("params must be a list of lists");
        }

        // Now you can use templateIds and params in your service call
        return queryExecutionService.executeMultipleQueries(
                templateIds.toArray(new String[0]), 
                params
        );
    }


	 @PostMapping("/executeProcedure")
    public Mono<Map<String, Object>> executeProcedure(@RequestParam String schemaName,
                                                      @RequestParam String catalogName,
                                                      @RequestParam String procedureName,
                                                      @RequestBody Map<Integer, Object> inParams,
                                                      @RequestParam Map<Integer, Integer> outParams) {
        return queryExecutionService.executeProcedure(schemaName, catalogName, procedureName, inParams, outParams);
    }
	
	@PostMapping("/executeProcedure")
    public Mono<Map<String, Object>> executeProcedure(@RequestBody Map<String, Object> requestBody) {
        return queryExecutionService.executeProcedure(requestBody);
    }
    @GetMapping("/status")
    public Mono<String> getStatus() {
        return Mono.just("Service is running");
    }
}
