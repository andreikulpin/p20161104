package com.kulpin.p20161104;

import java.math.BigDecimal;
import java.sql.*;

public class DBHelper {

    private static final String URL = "jdbc:sqlserver://localhost;databaseName=Costs;integratedSecurity=true;";

    private static Connection connection;
    public static boolean isTableExist = false;

    public static void saveData(BigDecimal cost, float supplierRate, BigDecimal priceCost){
        connection = null;
        try{
            connectToDB();
            if (!isTableExist) createTable();

            Statement statement = connection.createStatement();

            String query = "INSERT Total_Cost(Cost, Supplier_Rate, Price_Cost) VALUES " +
                    "(" + cost + ", " + supplierRate + ", " + priceCost + ")";
            statement.execute(query);

            if (statement != null) statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void connectToDB() throws SQLException{

        connection = DriverManager.getConnection(URL);
        if (connection != null) {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getTables(null, null, "%", null);
            while (resultSet.next()) {
                if (resultSet.getString(3).equals("Total_Cost")) isTableExist = true;
            }

            if (resultSet != null) resultSet.close();
        }
    }

    private static void createTable() throws SQLException{
        Statement statement = connection.createStatement();

        String query = "CREATE TABLE Total_Cost(" +
                "Cost float NOT NULL, " +
                "Supplier_Rate float NOT NULL, " +
                "Price_Cost float NOT NULL" +
                ")";
        statement.execute(query);

        if (statement != null) statement.close();
    }
}
