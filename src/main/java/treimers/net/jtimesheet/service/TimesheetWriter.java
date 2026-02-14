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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import treimers.net.jtimesheet.model.Activity;

public class TimesheetWriter {
    /** Variable markers in Excel templates for layout discovery. */
    private static final String VAR_MONTH = "$month";
    private static final String VAR_START = "$start";
    private static final String VAR_END = "$end";
    private static final String VAR_PAUSE = "$pause";
    private static final String VAR_TASK = "$task";

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
            Config resolvedConfig = resolveConfig(config, sheet, properties);
            writeMonthCell(workbook, sheet, resolvedConfig, monthStart);
            writeDayRows(sheet, resolvedConfig, monthStart, monthEnd, summaries);
            evaluateFormulas(workbook);
            workbook.setForceFormulaRecalculation(true);
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

    private void writeMonthCell(Workbook workbook, Sheet sheet, Config config, LocalDate monthStart) {
        Row row = getOrCreateRow(sheet, config.monthRow);
        Cell cell = getOrCreateCell(row, config.monthColumn);
        CellStyle existingStyle = cell.getCellStyle();
        Date date = Date.from(monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
        cell.setCellValue(date);
        CellStyle dateStyle = workbook.createCellStyle();
        dateStyle.cloneStyleFrom(existingStyle);
        dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy"));
        cell.setCellStyle(dateStyle);
    }

    private void writeDayRows(
        Sheet sheet,
        Config config,
        LocalDate monthStart,
        LocalDate monthEnd,
        Map<LocalDate, DaySummary> summaries
    ) {
        LocalDate date = monthStart;
        int rowIndex = 0;
        while (!date.isAfter(monthEnd)) {
            Row row = getOrCreateRow(sheet, config.dataRow + rowIndex);
            DaySummary summary = summaries.get(date);
            if (summary != null) {
                writeTimeCell(row, config.startColumn, summary.startTime);
                writeTimeCell(row, config.endColumn, summary.endTime);
                writePauseCell(row, config.pauseColumn, summary.pauseMinutes, config.rounding);
                Cell taskCell = getOrCreateCell(row, config.taskColumn);
                taskCell.setCellValue(summary.tasks);
            } else {
                clearCell(row, config.startColumn);
                clearCell(row, config.endColumn);
                clearCell(row, config.pauseColumn);
                clearCell(row, config.taskColumn);
            }
            rowIndex++;
            date = date.plusDays(1);
        }
    }

    private void clearCell(Row row, int columnIndex) {
        Cell cell = getOrCreateCell(row, columnIndex);
        cell.setCellType(Cell.CELL_TYPE_BLANK);
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
        double value;
        if (pauseMinutes <= 0) {
            value = 0.0; // 0:00
        } else {
            double hours = pauseMinutes / 60.0;
            if (rounding > 0) {
                hours = Math.round(hours * rounding) / rounding;
            }
            value = hours / 24.0;
        }
        Cell cell = getOrCreateCell(row, column);
        cell.setCellValue(value);
    }

    private void evaluateFormulas(Workbook workbook) {
        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
        evaluator.clearAllCachedResultValues();
        evaluator.evaluateAll();
    }

    /** Discovers layout from template variables ($month, $start, $end, $pause, $task). */
    private Config resolveConfig(Config config, Sheet sheet, Properties properties) {
        DiscoveredLayout discovered = discoverLayout(sheet);
        if (discovered != null && discovered.isComplete()) {
            return config.mergeWith(discovered);
        }
        throw new IllegalArgumentException(
            "Template does not contain required variables ($month, $start, $end, $pause, $task). " +
            "Add these markers to the Excel template.");
    }

    private DiscoveredLayout discoverLayout(Sheet sheet) {
        Integer monthRow = null;
        Integer monthCol = null;
        Integer dataRow = null;
        Integer startCol = null;
        Integer endCol = null;
        Integer pauseCol = null;
        Integer taskCol = null;

        int lastRow = sheet.getLastRowNum();
        for (int r = 0; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            int rowIdx = row.getRowNum();
            short lastCell = row.getLastCellNum();
            if (lastCell < 0) continue;
            for (short c = 0; c < lastCell; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) continue;
                String value = getCellStringValue(cell);
                if (value == null || value.isEmpty()) continue;
                if (value.contains(VAR_MONTH)) {
                    monthRow = rowIdx;
                    monthCol = cell.getColumnIndex();
                }
                if (value.contains(VAR_START)) {
                    dataRow = dataRow == null ? rowIdx : Math.min(dataRow, rowIdx);
                    startCol = cell.getColumnIndex();
                }
                if (value.contains(VAR_END)) {
                    dataRow = dataRow == null ? rowIdx : Math.min(dataRow, rowIdx);
                    endCol = cell.getColumnIndex();
                }
                if (value.contains(VAR_PAUSE)) {
                    dataRow = dataRow == null ? rowIdx : Math.min(dataRow, rowIdx);
                    pauseCol = cell.getColumnIndex();
                }
                if (value.contains(VAR_TASK)) {
                    dataRow = dataRow == null ? rowIdx : Math.min(dataRow, rowIdx);
                    taskCol = cell.getColumnIndex();
                }
            }
        }

        if (monthRow == null || dataRow == null || startCol == null
                || endCol == null || pauseCol == null || taskCol == null) {
            return null;
        }
        return new DiscoveredLayout(monthRow, monthCol, dataRow, startCol, endCol, pauseCol, taskCol);
    }

