package com.mxstrive.jenkins.plugin.contentreplace;

import hudson.FilePath;

import java.io.IOException;
import java.io.RandomAccessFile;

public class ContentReplaceBuilderUtil {
    public static boolean hasTrailingNewline(FilePath filePath) throws IOException {
        try (RandomAccessFile file = new RandomAccessFile(filePath.getRemote(), "r")) {
            long fileLength = file.length();
            if (fileLength == 0) {
                return false;
            }
            file.seek(fileLength - 1);
            byte lastByte = file.readByte();
            return lastByte == '\n' || lastByte == '\r';
        }
    }
}
