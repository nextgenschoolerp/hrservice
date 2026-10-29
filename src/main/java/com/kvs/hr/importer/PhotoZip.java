package com.kvs.hr.importer;

import com.kvs.core.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Reads a ZIP of photos named after the record they belong to (STU-2026-0001.jpg, DPS-EMP-2001.png). Folders, hidden files and
 * the junk Windows and macOS add (__MACOSX, Thumbs.db, .DS_Store) are ignored. Limits keep a malicious ZIP from filling memory.
 */
final class PhotoZip {

    static final long MAX_ZIP_BYTES = 60L * 1024 * 1024;
    static final int MAX_FILES = 2000;
    static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    private static final long HARD_ENTRY_LIMIT = 20L * 1024 * 1024;
    private static final long MAX_TOTAL = 400L * 1024 * 1024;

    /** One image of the ZIP: its file name, the key (name without extension, lower case), its bytes and whether it exceeds 5 MB. */
    record Entry(String fileName, String key, String extension, byte[] bytes, boolean tooBig) {
        /** The real type from the file's first bytes: image/jpeg, image/png, or null when it is neither. */
        String imageType() {
            if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) return "image/jpeg";
            if (bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') return "image/png";
            return null;
        }
    }

    private PhotoZip() { }

    static List<Entry> read(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Choose a ZIP file of photos", "IMPORT_FILE_REQUIRED");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".zip")) throw new BadRequestException("Upload the photos as a .zip file", "IMPORT_FILE_TYPE");
        if (file.getSize() > MAX_ZIP_BYTES) throw new BadRequestException("The ZIP is larger than 60 MB. Split the photos into two ZIP files.", "IMPORT_FILE_TOO_LARGE");
        List<Entry> out = new ArrayList<>();
        long total = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream())) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                String path = e.getName().replace('\\', '/');
                String base = path.substring(path.lastIndexOf('/') + 1);
                if (e.isDirectory() || base.isBlank() || base.startsWith(".") || path.contains("__MACOSX/")
                        || base.equalsIgnoreCase("Thumbs.db") || base.equalsIgnoreCase("desktop.ini")) continue;
                if (out.size() >= MAX_FILES) throw new BadRequestException("The ZIP has more than " + MAX_FILES + " files. Split it into smaller ZIP files.", "IMPORT_TOO_MANY_ROWS");
                byte[] bytes = readEntry(zip);
                total += bytes.length;
                if (total > MAX_TOTAL) throw new BadRequestException("The ZIP expands to more than 400 MB. Check that it holds photos only.", "IMPORT_FILE_TOO_LARGE");
                int dot = base.lastIndexOf('.');
                String stem = dot > 0 ? base.substring(0, dot) : base;
                String ext = dot > 0 ? base.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
                out.add(new Entry(base, stem.trim().toLowerCase(Locale.ROOT), ext, bytes, bytes.length > MAX_PHOTO_BYTES));
            }
        } catch (IOException e) {
            throw new BadRequestException("The ZIP file could not be read. Create it again and retry.", "IMPORT_FILE_UNREADABLE");
        }
        if (out.isEmpty()) throw new BadRequestException("The ZIP has no files in it", "IMPORT_FILE_EMPTY");
        return out;
    }

    private static byte[] readEntry(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[16 * 1024];
        int n;
        while ((n = in.read(chunk)) > 0) {
            buf.write(chunk, 0, n);
            if (buf.size() > HARD_ENTRY_LIMIT) throw new BadRequestException("A file in the ZIP is far larger than a photo (over 20 MB). Photos must be JPG or PNG up to 5 MB.", "IMPORT_FILE_TOO_LARGE");
        }
        return buf.toByteArray();
    }
}
