package com.shareshelf.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Set;

/**
 * Session gate for the whole app. Anyone who is not logged in can only reach the login/register
 * page and the static CSS; every other URL redirects to the login page. Doing this in ONE filter
 * means no servlet can forget to check the session.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_PATHS =
            Set.of("/", "/index.jsp", "/login", "/register", "/logout", "/hello", "/stats");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.isEmpty()) {
            path = "/";
        }

        if (PUBLIC_PATHS.contains(path) || path.startsWith("/css/")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        boolean loggedIn = session != null && session.getAttribute("studentId") != null;
        if (!loggedIn) {
            session = restoreFromRememberCookie(req, resp);   // null when there is no valid cookie
            loggedIn = session != null;
        }
        if (!loggedIn) {
            resp.sendRedirect(req.getContextPath() + "/index.jsp?error=login_required");
            return;
        }

        // Stops the browser showing a cached private page after logout / the Back button
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        if ("GET".equals(req.getMethod()) && !req.getRequestURI().endsWith("/live")) {
            addBellData(req, (String) session.getAttribute("studentId"));
        }
        chain.doFilter(request, response);
    }

    /**
     * The server forgot the login (restart, sleep, redeploy) but the browser still has a valid
     * "keep me signed in" cookie: sign the student in again quietly.
     *
     * @return the new session, or null when the cookie is missing, wrong or expired (a bad cookie is cleared)
     */
    private HttpSession restoreFromRememberCookie(HttpServletRequest req, HttpServletResponse resp) {
        String cookie = com.shareshelf.util.RememberCookie.read(req);
        if (cookie == null) {
            return null;
        }
        try {
            String studentId = new com.shareshelf.dao.RememberDAO().studentFor(cookie);
            com.shareshelf.model.Student student = studentId == null ? null : new com.shareshelf.dao.StudentDAO().findById(studentId);
            if (student == null) {
                com.shareshelf.util.RememberCookie.clear(req, resp);
                return null;
            }
            HttpSession fresh = req.getSession(true);
            fresh.setAttribute("studentId", student.getStudentId());
            fresh.setAttribute("studentName", student.getName());
            return fresh;
        } catch (RuntimeException e) {
            java.util.logging.Logger.getLogger(AuthFilter.class.getName()).warning("Remember-me check failed: " + e.getMessage());
            return null;
        }
    }

    /** Numbers for the top bar (bell count, ratings still owed). A failure here must never break the page. */
    private void addBellData(HttpServletRequest req, String studentId) {
        try {
            req.setAttribute("unreadCount", new com.shareshelf.dao.NotificationDAO().unreadCount(studentId));
            java.util.List<com.shareshelf.dao.RatingDAO.Pending> owed = new com.shareshelf.dao.RatingDAO().pendingFor(studentId);
            req.setAttribute("pendingRatings", owed);
        } catch (RuntimeException e) {
            java.util.logging.Logger.getLogger(AuthFilter.class.getName()).warning("Bell data unavailable: " + e.getMessage());
        }
    }
}
