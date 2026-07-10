package com.github.xfalcon.vhosts.editor;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class HostsContentTest {

    @Test
    public void parse_basicEntry() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("127.0.0.1 example.com");
        assertEquals(1, entries.size());
        assertEquals("127.0.0.1", entries.get(0).ip);
        assertEquals("example.com", entries.get(0).domain);
        assertFalse(entries.get(0).isComment);
    }

    @Test
    public void parse_wildcardEntry() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("127.0.0.1 .a.com");
        assertEquals(1, entries.size());
        assertEquals("127.0.0.1", entries.get(0).ip);
        assertEquals(".a.com", entries.get(0).domain);
    }

    @Test
    public void parse_ipv6Entry() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("::1 localhost");
        assertEquals(1, entries.size());
        assertEquals("::1", entries.get(0).ip);
        assertEquals("localhost", entries.get(0).domain);
    }

    @Test
    public void parse_commentLine() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("# this is a comment");
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).isComment);
        assertEquals("# this is a comment", entries.get(0).comment);
    }

    @Test
    public void parse_emptyLine() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("");
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).isEmpty);
    }

    @Test
    public void parse_invalidIpline_returnsComment() {
        // Invalid IP format lines should be preserved as comments
        List<HostsContent.HostsEntry> entries = HostsContent.parse("notanip example.com");
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).isComment);
    }

    @Test
    public void parse_multipleLines() {
        String input = "127.0.0.1 example.com\n# comment\n::1 localhost\n\n192.168.1.1 .test.com";
        List<HostsContent.HostsEntry> entries = HostsContent.parse(input);
        assertEquals(5, entries.size());
        assertFalse(entries.get(0).isComment);
        assertTrue(entries.get(1).isComment);
        assertEquals("::1", entries.get(2).ip);
        assertTrue(entries.get(3).isEmpty);
        assertEquals(".test.com", entries.get(4).domain);
    }

    @Test
    public void serialize_basicEntries() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("127.0.0.1 example.com\n# note");
        String result = HostsContent.serialize(entries);
        assertEquals("127.0.0.1 example.com\n# note", result);
    }

    @Test
    public void serialize_roundTrip() {
        String input = "127.0.0.1 example.com\n# a comment\n192.168.1.1 .test.com\n\n::1 localhost";
        List<HostsContent.HostsEntry> entries = HostsContent.parse(input);
        String result = HostsContent.serialize(entries);
        assertEquals(input, result);
    }

    @Test
    public void parse_null_returnsEmptyList() {
        assertEquals(0, HostsContent.parse(null).size());
    }

    @Test
    public void parse_inlineCommentInEntry() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("127.0.0.1 example.com # block ads");
        assertEquals(1, entries.size());
        assertEquals("127.0.0.1", entries.get(0).ip);
        assertEquals("example.com", entries.get(0).domain);
        assertFalse(entries.get(0).isComment);
    }

    @Test
    public void parse_tabSeparatedEntry() {
        List<HostsContent.HostsEntry> entries = HostsContent.parse("127.0.0.1\texample.com");
        assertEquals(1, entries.size());
        assertEquals("127.0.0.1", entries.get(0).ip);
        assertEquals("example.com", entries.get(0).domain);
    }

    @Test
    public void serialize_trailingNewline() {
        String input = "127.0.0.1 example.com\n";
        List<HostsContent.HostsEntry> entries = HostsContent.parse(input);
        String result = HostsContent.serialize(entries);
        assertEquals("127.0.0.1 example.com", result);
    }
}
