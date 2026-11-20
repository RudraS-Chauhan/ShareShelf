package com.shareshelf.servlet;

import com.shareshelf.dao.NotificationDAO;
import com.shareshelf.dao.WantedDAO;
import com.shareshelf.model.Wanted;
import com.shareshelf.util.AppConstants;
import com.shareshelf.util.ValidationUtil;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * The wanted board.
 *   GET  /wanted              list + "I need something" form
 *   POST /wanted              add a post          (action=add)
 *   POST /wanted              close my own post   (action=close, postId)
 *   POST /wanted              "I have this"       (action=offer, postId): tells the poster, then opens Add listing
 */
@WebServlet("/wanted")
public class WantedServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final transient WantedDAO wantedDAO = new WantedDAO();
    private final transient NotificationDAO notifier = new NotificationDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setAttribute("posts", wantedDAO.listOpen());
        req.setAttribute("categories", AppConstants.CATEGORIES);
        req.setAttribute("myMonth", wantedDAO.countRecent(currentStudentId(req)));
        req.setAttribute("maxMonth", AppConstants.MAX_WANTED_PER_MONTH);
        req.setAttribute("noOfferDays", AppConstants.WANTED_NO_OFFER_DAYS);
        req.setAttribute("maxLocation", AppConstants.MAX_WANTED_LOCATION);
        forward(req, resp, "wanted");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String me = currentStudentId(req);
        String action = param(req, "action");

        if ("add".equals(action)) {
            String title = ValidationUtil.limit(req.getParameter("title"), 200).replaceAll("\\p{Cntrl}", " ").trim();
            String note = ValidationUtil.limit(req.getParameter("note"), 600).replaceAll("\\p{Cntrl}", " ").trim();
            String location = ValidationUtil.limit(req.getParameter("location"), 200).replaceAll("\\p{Cntrl}", " ").trim();
            String category = param(req, "category");
            if (title.length() < 3 || title.length() > AppConstants.MAX_WANTED_TITLE) {
                flash(req, "error", "Say what you need in 3 to " + AppConstants.MAX_WANTED_TITLE + " characters.");
            } else if (!AppConstants.CATEGORIES.contains(category)) {
                flash(req, "error", "Choose a category from the list.");
            } else if (note.length() > AppConstants.MAX_WANTED_NOTE) {
                flash(req, "error", "The note can be at most " + AppConstants.MAX_WANTED_NOTE + " characters.");
            } else if (location.length() > AppConstants.MAX_WANTED_LOCATION) {
                flash(req, "error", "The location can be at most " + AppConstants.MAX_WANTED_LOCATION + " characters.");
            } else if (wantedDAO.add(me, title, category, note, location) < 0) {
                flash(req, "error", "You have already made " + AppConstants.MAX_WANTED_PER_MONTH + " posts in the last 30 days.");
            } else {
                flash(req, "success", "Posted. Students who have it can now tell you.");
            }
            redirect(req, resp, "/wanted");
            return;
        }

        Wanted post = wantedDAO.findById(ValidationUtil.parseIntOr(req.getParameter("postId"), -1));
        if (post == null) {
            flash(req, "error", "That post was not found.");
            redirect(req, resp, "/wanted");
        } else if ("close".equals(action)) {
            boolean closed = wantedDAO.close(post.getPostId(), me);
            flash(req, closed ? "success" : "error", closed ? "Post closed." : "Only the person who posted can close it.");
            redirect(req, resp, "/wanted");
        } else if ("offer".equals(action) && !post.getStudentId().equals(me)) {
            if (wantedDAO.offerOnce(post.getPostId(), me)) {
                notifier.add(post.getStudentId(), (String) req.getSession().getAttribute("studentName")
                        + " has \"" + post.getTitle() + "\" for you. They will list it, so check Search.",
                        "/search?q=" + URLEncoder.encode(post.getTitle(), StandardCharsets.UTF_8));
            }
            flash(req, "success", post.getStudentName() + " has been told. List the item so they can request it.");
            redirect(req, resp, "/add-listing?title=" + URLEncoder.encode(post.getTitle(), StandardCharsets.UTF_8));
        } else {
            redirect(req, resp, "/wanted");
        }
    }
}
