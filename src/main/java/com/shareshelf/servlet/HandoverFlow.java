package com.shareshelf.servlet;

import com.shareshelf.dao.HandoverDAO;
import com.shareshelf.model.ListingStatus;
import com.shareshelf.model.Request;
import com.shareshelf.model.RequestStatus;

/**
 * The rules of the QR flow in one place, so the QR page and the scan page cannot disagree.
 *
 *   HANDOVER  accepted borrow request, item RESERVED:  the OWNER shows the QR, the BORROWER scans / types the code.
 *   RETURN    item BORROWED (request ACCEPTED or OVERDUE): the BORROWER shows the QR, the OWNER scans / types the code.
 *
 * Whoever scans is the person who must be physically there to confirm, which is the point of the QR.
 */
final class HandoverFlow {

    private HandoverFlow() {
    }

    /** HANDOVER, RETURN, or null when no QR step applies to this request right now. */
    static String kindFor(Request r) {
        if (r == null || !r.isBorrow()) {
            return null;
        }
        boolean accepted = RequestStatus.ACCEPTED.name().equals(r.getStatus());
        boolean overdue = RequestStatus.OVERDUE.name().equals(r.getStatus());
        if (accepted && ListingStatus.RESERVED.name().equals(r.getItemStatus())) {
            return HandoverDAO.HANDOVER;
        }
        if ((accepted || overdue) && ListingStatus.BORROWED.name().equals(r.getItemStatus())) {
            return HandoverDAO.RETURN;
        }
        return null;
    }

    /** Who shows the QR: owner for a handover, borrower for a return. */
    static boolean mayShow(String kind, Request r, String studentId) {
        return HandoverDAO.HANDOVER.equals(kind) ? r.getOwnerId().equals(studentId) : r.getRequesterId().equals(studentId);
    }

    /** Who scans / types the code: borrower for a handover, owner for a return. */
    static boolean mayConfirm(String kind, Request r, String studentId) {
        return HandoverDAO.HANDOVER.equals(kind) ? r.getRequesterId().equals(studentId) : r.getOwnerId().equals(studentId);
    }
}
