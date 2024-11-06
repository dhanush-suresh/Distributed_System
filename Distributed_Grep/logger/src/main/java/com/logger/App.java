package com.logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class App {
    
    // Define multiple keywords for each pattern
    private static final String[] frequentWords = {"FREQUENT_ERROR", "FREQUENT_WARNING", "FREQUENT_INFO"};
    private static final String[] rare_key_words = {"RARE_ERROR", "RARE_WARNING", "RARE_INFO"};
    private static final String[] less_frequent = {"SOMEWHAT_FREQUENT_ERROR", "SOMEWHAT_FREQUENT_WARNING", "SOMEWHAT_FREQUENT_INFO"};
    private static final String[] one_file = {"ONE_FILE_ONLY_ERROR", "ONE_FILE_ONLY_WARNING", "ONE_FILE_ONLY_INFO"};
    private static final String[] random_words = {"DEBUG", "TRACE", "NOTICE", "USER_ACTION", "LOGIN", "LOGOUT"};

    private static Random random = new Random();

    public static void main(String[] args) {
        int numberOfFiles = 4;  // The number of test files generated
        int linesPerFile = 1000;  // The number of line items per file
        String outputDirectory = "Outputs";
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();  // Create the directory if it doesn't exist
        }
        for (int i = 1; i <= numberOfFiles; i++) {
            String fileName = "Outputs/logfile_" + i + ".log";
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
                for (int j = 0; j < linesPerFile; j++) {
                    writer.write(generateLogLine(i));
                    writer.newLine();
                }
                System.out.println("Generated " + fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String generateLogLine(int fileIndex) {
        int chance = random.nextInt(100);
        if (chance < 50) {
            return getRandomFrequentKeyword() + " - This is a frequent line item";
        } else if (chance < 55) {
            return getRandomRareKeyword() + " - This is a rare line item";
        } else if (chance < 75) {
            return getRandomSomewhatFrequentKeyword() + " - This is a less frequent line item";
        } else if (fileIndex == 1 && chance < 80) {  // Only in the first file
            return getRandomOneFileKeyword() + " - This line item exists in only one of the file.";
        } else {
            return getRandomRandomWord() + " - This is a random log entry";
        }
    }

    private static String getRandomFrequentKeyword() {
        return frequentWords[random.nextInt(frequentWords.length)];
    }

    private static String getRandomRareKeyword() {
        return rare_key_words[random.nextInt(rare_key_words.length)];
    }

    private static String getRandomSomewhatFrequentKeyword() {
        return less_frequent[random.nextInt(less_frequent.length)];
    }

    private static String getRandomOneFileKeyword() {
        return one_file[random.nextInt(one_file.length)];
    }

    private static String getRandomRandomWord() {
        return random_words[random.nextInt(random_words.length)];
    }
}
