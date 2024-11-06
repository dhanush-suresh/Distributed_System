package com.grepclient;

import static org.junit.Assert.assertEquals;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.junit.Test;

public class AppTest 
{

    @Test
    public void shouldCheckOneFilePattern() {
        String mockInput = "grep \"ONE_FILE\"";
        ByteArrayInputStream is = new ByteArrayInputStream(mockInput.getBytes());
        System.setIn(is);
        // App app = new App();
        App.main(new String[]{});
        //Check output dir if there are 4 files
        File outputDir = new File("./output");
        File[] files = outputDir.listFiles();
        System.out.println("files: " + files.length);
        assertEquals(1, files.length);
        int totalCount = 0;
        for (File file : files) {
            if (file.isFile()) {
                totalCount += getLineCount(file);
            }
        }
        File assertFile = new File("../logger/output_one_file.txt");
        assertEquals(totalCount, getLineCount(assertFile));
        clearOutputDir(outputDir);
    }

    // Distributed unit test for 
    @Test
    public void shouldCheckFrequentPattern() {
        String mockInput = "grep \"FREQUENT\"";
        ByteArrayInputStream is = new ByteArrayInputStream(mockInput.getBytes());
        System.setIn(is);
        // App app = new App();
        App.main(new String[]{});
        //Check output dir if there are 4 files
        File outputDir = new File("./output");
        File[] files = outputDir.listFiles();
        System.out.println("files: " + files.length);
        assertEquals(4, files.length);
        int totalCount = 0;
        for (File file : files) {
            if (file.isFile()) {
                totalCount += getLineCount(file);
            }
        }
        File assertFile = new File("../logger/output_frequent.txt");
        assertEquals(totalCount, getLineCount(assertFile));
        clearOutputDir(outputDir);
    }

    @Test
    public void shouldCheckRarePattern() {
        String mockInput = "grep \"RARE\"";
        ByteArrayInputStream is = new ByteArrayInputStream(mockInput.getBytes());
        System.setIn(is);
        // App app = new App();
        App.main(new String[]{});
        //Check output dir if there are 4 files
        File outputDir = new File("./output");
        File[] files = outputDir.listFiles();
        System.out.println("files: " + files.length);
        assertEquals(4, files.length);
        int totalCount = 0;
        for (File file : files) {
            if (file.isFile()) {
                totalCount += getLineCount(file);
            }
        }
        File assertFile = new File("../logger/output_rare.txt");
        assertEquals(totalCount, getLineCount(assertFile));
        clearOutputDir(outputDir);
    }

    @Test
    public void shouldCheckPartialFrequentPattern() {
        String mockInput = "grep \"SOMEWHAT_FREQUENT\"";
        ByteArrayInputStream is = new ByteArrayInputStream(mockInput.getBytes());
        System.setIn(is);
        // App app = new App();
        App.main(new String[]{});
        //Check output dir if there are 4 files
        File outputDir = new File("./output");
        File[] files = outputDir.listFiles();
        System.out.println("files: " + files.length);
        assertEquals(4, files.length);
        int totalCount = 0;
        for (File file : files) {
            if (file.isFile()) {
                totalCount += getLineCount(file);
            }
        }
        File assertFile = new File("../logger/output_somewhat_frequent.txt");
        assertEquals(totalCount, getLineCount(assertFile));
        clearOutputDir(outputDir);
    }

    @Test
    public void shouldCheckRegexPattern() {
        String mockInput = "grep \"^DEBUG\"";
        ByteArrayInputStream is = new ByteArrayInputStream(mockInput.getBytes());
        System.setIn(is);
        // App app = new App();
        App.main(new String[]{});
        //Check output dir if there are 4 files
        File outputDir = new File("./output");
        File[] files = outputDir.listFiles();
        System.out.println("files: " + files.length);
        assertEquals(4, files.length);
        int totalCount = 0;
        for (File file : files) {
            if (file.isFile()) {
                totalCount += getLineCount(file);
            }
        }
        File assertFile = new File("../logger/output_regex.txt");
        assertEquals(totalCount, getLineCount(assertFile));
        clearOutputDir(outputDir);
    }

    private static int getLineCount(File file) {
        int lineCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (reader.readLine() != null) {
                lineCount++;
            }
        } catch (IOException e) {
            System.out.println("An error occurred while reading the file: " + file.getName());
            e.printStackTrace();
        }
        return lineCount;
    }

    private static void clearOutputDir(File dir) {
        for(File file: dir.listFiles()) {
            if (!file.isDirectory()) 
            file.delete();
        }
    }
}
