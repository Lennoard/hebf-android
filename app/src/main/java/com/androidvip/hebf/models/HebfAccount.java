package com.androidvip.hebf.models;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HebfAccount {
    private String uid;
    private String displayName;
    private String email;
    private String photoUrl = "https://cdn4.iconfinder.com/data/icons/small-n-flat/24/user-512.png";
    private List<String> achievements;
    private Map<String, Object> backup;

    public HebfAccount() {

    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }


    public Map<String, Object> getBackup() {
        return backup;
    }

    public void setBackup(Map<String, Object> backup) {
        this.backup = backup;
    }

    public List<String> getAchievements() {
        return achievements;
    }

    public void setAchievements(List<String> achievements) {
        this.achievements = achievements;
    }

    @NonNull
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("displayName", displayName);
        map.put("email", email);
        map.put("photoUrl", photoUrl);
        map.put("uid", uid);
        map.put("achievements", achievements);
        map.put("backup", backup != null ? new HashMap<>(backup) : new HashMap<>());
        return map;
    }

    @NonNull
    @Override
    public String toString() {
        return toMap().toString();
    }
}