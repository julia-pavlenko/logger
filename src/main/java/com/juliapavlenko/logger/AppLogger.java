package com.juliapavlenko.logger;

/**
 * Created by julia on 14.01.18.
 */

public class AppLogger {

    private static String REPORT_FILE_PATH = "report/report.txt";

    private final String logFolder;

    // data

    public static void main(String[] args) {

        String logFolder = args[0];
        AppLogger appLogger = new AppLogger(logFolder);
        appLogger.parseLogsFolder();
        System.out.println(System.getProperty("simple.message") + args[0] + " from Simple.");
    }

    public AppLogger(String logFolder) {
        this.logFolder = logFolder;
    }

    public void parseLogsFolder() {

    }

    private void parseLogFile(String logFile) {

    }

    private void analyzeLogs() {

        generateReport();
    }

    private void generateReport() {
        // generate report in file logFolder + '/report/report.txt'
    }









}
