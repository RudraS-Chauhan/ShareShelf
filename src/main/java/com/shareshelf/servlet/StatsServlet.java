package com.shareshelf.servlet;

import com.shareshelf.dao.StatsDAO;
import com.shareshelf.util.JsonUtil;
import com.shareshelf.util.TtlCache;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /stats - public JSON: {"students":12,"items":30,"exchanges":7}. Shown on the login page as the campus
 * impact counter. Only totals, nothing personal. The answer is cached for a minute so the login page (and an
 * uptime pinger hitting this URL) costs the free database almost nothing.
 */
@WebServlet("/stats")
public class StatsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final long CACHE_MILLIS = 60_000;
    private final transient TtlCache<Map<String, Object>> cache =
            new TtlCache<>(CACHE_MILLIS, () -> new StatsDAO().campusImpact());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            JsonUtil.send(resp, 200, cache.get());
        } catch (RuntimeException e) {
            JsonUtil.send(resp, 503, new LinkedHashMap<String, Object>());
        }
    }
}
