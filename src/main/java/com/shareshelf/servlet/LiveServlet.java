package com.shareshelf.servlet;

import com.shareshelf.dao.NotificationDAO;
import com.shareshelf.dao.RequestDAO;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * GET /live - a tiny JSON answer the pages poll every few seconds: {"unread":2,"sig":"1a2b3c"}.
 * "unread" updates the bell; when "sig" changes, something happened to one of this student's requests
 * (new request, accept, pickup, return) and the open page refreshes itself. Plain polling needs no
 * extra server features, so it works on the free hosting plan.
 */
@WebServlet("/live")
public class LiveServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient NotificationDAO notificationDAO = new NotificationDAO();
    private final transient RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String me = currentStudentId(req);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        try {
            resp.getWriter().write("{\"unread\":" + notificationDAO.unreadCount(me)
                    + ",\"sig\":\"" + requestDAO.stateSignature(me) + "\"}");
        } catch (RuntimeException e) {
            resp.setStatus(503);
            resp.getWriter().write("{}");
        }
    }
}
