package com.shareshelf.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** A "wanted" post: a student asking for something they need. */
public class Wanted {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private int postId, year;
    private String studentId, studentName, branch, title, category, note, location;
    private LocalDateTime createdAt;

    public int getPostId() { return postId; }
    public void setPostId(int v) { postId = v; }
    public int getYear() { return year; }
    public void setYear(int v) { year = v; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String v) { studentId = v; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String v) { studentName = v; }
    public String getBranch() { return branch; }
    public void setBranch(String v) { branch = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { category = v; }
    public String getNote() { return note; }
    public void setNote(String v) { note = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { location = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public String getWhenText() { return createdAt == null ? "" : createdAt.format(WHEN); }
}
