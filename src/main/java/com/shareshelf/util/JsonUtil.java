package com.shareshelf.util;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Tiny JSON helper so servlets that answer with JSON (like the /live polling endpoint) do not each
 * build strings and set headers by hand. Supports what we need: a flat object of strings, numbers and booleans.
 */
public final class JsonUtil {

    private JsonUtil() {
    }

    /** Builds {"key":value,...} from a flat map. Strings are quoted and escaped; numbers and booleans are not. */
    public static String toJson(Map<String, ?> fields) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> e : fields.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(quote(e.getKey())).append(':');
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append(quote(v.toString()));
            }
        }
        return sb.append('}').toString();
    }

    /** Escapes a string and wraps it in double quotes. */
    public static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    /** Sends the map as the JSON response with the given HTTP status; never cached. */
    public static void send(HttpServletResponse resp, int status, Map<String, ?> fields) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(toJson(fields));
    }
}