    private String getCellStringValue(Cell cell) {
        int type = cell.getCellType();
        if (type == Cell.CELL_TYPE_STRING) {
            return cell.getStringCellValue();
        }
        if (type == Cell.CELL_TYPE_FORMULA) {
            try {
                String formula = cell.getCellFormula();
                if (formula != null && !formula.isEmpty()) {
                    return formula;
                }
                int formulaResultType = cell.getCachedFormulaResultType();
                if (formulaResultType == Cell.CELL_TYPE_STRING) {
                    return cell.getStringCellValue();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static class DiscoveredLayout {
        final int monthRow, monthColumn, dataRow;
        final int startColumn, endColumn, pauseColumn, taskColumn;

        DiscoveredLayout(int monthRow, int monthColumn, int dataRow,
                         int startColumn, int endColumn, int pauseColumn, int taskColumn) {
            this.monthRow = monthRow;
            this.monthColumn = monthColumn;
            this.dataRow = dataRow;
            this.startColumn = startColumn;
            this.endColumn = endColumn;
            this.pauseColumn = pauseColumn;
            this.taskColumn = taskColumn;
        }

        boolean isComplete() {
            return true;
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
        private final Integer monthRow;
        private final Integer monthColumn;
        private final Integer dataRow;
        private final Integer startColumn;
        private final Integer endColumn;
        private final Integer pauseColumn;
        private final Integer taskColumn;
        private final double rounding;
        private final String taskSeparator;

        private Config(Properties properties) {
            sheetNo = getRequiredInt(properties, "target.sheetno");
            monthRow = null;
            monthColumn = null;
            dataRow = null;
            startColumn = null;
            endColumn = null;
            pauseColumn = null;
            taskColumn = null;
            rounding = getDouble(properties, "rounding", 0.0);
            taskSeparator = getString(properties, "target.task.separator", ", ");
        }

        private Config(int sheetNo, DiscoveredLayout layout, double rounding, String taskSeparator) {
            this.sheetNo = sheetNo;
            this.monthRow = layout.monthRow;
            this.monthColumn = layout.monthColumn;
            this.dataRow = layout.dataRow;
            this.startColumn = layout.startColumn;
            this.endColumn = layout.endColumn;
            this.pauseColumn = layout.pauseColumn;
            this.taskColumn = layout.taskColumn;
            this.rounding = rounding;
            this.taskSeparator = taskSeparator;
        }

        Config mergeWith(DiscoveredLayout discovered) {
            return new Config(sheetNo, discovered, rounding, taskSeparator);
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

        private static String getString(Properties properties, String key, String fallback) {
            String value = properties.getProperty(key);
            return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
        }
    }
}
