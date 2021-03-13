package com.androidvip.hebf.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import androidx.annotation.NonNull;

public final class FileUtils {

    private FileUtils() {

    }

    @NonNull
    public static String readMultilineFile(File file) {
        return baseReadMultilineFile(file, "");
    }

    @NonNull
    public static String readMultilineFile(String file, String defaultOutput) {
        return baseReadMultilineFile(new File(file), defaultOutput);
    }

    public static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    @NonNull
    private static String baseReadMultilineFile(File file, String defaultOutput){
        if (file.isFile() || file.exists()) {
            if (file.canRead()) {
                try {
                    FileInputStream fin = new FileInputStream(file);
                    String ret = inputStreamToMultilineString(fin, defaultOutput);
                    fin.close();
                    return ret;
                } catch (Exception ignored) {}
                return defaultOutput;
            } else {
                return RootUtils.readMultilineFile(file.toString());
            }
        }
        return defaultOutput;
    }

    private static String inputStreamToMultilineString(InputStream inputStream, String defaultOutput) {
        String multilineString = defaultOutput;
        try {
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            multilineString = sb.toString();
        } catch (IOException ignored) {}
        return multilineString;
    }

    public static long getFileSize(File file) {
        long size = 0;
        try {
            if (file.isDirectory())
                for (File f : file.listFiles())
                    size += getFileSize(f);
            else
                size=file.length();
        } catch (Exception ignored) {}
        return size;
    }

    public static long getFileSizeRoot(String path) {
        long size = 0;
        File file = new File(path);
        if (file.canRead())
            return getFileSize(file);
        else {
            String outputSize = RootUtils.executeSync("du -sc " + path + " | tail -n 1 | awk '{print $1}'");
            try {
                // Default 'du' output is in kilobytes, convert to bytes
                size = (Long.parseLong(outputSize) * 1024);
            } catch (Exception ignored) {}
        }
        return size;
    }

    public static boolean deleteFile(String fileOrDir) {
        return baseDeleteFile(new File(fileOrDir));
    }

    public static boolean deleteFile(File fileOrDir) {
        return baseDeleteFile(fileOrDir);
    }

    private static boolean baseDeleteFile(File fileOrDir) {
        if (fileOrDir.isDirectory()) {
            Utils.runCommand("rm -rf \"" + fileOrDir + "\"", "");
            File deletedFile = new File(fileOrDir.toString());
            return !deletedFile.isDirectory();
        } else {
            if (fileOrDir.isFile() || fileOrDir.exists()) {
                if (!fileOrDir.delete()) {
                    RootUtils.executeSync("rm -f \"" + fileOrDir + "\"");
                }
                File deletedFile = new File(fileOrDir.toString());
                return !deletedFile.exists();
            }
        }
        return false;
    }
}
