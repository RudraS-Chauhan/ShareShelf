package com.shareshelf.util;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Reads, sets and clears the "keep me signed in" cookie (HttpOnly, SameSite=Lax, Secure on HTTPS). */
public final class RememberCookie {

    private RememberCookie() {
    }

    /** @return the cookie value, or null when the browser did not send one. */
    public static String read(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (AppConstants.REMEMBER_COOKIE.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    public static void set(HttpServletRequest req, HttpServletResponse resp, String value) {
        write(req, resp, value, AppConstants.REMEMBER_DAYS * 24 * 60 * 60);
    }

    public static void clear(HttpServletRequest req, HttpServletResponse resp) {
        write(req, resp, "", 0);
    }

    // The Servlet 4.0 Cookie class has no SameSite setting, so the header is written by hand.
    private static void write(HttpServletRequest req, HttpServletResponse resp, String value, int maxAgeSeconds) {
        String proto = req.getHeader("X-Forwarded-Proto");   // Render terminates HTTPS in front of Tomcat
        boolean https = "https".equals(proto) || req.isSecure();
        String path = req.getContextPath().isEmpty() ? "/" : req.getContextPath();
        resp.addHeader("Set-Cookie", AppConstants.REMEMBER_COOKIE + "=" + value + "; Max-Age=" + maxAgeSeconds
                + "; Path=" + path + "; HttpOnly; SameSite=Lax" + (https ? "; Secure" : ""));
    }
}
