package com.shareshelf.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class QrUtilTest {

    @Test
    void drawsAScalableSvgAndOnlyEverContainsShapes() {
        String svg = QrUtil.toSvg("https://example.com/ShareShelf/scan?t=0123456789abcdef0123456789abcdef");
        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.contains("viewBox"));
        assertTrue(svg.contains("<path"));
        assertFalse(svg.contains("<script"), "output must be safe to print unescaped");
    }

    @Test
    void differentTextGivesADifferentCode() {
        assertNotEquals(QrUtil.toSvg("one"), QrUtil.toSvg("two"));
    }
}
