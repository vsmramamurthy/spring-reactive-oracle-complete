import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RequestConverter {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH.mm");

    public static Map<Integer, Object> convertInParams(Map<String, String> inParams) {
        Map<Integer, Object> convertedInParams = new HashMap<>();

        for (Map.Entry<String, String> entry : inParams.entrySet()) {
            Integer key = Integer.parseInt(entry.getKey());  // Convert the key from String to Integer
            String value = entry.getValue();

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

        return convertedInParams;
    }

    public static Map<Integer, String> convertOutParams(Map<String, String> outParams) {
        Map<Integer, Integer> convertedOutParams = new HashMap<>();

        for (Map.Entry<String, String> entry : outParams.entrySet()) {
            Integer key = Integer.parseInt(entry.getKey());
            String value = entry.getValue().toUpperCase();

            switch (value) {
                case "NUMBER":
                    convertedOutParams.put(key, Types.NUMERIC);
                    break;
                case "VARCHAR":
                    convertedOutParams.put(key, Types.VARCHAR);
                    break;
                case "CURSOR":
                    convertedOutParams.put(key, OracleTypes.CURSOR);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported OracleType for key " + key + ": " + value);
            }
        }

        return convertedOutParams;
   }

    private static boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
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
}
