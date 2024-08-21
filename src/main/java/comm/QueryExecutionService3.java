public class QueryExecutionService {

    private final DataSource dataSource;

    public QueryExecutionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Mono<Map<String, Object>> executeProcedure(Map<String, Object> requestBody) {
        return Mono.fromCallable(() -> {
            String schemaName = (String) requestBody.get("schemaName");
            String catalogName = (String) requestBody.get("catalogName");
            String procedureName = (String) requestBody.get("procedureName");

            Map<Integer, Object> inParams = RequestConverter.convertInParams((Map<String, String>) requestBody.get("inParams"));
            Map<Integer, String> outParams = RequestConverter.convertOutParams((Map<String, String>) requestBody.get("outParams"));

            String callStatement = buildProcedureCall(schemaName, catalogName, procedureName, inParams.size(), outParams.size());

            try (Connection connection = dataSource.getConnection();
                 CallableStatement callableStatement = connection.prepareCall(callStatement)) {

                // Set IN parameters
                for (Map.Entry<Integer, Object> entry : inParams.entrySet()) {
                    callableStatement.setObject(entry.getKey(), entry.getValue());
                }

                // Register OUT parameters
                for (Map.Entry<Integer, String> entry : outParams.entrySet()) {
                    callableStatement.registerOutParameter(entry.getKey(), Types.VARCHAR); // Assuming VARCHAR for simplicity
                }

                // Execute the procedure
                callableStatement.execute();

                // Retrieve OUT parameters
                Map<String, Object> resultMap = new HashMap<>();
                for (Map.Entry<Integer, String> entry : outParams.entrySet()) {
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
}
