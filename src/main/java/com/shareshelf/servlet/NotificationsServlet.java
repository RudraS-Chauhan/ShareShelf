package com.shareshelf.servlet;

import com.shareshelf.dao.NotificationDAO;
import com.shareshelf.model.Notification;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/** GET /notifications - the list behind the bell. Opening it marks everything as read. */
@WebServlet("/notifications")
public class NotificationsServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient NotificationDAO notificationDAO = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String me = currentStudentId(req);
        List<Notification> list = notificationDAO.latest(me);   // loaded first, so unread ones still look unread on this page
        notificationDAO.markAllRead(me);
        req.setAttribute("notifications", list);
        req.setAttribute("unreadCount", 0);
        forward(req, resp, "notifications");
    }
}
