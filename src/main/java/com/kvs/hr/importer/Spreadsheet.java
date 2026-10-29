package com.kvs.hr.importer;

import com.kvs.core.exception.BadRequestException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Reads bulk-import files (.xlsx, .xls or .csv) into rows keyed by column, and writes the .xlsx / .csv templates and the
 * error report. Columns are matched on their header text, ignoring case, spaces, punctuation and the "*" of mandatory columns.
 */
public final class Spreadsheet {

    public static final int MAX_ROWS = 5000;
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;

    /** A template column: header text, key, whether it must be filled, the instructions and an optional dropdown list. */
    public record Column(String header, String key, boolean required, String help, List<String> choices) {
        public String label() { return required ? header + " *" : header; }
    }

    /** One data row of the file: its row number in the sheet (header = row 1) and the values by column key. */
    public record Row(int rowNumber, Map<String, String> values) { }

    private Spreadsheet() { }

    public static String normalise(String header) {
        return header == null ? "" : header.toLowerCase(Locale.ROOT).replace("*", "").replaceAll("[^a-z0-9]", "");
    }

    /** The file's data rows; blank rows are skipped. Throws a readable 400 for unknown types, missing columns or too many rows. */
    public static List<Row> read(MultipartFile file, List<Column> columns) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Choose a file to upload", "IMPORT_FILE_REQUIRED");
        if (file.getSize() > MAX_FILE_BYTES) throw new BadRequestException("The file is larger than 10 MB. Split it into smaller files.", "IMPORT_FILE_TOO_LARGE");
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        List<List<String>> table;
        try (InputStream in = file.getInputStream()) {
            if (name.endsWith(".csv")) table = readCsv(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            else if (name.endsWith(".xlsx") || name.endsWith(".xls")) table = readWorkbook(in);
            else throw new BadRequestException("Upload an Excel (.xlsx) or CSV (.csv) file", "IMPORT_FILE_TYPE");
        } catch (IOException e) {
            throw new BadRequestException("The file could not be read: " + e.getMessage(), "IMPORT_FILE_UNREADABLE");
        } catch (RuntimeException e) {
            if (e instanceof BadRequestException) throw e;
            throw new BadRequestException("The file could not be read. Save it again as .xlsx or .csv and retry.", "IMPORT_FILE_UNREADABLE");
        }
        if (table.isEmpty()) throw new BadRequestException("The file is empty", "IMPORT_FILE_EMPTY");

        // header row → column keys
        List<String> header = table.get(0);
        Map<String, String> byHeader = new HashMap<>();
        columns.forEach(c -> byHeader.put(normalise(c.header()), c.key()));
        String[] keys = new String[header.size()];
        Set<String> found = new HashSet<>();
        for (int i = 0; i < header.size(); i++) {
            keys[i] = byHeader.get(normalise(header.get(i)));
            if (keys[i] != null) found.add(keys[i]);
        }
        List<String> missing = columns.stream().filter(c -> c.required() && !found.contains(c.key())).map(Column::header).toList();
        if (!missing.isEmpty()) {
            throw new BadRequestException("These mandatory columns are missing from the file: " + String.join(", ", missing)
                    + ". Download the template and copy your data into it.", "IMPORT_COLUMNS_MISSING");
        }

        List<Row> rows = new ArrayList<>();
        for (int r = 1; r < table.size(); r++) {
            List<String> line = table.get(r);
            Map<String, String> values = new LinkedHashMap<>();
            boolean any = false;
            for (int i = 0; i < keys.length && i < line.size(); i++) {
                if (keys[i] == null) continue;
                String v = clean(line.get(i));
                if (!v.isEmpty()) { values.put(keys[i], v); any = true; }
            }
            if (any) rows.add(new Row(r + 1, values));
        }
        if (rows.isEmpty()) throw new BadRequestException("The file has a header row but no data rows", "IMPORT_FILE_EMPTY");
        if (rows.size() > MAX_ROWS) {
            throw new BadRequestException("The file has " + rows.size() + " rows; upload at most " + MAX_ROWS + " at a time", "IMPORT_TOO_MANY_ROWS");
        }
        return rows;
    }

