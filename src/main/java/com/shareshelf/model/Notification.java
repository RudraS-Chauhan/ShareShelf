package com.shareshelf.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** One line in a student's notification list (the bell). */
public class Notification {
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH);

    private int notificationId;
    private String message, link;
    private boolean read;
    private LocalDateTime createdAt;

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int v) { notificationId = v; }
    public String getMessage() { return message; }
    public void setMessage(String v) { message = v; }
    public String getLink() { return link; }
    public void setLink(String v) { link = v; }
    public boolean isRead() { return read; }
    public void setRead(boolean v) { read = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }

    public String getWhenText() {
        return createdAt == null ? "" : createdAt.format(WHEN);
    }
}
