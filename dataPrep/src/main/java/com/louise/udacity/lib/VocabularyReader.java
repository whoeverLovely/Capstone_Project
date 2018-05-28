package com.louise.udacity.lib;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class VocabularyReader {

    public final static String TAG_TOEFL = "toefl";
    public final static String TAG_IELTS = "ielts";
    public final static String TAG_GRE = "gre";

    // Read vocabulary data from database and output TOEFL, ITELS and GRE lists in protoco
    public static void prepLists(String tag) {
        com.louise.udacity.lib.VocabularyProtos.Vocabulary.Builder vocabularyBuilder
                = com.louise.udacity.lib.VocabularyProtos.Vocabulary.newBuilder();

        com.louise.udacity.lib.VocabularyProtos.Vocabularies.Builder vocabulariesBuilder
                = com.louise.udacity.lib.VocabularyProtos.Vocabularies.newBuilder();
        Connection conn = null;

        try {
            conn = SqliteConnection.connect();

            PreparedStatement statement = null;
            if (tag != null) {
                String query_1 = "select word, phonetic, definition, translation from dict_all where tag like ?";
                statement = conn.prepareStatement(query_1);
                statement.setString(1, tag);
            } else {
                String query_2 = "select word, phonetic, definition, translation from dict_all";
                statement = conn.prepareStatement(query_2);
            }

            ResultSet rs = statement.executeQuery();

            if (!rs.isBeforeFirst()) {
                System.out.println("No data");
            }

            int count = 0;
            while (rs.next()) {
                count++;
                String word = rs.getString(1);
                String phonetic = rs.getString(2);
                String definition = rs.getString(3);
                String translation = rs.getString(4);

                vocabularyBuilder.setWord(word);
                vocabularyBuilder.setPhonetic(phonetic);
                vocabularyBuilder.setDefinition(definition);
                vocabularyBuilder.setTranslation(translation);

                com.louise.udacity.lib.VocabularyProtos.Vocabulary vocabulary = vocabularyBuilder.build();
                vocabulariesBuilder.addVocabulary(vocabulary);
            }
            OutputStream stream = new DataOutputStream(new FileOutputStream(tag + "_list"));
            vocabulariesBuilder.build().writeTo(stream);

            System.out.println("Vocabulary no of " + tag + " is " + count);
        } catch (SQLException | IOException e) {
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

    public static List<com.louise.udacity.lib.VocabularyProtos.Vocabulary> parseProto(InputStream inputStream) {

        List<com.louise.udacity.lib.VocabularyProtos.Vocabulary> vocabularyList = null;
        try {

            com.louise.udacity.lib.VocabularyProtos.Vocabularies vocabularies
                    = com.louise.udacity.lib.VocabularyProtos.Vocabularies.parseFrom(inputStream);

            vocabularyList = vocabularies.getVocabularyList();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (vocabularyList != null)
            System.out.println("The size of vocabulary list is " + vocabularyList.size());
        else
            System.out.println("The vocabulary list is empty.");
        return vocabularyList;

    }

    public static void main(String[] args) {

        prepLists(TAG_IELTS);
        prepLists(TAG_GRE);
        prepLists(TAG_TOEFL);

        /*InputStream inputStream = null;
        try {
            inputStream = new FileInputStream("gre_list");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        parseProto(inputStream);*/
    }
}