    /** Trims, collapses inner spaces and drops a leading formula character so values never run as formulas later. */
    private static String clean(String v) {
        if (v == null) return "";
        String s = v.replace(' ', ' ').trim().replaceAll("\\s+", " ");
        while (!s.isEmpty() && "=+@".indexOf(s.charAt(0)) >= 0 && !s.matches("^\\+\\d[\\d ]*$")) s = s.substring(1).trim();
        return s;
    }

    private static List<List<String>> readWorkbook(InputStream in) throws IOException {
        try (Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) return List.of();
            DataFormatter fmt = new DataFormatter(Locale.ENGLISH);
            FormulaEvaluator eval = wb.getCreationHelper().createFormulaEvaluator();
            List<List<String>> table = new ArrayList<>();
            int width = 0;
            for (int r = sheet.getFirstRowNum(); r <= sheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                List<String> line = new ArrayList<>();
                if (row != null) {
                    int last = Math.max(row.getLastCellNum(), width);
                    for (int c = 0; c < last; c++) {
                        Cell cell = row.getCell(c, org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                        line.add(cell == null ? "" : cellText(cell, fmt, eval));
                    }
                }
                if (table.isEmpty()) width = line.size();
                table.add(line);
            }
            return table;
        }
    }

    private static String cellText(Cell cell, DataFormatter fmt, FormulaEvaluator eval) {
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (type == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            LocalDate d = cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return d.toString();
        }
        if (type == CellType.NUMERIC) {
            double n = cell.getNumericCellValue();
            // whole numbers (roll numbers, phone numbers typed as numbers) without ".0" or exponent
            if (n == Math.rint(n) && Math.abs(n) < 1e15) return String.valueOf((long) n);
            return String.valueOf(n);
        }
        return fmt.formatCellValue(cell, eval);
    }

