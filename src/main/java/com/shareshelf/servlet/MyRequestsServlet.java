package com.shareshelf.servlet;

import com.shareshelf.dao.RequestDAO;
import com.shareshelf.util.AppConstants;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** GET /my-requests - requests the student sent, and requests received on their listings. */
@WebServlet("/my-requests")
public class MyRequestsServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient RequestDAO requestDAO = new RequestDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String studentId = currentStudentId(req);
        req.setAttribute("sentRequests", requestDAO.findByRequester(studentId));
        req.setAttribute("receivedRequests", requestDAO.findByOwner(studentId));
        req.setAttribute("borrowDays", AppConstants.BORROW_DAYS);
        forward(req, resp, "my-requests");
    }
}
