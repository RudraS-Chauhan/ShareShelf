package com.shareshelf.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonUtilTest {

    @Test
    void buildsFlatJsonAndEscapesStrings() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("unread", 2);
        m.put("ok", true);
        m.put("sig", "a\"b\\c\n");
        m.put("none", null);
        assertEquals("{\"unread\":2,\"ok\":true,\"sig\":\"a\\\"b\\\\c\\n\",\"none\":null}", JsonUtil.toJson(m));
    }

    @Test
    void emptyMapIsEmptyObject() {
        assertEquals("{}", JsonUtil.toJson(new LinkedHashMap<>()));
    }
}
