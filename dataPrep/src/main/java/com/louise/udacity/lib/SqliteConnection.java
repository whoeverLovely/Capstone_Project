package com.louise.udacity.lib;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqliteConnection {

    static final private String dbPath = "jdbc:sqlite:my_dict.db";


    /**
     * Connect to a database
     */
    public static Connection connect() {
        Connection conn = null;
        try {
            // create a connection to the database
            conn = DriverManager.getConnection(dbPath);

            System.out.println("Connection to SQLite has been established.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Connection conn = connect();
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            String query = "select * from dict_all where word = 'hello'";

            ResultSet rs = stmt.executeQuery(query);

            if (!rs.isBeforeFirst()) {
                System.out.println("No data");
            }

            while (rs.next()) {
                String word = rs.getString(1);
                String phonetic = rs.getString(2);
                String definition = rs.getString(3);
                String translation = rs.getString(4);

                System.out.println("-----------------------" + word + phonetic + definition + translation + "----------------");

                rs.close();
                stmt.close();
                if (conn != null) {
                    conn.close();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
