package com.kvs.hr.importer;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/** A file read out of an uploaded ZIP, handed to the same photo storage as a photo uploaded on the profile. */
final class BytesMultipartFile implements MultipartFile {
    private final String name;
    private final String contentType;
    private final byte[] bytes;

    BytesMultipartFile(String name, String contentType, byte[] bytes) {
        this.name = name;
        this.contentType = contentType;
        this.bytes = bytes;
    }

    @Override public String getName() { return "file"; }
    @Override public String getOriginalFilename() { return name; }
    @Override public String getContentType() { return contentType; }
    @Override public boolean isEmpty() { return bytes.length == 0; }
    @Override public long getSize() { return bytes.length; }
    @Override public byte[] getBytes() { return bytes; }
    @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
    @Override public void transferTo(File dest) throws IOException { Files.write(dest.toPath(), bytes); }
}
