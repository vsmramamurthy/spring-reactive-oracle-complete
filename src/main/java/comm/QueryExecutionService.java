package com.example.service;

import oracle.jdbc.OraclePreparedStatement;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class QueryExecutionService {

    private final HikariDataSource hikariDataSource;
    private final Map<String, String> queryCache = new HashMap<>();
 private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH.mm");

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

            try (Connection connection = hikariDataSource.getConnection();
                 OraclePreparedStatement statement = (OraclePreparedStatement) connection.prepareStatement(query)) {

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
	
	
	public Mono<List<Map<String, Object>>> executeSingleQuery(String templateId, Object... params) {
    return Mono.fromCallable(() -> {
        String query = queryCache.get(templateId);
        if (query == null) {
            throw new IllegalArgumentException("Invalid template ID");
        }

        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection connection = hikariDataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {

            // Ensure that each parameter is set in the PreparedStatement
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]); // Parameters are 1-indexed in JDBC
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int j = 1; j <= columnCount; j++) {
                        row.put(metaData.getColumnName(j), resultSet.getObject(j));
                    }
                    results.add(row);
                }
            }
        }

        return results;
    }).subscribeOn(Schedulers.boundedElastic());
}
	public Mono<List<Map<String, Object>>> executeSingleQuery(String templateId) {
        return Mono.fromCallable(() -> {
            String query = queryCache.get(templateId);
            if (query == null) {
                throw new IllegalArgumentException("Invalid template ID");
            }

            List<Map<String, Object>> results = new ArrayList<>();

            try (Connection connection = hikariDataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), resultSet.getObject(i));
                    }
                    results.add(row);
                }
            }

            return results;
        }).subscribeOn(Schedulers.boundedElastic());
    }
	
	 public Flux<Map<String, Object>> executeSingleQuery(String templateId, int fetchSize) {
        return Flux.create(sink -> {
            String query = queryCache.get(templateId);
            if (query == null) {
                sink.error(new IllegalArgumentException("Invalid template ID"));
                return;
            }

            try (Connection connection = hikariDataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                statement.setFetchSize(fetchSize); // Set the fetch size

                try (ResultSet resultSet = statement.executeQuery()) {
                    ResultSetMetaData metaData = resultSet.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (resultSet.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(metaData.getColumnName(i), resultSet.getObject(i));
                        }
                        sink.next(row);
                    }
                    sink.complete();
                }
            } catch (SQLException e) {
                sink.error(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> executeMultipleQueries(String[] templateIds, Object[][] params) {
        return Mono.fromRunnable(() -> {
            try (Connection connection = hikariDataSource.getConnection()) {
                for (int i = 0; i < templateIds.length; i++) {
                    String query = queryCache.get(templateIds[i]);
                    if (query == null) {
                        throw new IllegalArgumentException("Invalid template ID");
                    }

                    try (OraclePreparedStatement statement = (OraclePreparedStatement) connection.prepareStatement(query)) {
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
	
	public Mono<List<List<Map<String, Object>>>> executeMultipleQueries(String[] templateIds, Object[][] params) {
        return Mono.fromCallable(() -> {
            List<List<Map<String, Object>>> allResults = new ArrayList<>();

            try (Connection connection = hikariDataSource.getConnection()) {
                for (int i = 0; i < templateIds.length; i++) {
                    String query = queryCache.get(templateIds[i]);
                    if (query == null) {
                        throw new IllegalArgumentException("Invalid template ID: " + templateIds[i]);
                    }

                    List<Map<String, Object>> resultSetList = new ArrayList<>();

                    try (PreparedStatement statement = connection.prepareStatement(query)) {
                        for (int j = 0; j < params[i].length; j++) {
                            statement.setObject(j + 1, params[i][j]); // Parameters are 1-indexed
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
                    }
                    allResults.add(resultSetList); // Add each result set to the list of result sets
                }
            } catch (SQLException e) {
                e.printStackTrace(); // Implement proper logging
                throw new RuntimeException("Database query execution failed", e);
            }

            return allResults;
        }).subscribeOn(Schedulers.boundedElastic());
    }
	
	private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }


    public Mono<String> executeProcedure(String procedureName, Object... params) {
        return Mono.fromCallable(() -> {
            try (Connection connection = hikariDataSource.getConnection();
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
	
	   private Map<Integer, String> castToIntegerStringMap(Object obj) {
        if (obj instanceof Map) {
            Map<?, ?> genericMap = (Map<?, ?>) obj;
            Map<Integer, String> typedMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : genericMap.entrySet()) {
                if (entry.getKey() instanceof Integer && entry.getValue() instanceof String) {
                    typedMap.put((Integer) entry.getKey(), (String) entry.getValue());
                } else {
                    throw new IllegalArgumentException("Invalid map types for inParams or outParams");
                }
            }
            return typedMap;
        } else {
            throw new IllegalArgumentException("Expected a map but got: " + obj.getClass());
        }
    }
	
	 private static boolean isDate(String value) {
        try {
            DATE_FORMAT.parse(value);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
	
	   public Mono<Map<String, Object>> executeProcedure(String schemaName, String catalogName, String procedureName,
                                                      Map<Integer, Object> inParams, Map<Integer, Integer> outParams) {
        return Mono.fromCallable(() -> {
            String callStatement = buildProcedureCall(schemaName, catalogName, procedureName, inParams.size(), outParams.size());

            try (Connection connection = dataSource.getConnection();
                 CallableStatement callableStatement = connection.prepareCall(callStatement)) {


 // Attempt to safely cast inParams and outParams
            Map<Integer, String> inParams = castToIntegerStringMap(requestBody.get("inParams"));
            Map<Integer, String> outParams = castToIntegerStringMap(requestBody.get("outParams"));

               // Set IN parameters with appropriate type conversion
                for (Map.Entry<Integer, String> entry : inParams.entrySet()) {
                    Integer paramIndex = entry.getKey();
                    String paramValue = entry.getValue();
// Attempt to determine the appropriate type for the value
            if (isInteger(value)) {
                convertedInParams.put(key, Integer.parseInt(value));
            } else if (isDouble(value)) {
                convertedInParams.put(key, Double.parseDouble(value));
            } else if (isDate(value)) {
                try {
                    Date dateValue = DATE_FORMAT.parse(value);
                    convertedInParams.put(key, new java.sql.Timestamp(dateValue.getTime()));
                } catch (ParseException e) {
                    throw new IllegalArgumentException("Invalid date format for key " + key + ": " + value);
                }
            } else {
                convertedInParams.put(key, value);  // Default to treating it as a String
            }
                }

                for (Map.Entry<Integer, Integer> entry : outParams.entrySet()) {
                    callableStatement.registerOutParameter(entry.getKey(), entry.getValue());
                }

                callableStatement.execute();

                Map<String, Object> resultMap = new HashMap<>();
                for (Map.Entry<Integer, Integer> entry : outParams.entrySet()) {
                    resultMap.put("OUT_PARAM_" + entry.getKey(), callableStatement.getObject(entry.getKey()));
                }

                return resultMap;

            } catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException("Error executing procedure", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String buildProcedureCall(String schemaName, String catalogName, String procedureName, int inParamsCount, int outParamsCount) {
        StringBuilder call = new StringBuilder();
        call.append("{call ");

        if (schemaName != null && !schemaName.isEmpty()) {
            call.append(schemaName).append(".");
        }

        if (catalogName != null && !catalogName.isEmpty()) {
            call.append(catalogName).append(".");
        }

        call.append(procedureName).append("(");

        int totalParams = inParamsCount + outParamsCount;
        for (int i = 0; i < totalParams; i++) {
            if (i > 0) {
                call.append(", ");
            }
            call.append("?");
        }

        call.append(")}");
        return call.toString();
    }
	
	
	
	
	---------------------------------------------------------------------
	
	
	public Mono<Map<String, Object>> executeProcedure(Map<String, Object> requestBody) {
        return Mono.fromCallable(() -> {
            String schemaName = (String) requestBody.get("schemaName");
            String catalogName = (String) requestBody.get("catalogName");
            String procedureName = (String) requestBody.get("procedureName");

            Map<String, String> inParams = (Map<String, String>) requestBody.get("inParams");
            Map<String, String> outParams = (Map<String, String>) requestBody.get("outParams");

            String callStatement = buildProcedureCall(schemaName, catalogName, procedureName, inParams.size(), outParams.size());

            try (Connection connection = dataSource.getConnection();
                 CallableStatement callableStatement = connection.prepareCall(callStatement)) {

                // Set IN parameters
                int index = 1;
                for (Map.Entry<String, String> entry : inParams.entrySet()) {
                    String value = entry.getValue();
                    callableStatement.setObject(index, convertToJDBCType(value));
                    index++;
                }

                // Register OUT parameters
                index = inParams.size() + 1;
                for (Map.Entry<String, String> entry : outParams.entrySet()) {
                    String sqlType = entry.getValue();
                    callableStatement.registerOutParameter(index, convertSQLType(sqlType));
                    index++;
                }

                // Execute the procedure
                callableStatement.execute();

                // Retrieve OUT parameters
                Map<String, Object> resultMap = new HashMap<>();
                index = inParams.size() + 1;
                for (String paramName : outParams.keySet()) {
                    resultMap.put(paramName, callableStatement.getObject(index));
                    index++;
                }

                return resultMap;

            } catch (SQLException e) {
                e.printStackTrace(); // Replace with proper logging
                throw new RuntimeException("Error executing procedure", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String buildProcedureCall(String schemaName, String catalogName, String procedureName, int inParamsCount, int outParamsCount) {
        StringBuilder call = new StringBuilder();
        call.append("{call ");

        if (schemaName != null && !schemaName.isEmpty()) {
            call.append(schemaName).append(".");
        }

        if (catalogName != null && !catalogName.isEmpty()) {
            call.append(catalogName).append(".");
        }

        call.append(procedureName).append("(");

        int totalParams = inParamsCount + outParamsCount;
        for (int i = 0; i < totalParams; i++) {
            if (i > 0) {
                call.append(", ");
            }
            call.append("?");
        }

        call.append(")}");
        return call.toString();
    }

    private Object convertToJDBCType(String value) {
        // Simple conversion logic based on the value format
        if (value.matches("-?\\d+")) {
            return Integer.parseInt(value);  // Convert to Integer
        } else if (value.matches("-?\\d+(\\.\\d+)?")) {
            return Double.parseDouble(value);  // Convert to Double
        } else {
            return value;  // Treat as String
        }
    }

    private int convertSQLType(String sqlType) {
        switch (sqlType.toUpperCase()) {
            case "VARCHAR":
                return Types.VARCHAR;
            case "NUMERIC":
                return Types.NUMERIC;
            case "INTEGER":
                return Types.INTEGER;
            case "FLOAT":
                return Types.FLOAT;
            case "DOUBLE":
                return Types.DOUBLE;
            case "DATE":
                return Types.DATE;
            default:
                throw new IllegalArgumentException("Unsupported SQL type: " + sqlType);
        }
		
		
		
		=====================
		
		public Mono<Map<String, Object>> executeProcedure(Map<String, Object> requestBody) {
        return Mono.fromCallable(() -> {
            String schemaName = (String) requestBody.get("schemaName");
            String catalogName = (String) requestBody.get("catalogName");
            String procedureName = (String) requestBody.get("procedureName");

            Map<String, String> inParams = (Map<String, String>) requestBody.get("inParams");
            Map<String, String> outParams = (Map<String, String>) requestBody.get("outParams");

            String callStatement = buildProcedureCall(schemaName, catalogName, procedureName, inParams.size(), outParams.size());

            try (Connection connection = dataSource.getConnection();
                 CallableStatement callableStatement = connection.prepareCall(callStatement)) {

                // Set IN parameters
                int index = 1;
                for (Map.Entry<String, String> entry : inParams.entrySet()) {
                    callableStatement.setString(index, entry.getValue());
                    index++;
                }

                // Register OUT parameters (as Strings)
                index = inParams.size() + 1;
                for (Map.Entry<String, String> entry : outParams.entrySet()) {
                    callableStatement.registerOutParameter(index, java.sql.Types.VARCHAR);
                    index++;
                }

                // Execute the procedure
                callableStatement.execute();

                // Retrieve OUT parameters
                Map<String, Object> resultMap = new HashMap<>();
                index = inParams.size() + 1;
                for (String paramName : outParams.keySet()) {
                    resultMap.put(paramName, callableStatement.getString(index));
                    index++;
                }

                return resultMap;

            } catch (SQLException e) {
                e.printStackTrace(); // Replace with proper logging
                throw new RuntimeException("Error executing procedure", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String buildProcedureCall(String schemaName, String catalogName, String procedureName, int inParamsCount, int outParamsCount) {
        StringBuilder call = new StringBuilder();
        call.append("{call ");

        if (schemaName != null && !schemaName.isEmpty()) {
            call.append(schemaName).append(".");
        }

        if (catalogName != null && !catalogName.isEmpty()) {
            call.append(catalogName).append(".");
        }

        call.append(procedureName).append("(");

        int totalParams = inParamsCount + outParamsCount;
        for (int i = 0; i < totalParams; i++) {
            if (i > 0) {
                call.append(", ");
            }
            call.append("?");
        }

        call.append(")}");
        return call.toString();
    }
    
	
	public Mono<Map<String, Object>> executeProcedure(Map<String, Object> requestBody) {
        return Mono.fromCallable(() -> {
            String schemaName = (String) requestBody.get("schemaName");
            String catalogName = (String) requestBody.get("catalogName");
            String procedureName = (String) requestBody.get("procedureName");

            Map<String, String> inParams = (Map<String, String>) requestBody.get("inParams");
            Map<String, String> outParams = (Map<String, String>) requestBody.get("outParams");

            String callStatement = buildProcedureCall(schemaName, catalogName, procedureName, inParams.size(), outParams.size());

            try (Connection connection = dataSource.getConnection();
                 CallableStatement callableStatement = connection.prepareCall(callStatement)) {

                // Set IN parameters
                int index = 1;
                for (Map.Entry<String, String> entry : inParams.entrySet()) {
                   callableStatement.setObject(index, convertToObject(entry.getValue()));
                    index++;
                }

                // Register OUT parameters with default SQL type (e.g., VARCHAR)
                index = inParams.size() + 1;
                for (Map.Entry<String, String> entry : outParams.entrySet()) {
                    callableStatement.registerOutParameter(index, Types.VARCHAR);
                    index++;
                }

                // Execute the procedure
                callableStatement.execute();

                // Retrieve OUT parameters
                Map<String, Object> resultMap = new HashMap<>();
                index = inParams.size() + 1;
                for (String paramName : outParams.keySet()) {
                    resultMap.put(paramName, callableStatement.getString(index));
                    index++;
                }

                return resultMap;

            } catch (SQLException e) {
                e.printStackTrace(); // Replace with proper logging
                throw new RuntimeException("Error executing procedure", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
	
	private Object convertToObject(String value) {
        // Simple type conversion logic; this can be expanded
        try {
            return Integer.parseInt(value); // Attempt to convert to Integer
        } catch (NumberFormatException e) {
            // If it's not an integer, return as a String
            return value;
        }
    }
}
