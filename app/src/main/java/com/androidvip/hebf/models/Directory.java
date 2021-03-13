package com.androidvip.hebf.models;

import java.io.File;

public class Directory {
    private String name;
    private String path;
    private long size;
    private boolean checked;

    public Directory(String path){
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean exists() {
        File file = getFile();
        return file.exists() || file.isDirectory();
    }

    public File getFile() {
        return new File(path);
    }
}