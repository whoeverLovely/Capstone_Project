package com.louise.udacity.firebasestore;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MyClass {

    static final private String dbPath = "jdbc:sqlite:my_dict.db";

    public static Firestore setUp() throws ExecutionException, InterruptedException {

        InputStream serviceAccount = null;
        GoogleCredentials credentials = null;
        try {
            serviceAccount = new FileInputStream("/home/yan/StudioProjects/MyDict/mydict-96553-firebase-adminsdk-sipkz-8eb0ad9011.json");
            credentials = GoogleCredentials.fromStream(serviceAccount);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(credentials)
                .build();
        FirebaseApp.initializeApp(options);

        Firestore db = FirestoreClient.getFirestore();

        return db;
    }

    public static void addTestData() throws ExecutionException, InterruptedException {
        Firestore db = setUp();
        DocumentReference docRef = db.collection("users").document("alovelace");
        // Add document data  with id "alovelace" using a hashmap
        Map<String, Object> data = new HashMap<>();
        data.put("first", "Ada");
        data.put("last", "Lovelace");
        data.put("born", 1815);
        //asynchronously write data
        ApiFuture<WriteResult> result = docRef.set(data);
        // ...
        // result.get() blocks on response
        System.out.println("Update time : " + result.get().getUpdateTime());

    }

    public static void addData(int start, int end) {
        Connection conn = null;

        try {
            // create a connection to the database
            conn = DriverManager.getConnection(dbPath);

            System.out.println("Connection to SQLite has been established.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        try {
            PreparedStatement statement = null;
            String query_2 = "select word, phonetic, definition, translation, bnc from dict_all where bnc=?";
            statement = conn.prepareStatement(query_2);

            int count = 0;
            Firestore db = setUp();
            for (int i = start; i<end; i++) {
                count++;
                statement.setInt(1, i);
                ResultSet rs = statement.executeQuery();
                if (!rs.isBeforeFirst()) {
                    System.out.println("No data");
                } else {
                    DocumentReference docRef = null;

                    rs.next();
                    String word = rs.getString(1);
                    String phonetic = rs.getString(2);
                    String definition = rs.getString(3);
                    String translation = rs.getString(4);
                    String bnc = rs.getString(5);

                    docRef = db.collection("vocabulary").document(word);
                    Map<String, Object> data = new HashMap<>();
                    data.put("phonetic", phonetic);
                    data.put("definition", definition);
                    data.put("translation", translation);
                    data.put("bnc", Integer.parseInt(bnc));
                    //asynchronously write data
                    ApiFuture<WriteResult> result = docRef.set(data);
                    // ...
                    // result.get() blocks on response
                    System.out.println("Update time : " + result.get().getUpdateTime());
                    System.out.println(word + bnc);

                }
            }

            System.out.println(count + " vocabularies added.");
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {

            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

   public static void readData(){
       Firestore db = null;
       try {
           db = setUp();
       } catch (ExecutionException e) {
           e.printStackTrace();
       } catch (InterruptedException e) {
           e.printStackTrace();
       }

       DocumentReference docRef = db.collection("vocabulary").document("hello");
// asynchronously retrieve the document
       ApiFuture<DocumentSnapshot> future = docRef.get();
// ...
// future.get() blocks on response
       DocumentSnapshot document = null;
       try {
           document = future.get();
       } catch (InterruptedException e) {
           e.printStackTrace();
       } catch (ExecutionException e) {
           e.printStackTrace();
       }
       if (document.exists()) {
           System.out.println("Document data: " + document.getData());
       } else {
           System.out.println("No such document!");
       }

   }

    public static void main(String[] args) {
        System.out.println("hello world");

        readData();
    }

}
