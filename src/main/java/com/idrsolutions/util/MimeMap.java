package com.idrsolutions.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MimeMap {
    private static final HashMap<String, String> map = new HashMap<>();

    static {
        map.put("doc", "application/msword");
        map.put("dot", "application/msword");

        map.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        map.put("dotx", "application/vnd.openxmlformats-officedocument.wordprocessingml.template");
        map.put("docm", "application/vnd.ms-word.document.macroEnabled.12");
        map.put("dotm", "application/vnd.ms-word.template.macroEnabled.12");

        map.put("xls", "application/vnd.ms-excel");
        map.put("xlt", "application/vnd.ms-excel");
        map.put("xla", "application/vnd.ms-excel");

        map.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        map.put("xltx", "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
        map.put("xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12");
        map.put("xltm", "application/vnd.ms-excel.template.macroEnabled.12");
        map.put("xlam", "application/vnd.ms-excel.addin.macroEnabled.12");
        map.put("xlsb", "application/vnd.ms-excel.sheet.binary.macroEnabled.12");

        map.put("ppt", "application/vnd.ms-powerpoint");
        map.put("pot", "application/vnd.ms-powerpoint");
        map.put("pps", "application/vnd.ms-powerpoint");
        map.put("ppa", "application/vnd.ms-powerpoint");

        map.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        map.put("potx", "application/vnd.openxmlformats-officedocument.presentationml.template");
        map.put("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
        map.put("ppam", "application/vnd.ms-powerpoint.addin.macroEnabled.12");
        map.put("pptm", "application/vnd.ms-powerpoint.presentation.macroEnabled.12");
        map.put("potm", "application/vnd.ms-powerpoint.template.macroEnabled.12");
        map.put("ppsm", "application/vnd.ms-powerpoint.slideshow.macroEnabled.12");

        map.put("mdb", "application/vnd.ms-access");
    }

    public static String getMimeType(String extension) {
        return map.get(extension);
    }

    public static String getExtension(String mimeType) {
        Optional<Map.Entry<String, String>> extension = map.entrySet().stream().filter((entry) -> entry.getValue().equals(mimeType)).findFirst();

        return extension.map(Map.Entry::getKey).orElse(null);
    }
}
