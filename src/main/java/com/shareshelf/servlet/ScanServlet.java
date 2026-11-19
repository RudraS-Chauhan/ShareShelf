package com.shareshelf.servlet;

import com.shareshelf.dao.HandoverDAO;
import com.shareshelf.dao.ListingDAO;
import com.shareshelf.dao.NotificationDAO;
import com.shareshelf.dao.RequestDAO;
import com.shareshelf.model.ListingStatus;
import com.shareshelf.model.Request;
import com.shareshelf.util.AppConstants;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * /scan - the confirming side of the QR flow.
 *   GET  /scan?t=TOKEN        opened by scanning the QR with the phone camera
 *   GET  /scan?requestId=N    the "type the 6-digit code instead" page
 *   POST /scan                confirms the step
 *
 * HANDOVER: the borrower confirms pickup  -> item becomes BORROWED, the owner gets a notification.
 * RETURN:   the owner confirms the return -> request COMPLETED, then the owner rates the borrower
 *           and note the item's condition; the rating screen opens right after the code is verified.
 */
@WebServlet("/scan")
public class ScanServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient RequestDAO requestDAO = new RequestDAO();
    private final transient ListingDAO listingDAO = new ListingDAO();
    private final transient HandoverDAO handoverDAO = new HandoverDAO();
    private final transient NotificationDAO notifier = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = param(req, "t");
        int requestId = token.isEmpty() ? ValidationUtil.parseIntOr(req.getParameter("requestId"), -1)
                : handoverDAO.requestIdOfToken(token);
        if (!prepare(req, resp, requestId, token)) {
            return;
        }
        forward(req, resp, "scan");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String token = param(req, "t");
        int requestId = ValidationUtil.parseIntOr(req.getParameter("requestId"), -1);
        if (!prepare(req, resp, requestId, token)) {
            return;
        }
        Request r = (Request) req.getAttribute("request");
        String kind = (String) req.getAttribute("kind");
        String me = currentStudentId(req);
        String code = param(req, "code").replaceAll("\\D", "");

        String condition = param(req, "condition");
        boolean isReturn = HandoverDAO.RETURN.equals(kind);
        if (isReturn && !AppConstants.RETURN_CONDITIONS.contains(condition)) {
            showError(req, resp, "Please choose the item's condition.");
            return;
        }

        if (!handoverDAO.redeem(r.getRequestId(), kind, token.isEmpty() ? null : token, code.isEmpty() ? null : code)) {
            showError(req, resp, "That code is wrong, expired or already used. Ask for a fresh QR and try again.");
            return;
        }

        if (!isReturn) {
            if (requestDAO.startBorrow(r.getRequestId(), r.getItemId())) {
                Request now = requestDAO.findById(r.getRequestId());
                notifier.add(r.getOwnerId(), r.getRequesterName() + " picked up \"" + r.getItemTitle() + "\". It is now marked as borrowed.", "/my-requests");
                flash(req, "success", "Pickup confirmed. \"" + r.getItemTitle() + "\" is yours"
                        + (now != null && !now.getDueText().isEmpty() ? " until " + now.getDueText() + "." : "."));
            } else {
                flash(req, "error", "Could not confirm the pickup. Please try again.");
            }
            redirect(req, resp, "/my-requests");
            return;
        }

        if (requestDAO.complete(r.getRequestId(), r.getItemId())) {
            requestDAO.setReturnCondition(r.getRequestId(), condition);
            notifier.add(r.getRequesterId(), "Return of \"" + r.getItemTitle() + "\" confirmed. Please rate " + r.getOwnerName() + ".",
                    "/rate?requestId=" + r.getRequestId());
            flash(req, "success", "Return verified. \"" + r.getItemTitle() + "\" is available again. Now rate " + r.getRequesterName() + ".");
            redirect(req, resp, "/rate?requestId=" + r.getRequestId());   // step 2: the rating, after the code was verified
        } else {
            flash(req, "error", "Could not complete the return. Please try again.");
            redirect(req, resp, "/my-requests");
        }
    }

    /** Loads the request and checks that this student is the one who confirms this step. */
    private boolean prepare(HttpServletRequest req, HttpServletResponse resp, int requestId, String token)
            throws IOException {
        Request r = requestId > 0 ? requestDAO.findById(requestId) : null;
        String kind = HandoverFlow.kindFor(r);
        if (r == null || kind == null) {
            flash(req, "error", "Nothing to confirm for this request right now.");
            redirect(req, resp, "/my-requests");
            return false;
        }
        if (!HandoverFlow.mayConfirm(kind, r, currentStudentId(req))) {
            flash(req, "error", HandoverDAO.HANDOVER.equals(kind)
                    ? "Only the borrower can confirm the pickup."
                    : "Only the owner can confirm the return.");
            redirect(req, resp, "/my-requests");
            return false;
        }
        req.setAttribute("request", r);
        req.setAttribute("kind", kind);
        req.setAttribute("token", token);
        req.setAttribute("conditions", AppConstants.RETURN_CONDITIONS);
        return true;
    }

    private void showError(HttpServletRequest req, HttpServletResponse resp, String message)
            throws ServletException, IOException {
        req.setAttribute("error", message);
        forward(req, resp, "scan");
    }
}
