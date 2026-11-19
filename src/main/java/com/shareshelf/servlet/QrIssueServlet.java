package com.shareshelf.servlet;

import com.shareshelf.dao.HandoverDAO;
import com.shareshelf.dao.RequestDAO;
import com.shareshelf.model.Request;
import com.shareshelf.util.QrUtil;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * GET /handover/qr?requestId=N - shows a QR code and a 6-digit code for the current step.
 * The QR holds a link to /scan, so a phone's normal camera app can scan it. Every visit makes a
 * fresh token and cancels the previous one.
 */
@WebServlet("/handover/qr")
public class QrIssueServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient RequestDAO requestDAO = new RequestDAO();
    private final transient HandoverDAO handoverDAO = new HandoverDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String me = currentStudentId(req);
        Request r = requestDAO.findById(ValidationUtil.parseIntOr(req.getParameter("requestId"), -1));
        String kind = HandoverFlow.kindFor(r);
        if (r == null || kind == null || !HandoverFlow.mayShow(kind, r, me)) {
            flash(req, "error", "There is no QR to show for this request right now.");
            redirect(req, resp, "/my-requests");
            return;
        }
        HandoverDAO.Issued issued = handoverDAO.issue(r.getRequestId(), kind);

        // Behind Render's HTTPS proxy Tomcat sees plain http, so trust the forwarded protocol header.
        String proto = req.getHeader("X-Forwarded-Proto");
        if (proto == null || !(proto.equals("http") || proto.equals("https"))) {
            proto = req.getScheme();
        }
        String host = req.getHeader("Host");
        String base = proto + "://" + (host != null ? host : req.getServerName() + ":" + req.getServerPort()) + req.getContextPath();
        String scanUrl = base + "/scan?t=" + issued.token();

        req.setAttribute("request", r);
        req.setAttribute("kind", kind);
        req.setAttribute("code", issued.code());
        req.setAttribute("expires", issued.expiresAt().format(DateTimeFormatter.ofPattern("h:mm a")));
        req.setAttribute("qrSvg", QrUtil.toSvg(scanUrl));
        forward(req, resp, "qr");
    }
}
