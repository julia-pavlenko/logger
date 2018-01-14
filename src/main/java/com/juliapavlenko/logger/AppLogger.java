package com.juliapavlenko.logger;

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by julia on 14.01.18.
 */

public class AppLogger {

    private static String REPORT_FILE_PATH = "/report.txt";
    private static String DATE_TIME_FORMAT_LOG_TIME = "EEE, dd MMM yyyy HH:mm:ss z";
    private static String SHORT_DATE_FORMAT = "dd-MM-yyyy";
    private static String LOG_TIME_KEY = "Log time: ";
    private static String CAPTURED_TRANSACTIONS_KEY = "Captured transactions:";
    private static String ASSET_ID_KEY = "Asset id:";
    private static long TIME_1_DAYS_IM_MS = 24 * 60 * 60 * 1000; // 1 day
    private static long TIME_13_DAYS_IM_MS = 13 * TIME_1_DAYS_IM_MS; // 13 days

    private final String logFolder;
    private Map<Long, DayLogsData> dateLog;
    private boolean isReadIdSection = false;

    public static void main(String[] args) {
        String logFolder = args[0];
        AppLogger appLogger = new AppLogger(logFolder);
        appLogger.execute();
    }

    public void execute() {
        parseLogsFolder();
        if (dateLog == null) {
            throw new Error(String.format("The folder \"%s\" does not contain any log files", logFolder));
        }
        analyzeLogs();
    }

    public AppLogger(String logFolder) {
        this.logFolder = logFolder;
    }

    private void parseLogsFolder() {
        File logFolderFile = new File(logFolder);
        if (logFolderFile.exists() && logFolderFile.isDirectory()) {
            for (final File file : logFolderFile.listFiles()) {
                if (file.isFile() && file.getName().startsWith("transactionsLog_")) {
                    if (dateLog == null) {
                        dateLog = new TreeMap<Long, DayLogsData>();
                    }
                    parseLogFile(file);
                }
            }
        } else {
            throw new Error(String.format("Directory \"%s\" not found", logFolder));
        }
    }