    /** RFC 4180 CSV: quoted fields, doubled quotes, commas and line breaks inside quotes; a UTF-8 byte-order mark is ignored. */
    static List<List<String>> readCsv(String text) {
        if (text.startsWith("﻿")) text = text.substring(1);
        List<List<String>> table = new ArrayList<>();
        List<String> line = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                else if (ch == '"') quoted = false;
                else cell.append(ch);
            } else if (ch == '"') quoted = true;
            else if (ch == ',') { line.add(cell.toString()); cell.setLength(0); }
            else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                line.add(cell.toString()); cell.setLength(0);
                table.add(line); line = new ArrayList<>();
            } else cell.append(ch);
        }
        if (cell.length() > 0 || !line.isEmpty()) { line.add(cell.toString()); table.add(line); }
        return table;
    }

    /* ---------- writing ---------- */

    /** The .xlsx template: the data sheet (mandatory headers shaded, dropdowns), an Instructions sheet and a hidden Lists sheet. */
    public static byte[] template(String sheetName, List<Column> columns, List<String> example, List<String> notes) {
        return template(sheetName, columns, example, notes, List.of());
    }

    /** The same template with data rows already filled in (the current records, to correct and upload again). */
    public static byte[] template(String sheetName, List<Column> columns, List<String> example, List<String> notes, List<List<String>> data) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Font bold = wb.createFont();
            bold.setBold(true);
            CellStyle required = wb.createCellStyle();
            required.setFont(bold);
            required.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            required.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle optional = wb.createCellStyle();
            optional.setFont(bold);
            CellStyle text = wb.createCellStyle();
            text.setDataFormat(wb.createDataFormat().getFormat("@"));

            Sheet sheet = wb.createSheet(sheetName);
            org.apache.poi.ss.usermodel.Row head = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell c = head.createCell(i);
                c.setCellValue(columns.get(i).label());
                c.setCellStyle(columns.get(i).required() ? required : optional);
                sheet.setColumnWidth(i, Math.max(14, columns.get(i).label().length() + 4) * 256);
                sheet.setDefaultColumnStyle(i, text); // keeps leading zeros and +91 in phone numbers
            }
            sheet.createFreezePane(0, 1);
            for (int rr = 0; rr < data.size(); rr++) {
                org.apache.poi.ss.usermodel.Row line = sheet.createRow(rr + 1);
                for (int c = 0; c < columns.size() && c < data.get(rr).size(); c++) {
                    String cell = data.get(rr).get(c);
                    if (cell != null && !cell.isEmpty()) { Cell cc = line.createCell(c); cc.setCellValue(cell); cc.setCellStyle(text); }
                }
            }

            // dropdowns: each list on the hidden Lists sheet, referenced by the column's validation
            Sheet lists = wb.createSheet("Lists");
            DataValidationHelper dv = sheet.getDataValidationHelper();
            int listCol = 0;
            for (int i = 0; i < columns.size(); i++) {
                List<String> choices = columns.get(i).choices();
                if (choices == null || choices.isEmpty()) continue;
                for (int r = 0; r < choices.size(); r++) {
                    org.apache.poi.ss.usermodel.Row row = lists.getRow(r) != null ? lists.getRow(r) : lists.createRow(r);
                    row.createCell(listCol).setCellValue(choices.get(r));
                }
                String col = colName(listCol);
                DataValidationConstraint constraint = dv.createFormulaListConstraint("Lists!$" + col + "$1:$" + col + "$" + choices.size());
                DataValidation v = dv.createValidation(constraint, new CellRangeAddressList(1, MAX_ROWS, i, i));
                v.setShowErrorBox(true);
                v.createErrorBox("Choose from the list", "Pick one of the values in the dropdown for " + columns.get(i).header() + ".");
                sheet.addValidationData(v);
                listCol++;
            }
            wb.setSheetHidden(wb.getSheetIndex(lists), true);

            Sheet help = wb.createSheet("Instructions");
            int r = 0;
            for (String n : notes) help.createRow(r++).createCell(0).setCellValue(n);
            r++;
            org.apache.poi.ss.usermodel.Row h = help.createRow(r++);
            String[] titles = { "Column", "Mandatory", "What to enter", "Example" };
            for (int i = 0; i < titles.length; i++) { h.createCell(i).setCellValue(titles[i]); h.getCell(i).setCellStyle(optional); }
            for (int i = 0; i < columns.size(); i++) {
                Column c = columns.get(i);
                org.apache.poi.ss.usermodel.Row row = help.createRow(r++);
                row.createCell(0).setCellValue(c.header());
                row.createCell(1).setCellValue(c.required() ? "Yes" : "No");
                row.createCell(2).setCellValue(c.help());
                row.createCell(3).setCellValue(i < example.size() ? example.get(i) : "");
            }
            help.setColumnWidth(0, 26 * 256);
            help.setColumnWidth(1, 11 * 256);
            help.setColumnWidth(2, 80 * 256);
            help.setColumnWidth(3, 28 * 256);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not build the template", e);
        }
    }

    private static String colName(int i) {
        StringBuilder s = new StringBuilder();
        for (int n = i + 1; n > 0; n = (n - 1) / 26) s.insert(0, (char) ('A' + (n - 1) % 26));
        return s.toString();
    }

    /** CSV with a header row and the given rows; values that would start a formula get a leading apostrophe. */
    public static byte[] csv(List<String> header, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder("﻿");
        appendCsv(sb, header);
        rows.forEach(r -> appendCsv(sb, r));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendCsv(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            String v = Optional.ofNullable(cells.get(i)).orElse("");
            if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0 && !v.matches("^\\+\\d+$")) v = "'" + v;
            if (i > 0) sb.append(',');
            sb.append('"').append(v.replace("\"", "\"\"")).append('"');
        }
        sb.append("\r\n");
    }
}
