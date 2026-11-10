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
            Set.of("/", "/index.jsp", "/login", "/register", "/logout", "/hello");

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
            resp.sendRedirect(req.getContextPath() + "/index.jsp?error=login_required");
            return;
        }

        // Stops the browser showing a cached private page after logout / the Back button
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        chain.doFilter(request, response);
    }
}