    private void parseLogFile(File logFile) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), "UTF-8"));
            String line;
            DayLogsData dayLogsData = null;
            while ((line = br.readLine()) != null) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith(LOG_TIME_KEY)) {
                    isReadIdSection = false;
                    long date = parseDate(trimmedLine);
                    if (date != 0) {
                        if (dayLogsData != null && !dayLogsData.getIdsCount().isEmpty()) {
                            DayLogsData storedDateLog = dateLog.get(date);
                            if (storedDateLog != null) {
                                mergeDayLogIds(storedDateLog, dayLogsData);
                            } else {
                                dateLog.put(date, dayLogsData);
                            }
                        }
                        dayLogsData = new DayLogsData();
                        dayLogsData.setDate(date);
                        dayLogsData.setIdsCount(new TreeMap<String, Integer>());
                    }
                } else if (dayLogsData != null) {
                    processDayLogLine(dayLogsData, trimmedLine);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void mergeDayLogIds(DayLogsData storedDateLog, DayLogsData dayLogsData) {
        for (String id : dayLogsData.getIdsCount().keySet()) {
            if (storedDateLog.getIdsCount().containsKey(id)) {
                storedDateLog.getIdsCount().put(id, storedDateLog.getIdsCount().get(id) + dayLogsData.getIdsCount().get(id));
            } else {
                storedDateLog.getIdsCount().put(id, dayLogsData.getIdsCount().get(id));
            }
        }
    }

    private long parseDate(String line) {
        String clearDate = line.substring(LOG_TIME_KEY.length());
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT_LOG_TIME, Locale.US);
        try {
            Date date = dateFormat.parse(clearDate);
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            return c.getTimeInMillis();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void processDayLogLine(DayLogsData dayLogsData, String line) {
        if (line.equals("")) {
            return;
        }
        if (isReadIdSection) {
            if (line.contains(ASSET_ID_KEY) || line.equalsIgnoreCase("none")) {
                isReadIdSection = false;
            } else {
                String[] idArray = line.split(",");
                for (String id : idArray) {
                    if (dayLogsData.getIdsCount().containsKey(id)) {
                        int count = dayLogsData.getIdsCount().get(id) + 1;
                        dayLogsData.getIdsCount().put(id, count);
                    } else {
                        dayLogsData.getIdsCount().put(id, 1);
                    }
                }
            }
        } else if (line.startsWith(CAPTURED_TRANSACTIONS_KEY)) {
            isReadIdSection = true;
        }
    }

    private void analyzeLogs() {
        TreeMap<Long, TwoWeekReportData> reportData = new TreeMap<Long, TwoWeekReportData>();
        for (Long date : dateLog.keySet()) {
            if (reportData.isEmpty()) {
                TwoWeekReportData reportDataItem = new TwoWeekReportData();
                reportDataItem.setDate(date);
                reportDataItem.setEndDate(date + TIME_13_DAYS_IM_MS);
                reportDataItem.setIdsCount(new HashMap<String, Integer>());
                mergeDayLogIds(reportDataItem, dateLog.get(date));
                reportData.put(date, reportDataItem);
            } else {
                long startDate = reportData.firstKey();
                long endDate = startDate + TIME_13_DAYS_IM_MS;
                while (date > endDate) {
                    startDate = endDate + TIME_1_DAYS_IM_MS;
                    endDate = startDate + TIME_13_DAYS_IM_MS;
                }
                if (reportData.containsKey(startDate)) {
                    TwoWeekReportData reportDataItem = reportData.get(startDate);
                    mergeDayLogIds(reportDataItem, dateLog.get(date));
                } else {
                    TwoWeekReportData reportDataItem = new TwoWeekReportData();
                    reportDataItem.setDate(startDate);
                    reportDataItem.setEndDate(endDate);
                    reportDataItem.setIdsCount(new HashMap<String, Integer>());
                    mergeDayLogIds(reportDataItem, dateLog.get(date));
                    reportData.put(date, reportDataItem);
                }
            }
        }
        generateReport(reportData);
    }

    private void generateReport(Map<Long, TwoWeekReportData> reportData) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(SHORT_DATE_FORMAT);
        StringBuilder reportText = new StringBuilder();
        for (Long date : reportData.keySet()) {
            TwoWeekReportData reportDataItem = reportData.get(date);
            String startDateString = dateFormat.format(new Date(date));
            String endDateString = dateFormat.format(reportDataItem.getEndDate());
            reportText.append(String.format("%s - %s", startDateString, endDateString)).append("\n");
            List<String> idCountList = new ArrayList<String>();
            for (String id : reportDataItem.getIdsCount().keySet()) {
                int count = reportDataItem.getIdsCount().get(id);
                idCountList.add(String.format("%s:%d", id, count));
            }
            String idCountString = StringUtils.join(idCountList, ", ");
            reportText.append(idCountString).append("\n\n");
        }
        saveReportToFile(reportText.toString());
    }

    private void saveReportToFile(String reportText) {
        try{
            PrintWriter writer = new PrintWriter(logFolder + REPORT_FILE_PATH, "UTF-8");
            writer.println(reportText);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private static class DayLogsData {
        private long date;
        private Map<String, Integer> idsCount;

        public long getDate() {
            return date;
        }

        public void setDate(long date) {
            this.date = date;
        }

        public Map<String, Integer> getIdsCount() {
            return idsCount;
        }

        public void setIdsCount(Map<String, Integer> idsCount) {
            this.idsCount = idsCount;
        }
    }


    private static class TwoWeekReportData extends DayLogsData {
        private long endDate;

        public long getEndDate() {
            return endDate;
        }

        public void setEndDate(long endDate) {
            this.endDate = endDate;
        }
    }
}
