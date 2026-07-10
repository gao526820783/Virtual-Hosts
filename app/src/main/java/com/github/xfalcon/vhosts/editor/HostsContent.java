package com.github.xfalcon.vhosts.editor;

import org.xbill.DNS.Address;

import java.util.ArrayList;
import java.util.List;

public class HostsContent {

    public static class HostsEntry {
        public String ip;
        public String domain;
        public String comment;
        public boolean isComment;
        public boolean isEmpty;

        public static HostsEntry createRecord(String ip, String domain) {
            HostsEntry e = new HostsEntry();
            e.ip = ip;
            e.domain = domain;
            e.isComment = false;
            e.isEmpty = false;
            return e;
        }

        public static HostsEntry createComment(String comment) {
            HostsEntry e = new HostsEntry();
            e.comment = comment;
            e.isComment = true;
            e.isEmpty = false;
            return e;
        }

        public static HostsEntry createEmpty() {
            HostsEntry e = new HostsEntry();
            e.isEmpty = true;
            e.isComment = false;
            return e;
        }
    }

    public static List<HostsEntry> parse(String text) {
        List<HostsEntry> entries = new ArrayList<>();
        if (text == null) return entries;

        String[] lines = text.split("\\n", -1); // -1 to keep trailing empty lines
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                entries.add(HostsEntry.createEmpty());
            } else if (trimmed.startsWith("#")) {
                entries.add(HostsEntry.createComment(trimmed));
            } else {
                String[] parts = trimmed.split("\\s+", 2);
                if (parts.length == 2) {
                    String ip = parts[0].trim();
                    String domainPart = parts[1].trim();
                    // Strip inline comment
                    int commentIdx = domainPart.indexOf('#');
                    String domain = commentIdx >= 0 ? domainPart.substring(0, commentIdx).trim() : domainPart;
                    if (domain.isEmpty()) {
                        entries.add(HostsEntry.createComment(trimmed));
                        continue;
                    }
                    try {
                        Address.getByAddress(ip);
                        entries.add(HostsEntry.createRecord(ip, domain));
                    } catch (Exception e) {
                        // Invalid IP, treat entire line as a comment to preserve it
                        entries.add(HostsEntry.createComment(trimmed));
                    }
                } else {
                    // Can't parse as record, keep as comment
                    entries.add(HostsEntry.createComment(trimmed));
                }
            }
        }
        return entries;
    }

    public static String serialize(List<HostsEntry> entries) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            HostsEntry entry = entries.get(i);
            if (entry.isEmpty) {
                sb.append("");
            } else if (entry.isComment) {
                sb.append(entry.comment != null ? entry.comment : "");
            } else {
                sb.append(entry.ip).append(" ").append(entry.domain);
            }
            if (i < entries.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
