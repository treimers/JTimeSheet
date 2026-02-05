package treimers.net.jtimesheet.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import treimers.net.jtimesheet.model.Activity;

public class TimesheetWriter {
    private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public void writeTimesheet(
        Properties properties,
        Path templatePath,
        Path outputPath,
        List<Activity> activities,
        Function<String, String> customerResolver,
        Function<String, String> projectResolver,
        Function<String, String> taskResolver
    ) throws IOException {
        if (properties == null) {
            throw new IllegalArgumentException("Missing timesheet properties.");
        }
        Config config = new Config(properties);
        List<ActivityRecord> records = toRecords(activities, customerResolver, projectResolver, taskResolver);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("No activities to export.");
        }
        LocalDate monthStart = records.get(0).start.toLocalDate().withDayOfMonth(1);
        LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);
        Map<LocalDate, DaySummary> summaries = buildSummaries(records, monthStart, monthEnd, config.taskSeparator);

        try (
            InputStream inputStream = Files.newInputStream(templatePath);
            Workbook workbook = createWorkbook(inputStream)
        ) {
            Sheet sheet = workbook.getSheetAt(config.sheetNo);
            writeMonthCell(sheet, config, monthStart);
            writeDayRows(sheet, config, monthStart, monthEnd, summaries);
            if (config.evaluateFormulas) {
                evaluateFormulas(workbook);
            }
            try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
                workbook.write(outputStream);
            }
        }
    }

    private Workbook createWorkbook(InputStream inputStream) throws IOException {
        try {
            return WorkbookFactory.create(inputStream);
        } catch (InvalidFormatException exception) {
            throw new IOException("Invalid Excel template format.", exception);
        }
    }

    private void writeMonthCell(Sheet sheet, Config config, LocalDate monthStart) {
        Row row = getOrCreateRow(sheet, config.monthRow);
        Cell cell = getOrCreateCell(row, config.monthColumn);
        Date date = Date.from(monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
        cell.setCellValue(date);
    }

    private void writeDayRows(
        Sheet sheet,
        Config config,
        LocalDate monthStart,
        LocalDate monthEnd,
        Map<LocalDate, DaySummary> summaries
    ) {
        DateTimeFormatter dateFormatter = config.dateFormat != null
            ? DateTimeFormatter.ofPattern(config.dateFormat)
            : DEFAULT_DATE_FORMAT;
        LocalDate date = monthStart;
        int rowIndex = 0;
        while (!date.isAfter(monthEnd)) {
            Row row = getOrCreateRow(sheet, config.dataRow + rowIndex);
            Cell dateCell = getOrCreateCell(row, config.dateColumn);
            dateCell.setCellValue(dateFormatter.format(date));
            DaySummary summary = summaries.get(date);
            if (summary != null) {
                writeTimeCell(row, config.startColumn, summary.startTime);
                writeTimeCell(row, config.endColumn, summary.endTime);
                writePauseCell(row, config.pauseColumn, summary.pauseMinutes, config.rounding);
                Cell taskCell = getOrCreateCell(row, config.taskColumn);
                taskCell.setCellValue(summary.tasks);
            }
            rowIndex++;
            date = date.plusDays(1);
        }
    }

    private void writeTimeCell(Row row, int column, LocalTime time) {
        if (time == null) {
            return;
        }
        double value = (time.getHour() + time.getMinute() / 60.0) / 24.0;
        Cell cell = getOrCreateCell(row, column);
        cell.setCellValue(value);
    }

    private void writePauseCell(Row row, int column, long pauseMinutes, double rounding) {
        if (pauseMinutes <= 0) {
            return;
        }
        double hours = pauseMinutes / 60.0;
        if (rounding > 0) {
            hours = Math.round(hours * rounding) / rounding;
        }
        double value = hours / 24.0;
        Cell cell = getOrCreateCell(row, column);
        cell.setCellValue(value);
    }

    private void evaluateFormulas(Workbook workbook) {
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
            Sheet sheet = workbook.getSheetAt(sheetNum);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
                        evaluator.evaluateFormulaCell(cell);
                    }
                }
            }
        }
    }

    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row != null ? row : sheet.createRow(rowIndex);
    }

    private Cell getOrCreateCell(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        return cell != null ? cell : row.createCell(columnIndex);
    }

    private List<ActivityRecord> toRecords(
        List<Activity> activities,
        Function<String, String> customerResolver,
        Function<String, String> projectResolver,
        Function<String, String> taskResolver
    ) {
        List<ActivityRecord> records = new ArrayList<>();
        for (Activity activity : activities) {
            LocalDateTime start = Activity.parseStoredDateTime(activity.getFrom());
            LocalDateTime end = Activity.parseStoredDateTime(activity.getTo());
            if (start == null || end == null) {
                continue;
            }
            String task = safeName(taskResolver.apply(activity.getTaskId()));
            String label = task;
            records.add(new ActivityRecord(start, end, label));
        }
        records.sort((left, right) -> left.start.compareTo(right.start));
        return records;
    }

    private String safeName(String value) {
        return value == null ? "" : value.trim();
    }

    private Map<LocalDate, DaySummary> buildSummaries(
        List<ActivityRecord> records,
        LocalDate monthStart,
        LocalDate monthEnd,
        String taskSeparator
    ) {
        Map<LocalDate, List<ActivityRecord>> grouped = new TreeMap<>();
        for (ActivityRecord record : records) {
            LocalDate date = record.start.toLocalDate();
            if (date.isBefore(monthStart) || date.isAfter(monthEnd)) {
                continue;
            }
            grouped.computeIfAbsent(date, value -> new ArrayList<>()).add(record);
        }
        Map<LocalDate, DaySummary> summaries = new TreeMap<>();
        for (Map.Entry<LocalDate, List<ActivityRecord>> entry : grouped.entrySet()) {
            List<ActivityRecord> dayRecords = entry.getValue();
            LocalTime start = null;
            LocalTime end = null;
            Duration total = Duration.ZERO;
            Set<String> tasks = new LinkedHashSet<>();
            for (ActivityRecord record : dayRecords) {
                LocalTime recordStart = record.start.toLocalTime();
                LocalTime recordEnd = record.end.toLocalTime();
                if (start == null || recordStart.isBefore(start)) {
                    start = recordStart;
                }
                if (end == null || recordEnd.isAfter(end)) {
                    end = recordEnd;
                }
                if (record.end.isAfter(record.start)) {
                    total = total.plus(Duration.between(record.start, record.end));
                }
                if (!record.taskLabel.isBlank()) {
                    tasks.add(record.taskLabel);
                }
            }
            long pauseMinutes = 0;
            if (start != null && end != null) {
                Duration span = Duration.between(start, end);
                long spanMinutes = span.toMinutes();
                long totalMinutes = total.toMinutes();
                if (spanMinutes > totalMinutes) {
                    pauseMinutes = spanMinutes - totalMinutes;
                }
            }
            summaries.put(entry.getKey(), new DaySummary(start, end, pauseMinutes, String.join(taskSeparator, tasks)));
        }
        return summaries;
    }

    private static class ActivityRecord {
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final String taskLabel;

        private ActivityRecord(LocalDateTime start, LocalDateTime end, String taskLabel) {
            this.start = start;
            this.end = end;
            this.taskLabel = taskLabel == null ? "" : taskLabel;
        }
    }

    private static class DaySummary {
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final long pauseMinutes;
        private final String tasks;

        private DaySummary(LocalTime startTime, LocalTime endTime, long pauseMinutes, String tasks) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.pauseMinutes = pauseMinutes;
            this.tasks = tasks == null ? "" : tasks;
        }
    }

    private static class Config {
        private final int sheetNo;
        private final int monthRow;
        private final int monthColumn;
        private final int dataRow;
        private final int dateColumn;
        private final int startColumn;
        private final int endColumn;
        private final int pauseColumn;
        private final int taskColumn;
        private final double rounding;
        private final String dateFormat;
        private final boolean evaluateFormulas;
        private final String taskSeparator;

        private Config(Properties properties) {
            sheetNo = getRequiredInt(properties, "target.sheetno");
            monthRow = getRequiredInt(properties, "target.month.row");
            monthColumn = getRequiredInt(properties, "target.month.column");
            dataRow = getRequiredInt(properties, "target.data.row");
            dateColumn = getRequiredInt(properties, "target.date.column");
            startColumn = getRequiredInt(properties, "target.start.column");
            endColumn = getRequiredInt(properties, "target.end.column");
            pauseColumn = getRequiredInt(properties, "target.pause.column");
            taskColumn = getRequiredInt(properties, "target.task.column");
            rounding = getDouble(properties, "rounding", 0.0);
            dateFormat = getString(properties, "target.date.format");
            evaluateFormulas = getBoolean(properties, "target.evaluate.formulas", true);
            taskSeparator = getString(properties, "target.task.separator", ", ");
        }

        private static int getRequiredInt(Properties properties, String key) {
            String value = properties.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException("Missing property: " + key);
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid integer for " + key + ": " + value);
            }
        }

        private static double getDouble(Properties properties, String key, double fallback) {
            String value = properties.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException exception) {
                return fallback;
            }
        }

        private static boolean getBoolean(Properties properties, String key, boolean fallback) {
            String value = properties.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }
            return Boolean.parseBoolean(value.trim());
        }

        private static String getString(Properties properties, String key) {
            String value = properties.getProperty(key);
            return value != null && !value.trim().isEmpty() ? value.trim() : null;
        }

        private static String getString(Properties properties, String key, String fallback) {
            String value = properties.getProperty(key);
            return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
        }
    }
}
