package com.shareshelf.servlet;

import com.shareshelf.util.DataAccessException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common behaviour for every ShareShelf servlet:
 *  - small helpers (current student, flash messages, forward/redirect, trimmed parameters)
 *  - ONE central place that turns a database failure into a friendly error page, so individual
 *    servlets do not need try/catch around every DAO call.
 */
public abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(BaseServlet.class.getName());

    /** JSP views live under WEB-INF so they can only be reached through a servlet. */
    private static final String VIEWS = "/WEB-INF/views/";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            super.service(req, resp);
        } catch (DataAccessException e) {
            LOG.log(Level.SEVERE, "Database error while handling " + req.getRequestURI(), e);
            if (resp.isCommitted()) {
                throw e;
            }
            req.setAttribute("errorTitle", "We couldn't reach the database");
            req.setAttribute("errorMessage", "Please try again in a moment. If it keeps happening, tell the team.");
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            forward(req, resp, "error");
        }
    }

    /** The logged-in student's id, or null when nobody is logged in. */
    protected String currentStudentId(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (String) session.getAttribute("studentId");
    }

    /** Stores a one-time message ("success" or "error") that the next page shows once. */
    protected void flash(HttpServletRequest req, String type, String text) {
        HttpSession session = req.getSession();
        session.setAttribute("flashType", type);
        session.setAttribute("flashText", text);
    }

    protected void forward(HttpServletRequest req, HttpServletResponse resp, String viewName)
            throws ServletException, IOException {
        req.getRequestDispatcher(VIEWS + viewName + ".jsp").forward(req, resp);
    }

    /** Redirects to a path inside this app, e.g. "/dashboard". */
    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    /** Redirects to the page named by the hidden "back" field (dashboard or my-requests only). */
    protected void redirectBack(HttpServletRequest req, HttpServletResponse resp, String defaultPage) throws IOException {
        String back = param(req, "back");
        String page = ("dashboard".equals(back) || "my-requests".equals(back)) ? back : defaultPage;
        redirect(req, resp, "/" + page);
    }

    /** A request parameter, trimmed; empty string instead of null. */
    protected String param(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? "" : value.trim();
    }
}
