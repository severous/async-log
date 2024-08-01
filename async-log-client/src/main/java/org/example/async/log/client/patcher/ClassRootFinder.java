package org.example.async.log.client.patcher;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;

public class ClassRootFinder {

    private static String urlDecode(String in, boolean forceUtf8) {
        try {
            return URLDecoder.decode(in, forceUtf8 ? "UTF-8" : Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException e) {
            try {
                return URLDecoder.decode(in, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                return in;
            }
        }
    }


    public static String findClassRootOfClass(Class<?> context) {
        String name = context.getName();
        int idx = name.lastIndexOf('.');
        String packageBase;
        if (idx > -1) {
            packageBase = name.substring(0, idx);
            name = name.substring(idx + 1);
        } else {
            packageBase = "";
        }

        URL selfURL = context.getResource(name + ".class");
        String self = selfURL.toString();
        if (self.startsWith("file:/"))  {
            String path = urlDecode(self.substring(5), false);
            if (!new File(path).exists()) path = urlDecode(self.substring(5), true);
            String suffix = "/" + packageBase.replace('.', '/') + "/" + name + ".class";
            if (!path.endsWith(suffix)) throw new IllegalArgumentException("Unknown path structure: " + path);

            self = path.substring(0, path.length() - suffix.length());
        } else if (self.startsWith("jar:")) {
            int sep = self.indexOf('!');
            if (sep == -1) throw new IllegalArgumentException("No separator in jar protocol: " + self);
            String jarLoc = self.substring(4, sep);
            if (jarLoc.startsWith("file:/")) {
                String path = urlDecode(jarLoc.substring(5), false);
                if (!new File(path).exists()) path = urlDecode(jarLoc.substring(5), true);
                self = path;
            } else throw new IllegalArgumentException("Unknown path structure: " + self);
        } else {
            throw new IllegalArgumentException("Unknown protocol: " + self);
        }

        if (self.isEmpty()) self = "/";

        return self;
    }

}
