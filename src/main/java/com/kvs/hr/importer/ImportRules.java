package com.kvs.hr.importer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Value checks shared by the bulk imports: dates, Indian mobile numbers, email, PAN, IFSC and Aadhaar. */
public final class ImportRules {

    // STRICT: 31/02/2015 is rejected instead of being turned into 28 February
    private static final List<DateTimeFormatter> DATES = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT), DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT), DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT), DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.ENGLISH).withResolverStyle(ResolverStyle.STRICT));
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$");
    private static final Pattern PAN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern IFSC = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern AADHAAR = Pattern.compile("^[2-9][0-9]{11}$");

    private ImportRules() { }

    /** DD/MM/YYYY, DD-MM-YYYY, YYYY-MM-DD or 12 Mar 2015; null when it is none of these. */
    public static LocalDate date(String v) {
        if (v == null) return null;
        String s = v.trim();
        for (DateTimeFormatter f : DATES) {
            try { return LocalDate.parse(s, f); } catch (DateTimeParseException ignored) { /* next format */ }
        }
        return null;
    }

    /** An Indian mobile number as +91XXXXXXXXXX (10 digits starting 6–9, with or without +91 / 0), else null. */
    public static String mobile(String v) {
        if (v == null) return null;
        String d = v.replaceAll("[\\s\\-().]", "");
        if (d.startsWith("+91")) d = d.substring(3);
        else if (d.startsWith("91") && d.length() == 12) d = d.substring(2);
        else if (d.startsWith("0") && d.length() == 11) d = d.substring(1);
        return d.matches("^[6-9][0-9]{9}$") ? "+91" + d : null;
    }

    public static boolean email(String v) { return v != null && v.length() <= 150 && EMAIL.matcher(v).matches(); }
    public static boolean pan(String v) { return v != null && PAN.matcher(v.toUpperCase(Locale.ROOT)).matches(); }
    public static boolean ifsc(String v) { return v != null && IFSC.matcher(v.toUpperCase(Locale.ROOT)).matches(); }
    public static boolean aadhaar(String v) { return v != null && AADHAAR.matcher(v.replaceAll("\\s", "")).matches(); }

    /** A positive amount ("45,000" and "₹45000" allowed), else null. */
    public static BigDecimal amount(String v) {
        if (v == null) return null;
        try {
            BigDecimal n = new BigDecimal(v.replaceAll("[₹,\\s]|Rs\\.?", ""));
            return n.signum() > 0 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** A positive whole number, else null. */
    public static Integer positive(String v) {
        if (v == null) return null;
        try {
            int n = Integer.parseInt(v.trim());
            return n > 0 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
