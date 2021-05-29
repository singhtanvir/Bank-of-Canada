package ca.bankofcanada;

import ca.bankofcanada.model.ExchangeRates;
import ca.bankofcanada.sql.DataSource;
import com.tunyk.currencyconverter.api.Currency;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;

/*
 * @author Tanvir Singh
 * @version 0.0.1
 *
 * This program downloads the FOREX data from Bank of Canada's official site and persists that into a specified data source.
 */
public class BankofCanada {

    final static String FILE_URL = "https://www.bankofcanada.ca/valet/observations/group/FX_RATES_DAILY/json";
    final static String FILE_NAME = "boc/FX_RATES_DAILY-2.json";
    private final static String TABLE_NAME = "boc_exchange_rates";

    /*
     * The main method is the starting point
     * Here is the utmost description of working of this method
     * The program first downloads the FOREX data from the specified URL
     * Then it saves that data into a file
     * Takes that data from the file parses it (i.e. makes it compatible with POJO)
     * Finally, persists all the data into the database
     */
    public static void main(String[] args) throws IOException, SQLException, ClassNotFoundException {
        downloadExchangeRates(FILE_URL, FILE_NAME);

        ArrayList<ExchangeRates> parsedObjects = parseJSONObject(FILE_NAME);

        if (checkIfDataExists(TABLE_NAME)) {
            updateUsingBatch(parsedObjects, TABLE_NAME);
        } else {
            insertUsingBatch(parsedObjects, TABLE_NAME);
        }
    }

    /*
     * Downloads the FOREX data from the given URL taken as an argument and stores the data in the file name also specified as the argument.
     */
    private static void downloadExchangeRates(String FILE_URL, String FILE_NAME) {
        try (BufferedInputStream in = new BufferedInputStream(new URL(FILE_URL).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(FILE_NAME)) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            System.err.println("Something went wrong while downloading the file, please check the logs");
            e.printStackTrace();
        }
    }

    /*
     * This method takes a JSON file specified by a filename and then it
     * Takes all the data from the file and converts it into a string
     * Remove all the unnecessary characters from that string
     * Finally returns an array list of an object containing rate and symbol.
     */
    private static ArrayList<ExchangeRates> parseJSONObject(String fileName) throws IOException {
        if (fileName != null) {
            ArrayList<ExchangeRates> exchangeRatesArrayList = new ArrayList<>();

            String data = convertFileToString(fileName);

            JSONObject lastIndex = cleanJsonObject(data);

            Set<String> stringSet = lastIndex.keySet();

            for (String key : stringSet) {
                String value = removeEverythingExceptFloat(lastIndex, key);

                exchangeRatesArrayList.add(new ExchangeRates(
                        removeUnnecessaryChars(key),
                        BigDecimal.ONE.divide(new BigDecimal(value), MathContext.DECIMAL128))
                );
            }

            exchangeRatesArrayList.add(new ExchangeRates("CAD", BigDecimal.ONE));

            return exchangeRatesArrayList;
        } else {
            System.err.println("Please enter a valid file name.");
        }
        return null;
    }

    private static String removeUnnecessaryChars(String key) {
        return key.replace("FX", "").replace("CAD", "");
    }

    private static String removeEverythingExceptFloat(JSONObject lastIndex, String key) {
        return lastIndex.get(key).toString().replaceAll("[^\\d.]+", "");
    }

    private static String convertFileToString(String fileName) throws IOException {
        byte[] list = Files.readAllBytes(Paths.get(fileName));

        String data = new String(list, StandardCharsets.UTF_8);

        return data;
    }

    private static JSONObject cleanJsonObject(String data) {
        JSONObject jsonObject = new JSONObject(data);

        JSONArray observations = (JSONArray) jsonObject.get("observations");

        JSONObject lastIndex = (JSONObject) observations.get(observations.length() - 1);

        lastIndex.remove("d");

        return lastIndex;   //Last index because it contains the latest data according to date
    }

    /*
     * Checks if the data already exists in the database returns a boolean value based on the evaluation.
     */
    private static boolean checkIfDataExists(String tableName) throws SQLException, ClassNotFoundException {
        String query = "SELECT * FROM " + tableName;

        PreparedStatement preparedStatement = DataSource.getInstance().getConnection().prepareStatement(
                query
        );

        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next() == true) {
            return true;
        }

        return false;
    }

    private static void insertUsingBatch(ArrayList<ExchangeRates> exchangeRatesArrayList, String tableName) throws SQLException, ClassNotFoundException {

        String insertQuery = "INSERT INTO " + tableName + "(symbol, rate) VALUES(?, ?)";

        PreparedStatement preparedStatement = DataSource.getInstance().getConnection().prepareStatement(
                insertQuery
        );

        for (ExchangeRates resultSet : exchangeRatesArrayList) {
            preparedStatement.setString(1, resultSet.getSymbol());
            preparedStatement.setBigDecimal(2, resultSet.getRate());
            preparedStatement.addBatch();
        }

        preparedStatement.executeBatch();
    }

    private static void updateUsingBatch(ArrayList<ExchangeRates> exchangeRatesArrayList, String tableName) throws SQLException, ClassNotFoundException {
        String updateQuery = "UPDATE " + tableName + " SET rate = ? WHERE symbol = ?";

        PreparedStatement preparedStatement = DataSource.getInstance().getConnection().prepareStatement(
                updateQuery
        );

        for (ExchangeRates resultSet : exchangeRatesArrayList) {
            preparedStatement.setBigDecimal(1, resultSet.getRate());
            preparedStatement.setString(2, resultSet.getSymbol());
            preparedStatement.addBatch();
        }

        preparedStatement.executeBatch();
    }
}
