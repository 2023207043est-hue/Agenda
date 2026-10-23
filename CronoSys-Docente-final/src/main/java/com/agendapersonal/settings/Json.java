package com.agendapersonal.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON mínimo (sin dependencias): objetos → Map, arreglos → List, números → Double/Long. */
public final class Json {
    private Json() {}

    public static final class JsonException extends RuntimeException {
        public JsonException(String m) { super(m); }
    }

    public static Object parse(String s) {
        if (s == null) throw new JsonException("JSON vacío");
        P p = new P(s);
        p.ws();
        Object v = p.value();
        p.ws();
        if (p.i != s.length()) throw new JsonException("Texto sobrante en la posición " + p.i);
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String s) {
        Object o = parse(s);
        if (o instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new JsonException("Se esperaba un objeto JSON");
    }

    public static String write(Object o) { StringBuilder b = new StringBuilder(); w(o, b, 0, false); return b.toString(); }
    public static String pretty(Object o) { StringBuilder b = new StringBuilder(); w(o, b, 0, true); return b.toString(); }

    private static void w(Object o, StringBuilder b, int ind, boolean pretty) {
        if (o == null) { b.append("null"); }
        else if (o instanceof String s) str(s, b);
        else if (o instanceof Boolean || o instanceof Long || o instanceof Integer) b.append(o);
        else if (o instanceof Number n) {
            double d = n.doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e15) b.append((long) d); else b.append(d);
        } else if (o instanceof Map<?, ?> m) {
            if (m.isEmpty()) { b.append("{}"); return; }
            b.append('{'); boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) b.append(',');
                first = false;
                nl(b, ind + 1, pretty);
                str(String.valueOf(e.getKey()), b); b.append(pretty ? ": " : ":");
                w(e.getValue(), b, ind + 1, pretty);
            }
            nl(b, ind, pretty); b.append('}');
        } else if (o instanceof Iterable<?> it) {
            boolean first = true; b.append('[');
            for (Object x : it) {
                if (!first) b.append(',');
                first = false;
                nl(b, ind + 1, pretty);
                w(x, b, ind + 1, pretty);
            }
            if (!first) nl(b, ind, pretty);
            b.append(']');
        } else str(String.valueOf(o), b);
    }

    private static void nl(StringBuilder b, int ind, boolean pretty) {
        if (!pretty) return;
        b.append('\n');
        for (int k = 0; k < ind; k++) b.append("  ");
    }

    private static void str(String s, StringBuilder b) {
        b.append('"');
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        b.append('"');
    }

    private static final class P {
        final String s; int i;
        P(String s) { this.s = s; }
        void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        char peek() { if (i >= s.length()) throw new JsonException("Fin inesperado"); return s.charAt(i); }
        void expect(char c) { if (peek() != c) throw new JsonException("Se esperaba '" + c + "' en " + i); i++; }

        Object value() {
            char c = peek();
            if (c == '{') return obj();
            if (c == '[') return arr();
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            return number();
        }
        Map<String, Object> obj() {
            Map<String, Object> m = new LinkedHashMap<>();
            expect('{'); ws();
            if (peek() == '}') { i++; return m; }
            while (true) {
                ws(); String k = string(); ws(); expect(':'); ws();
                m.put(k, value()); ws();
                char c = peek(); i++;
                if (c == '}') return m;
                if (c != ',') throw new JsonException("Se esperaba ',' o '}' en " + (i - 1));
            }
        }
        List<Object> arr() {
            List<Object> l = new ArrayList<>();
            expect('['); ws();
            if (peek() == ']') { i++; return l; }
            while (true) {
                ws(); l.add(value()); ws();
                char c = peek(); i++;
                if (c == ']') return l;
                if (c != ',') throw new JsonException("Se esperaba ',' o ']' en " + (i - 1));
            }
        }
        String string() {
            expect('"');
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = peek(); i++;
                if (c == '"') return b.toString();
                if (c != '\\') { b.append(c); continue; }
                char e = peek(); i++;
                switch (e) {
                    case 'n' -> b.append('\n'); case 't' -> b.append('\t'); case 'r' -> b.append('\r');
                    case 'b' -> b.append('\b'); case 'f' -> b.append('\f');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw new JsonException("Escape \\u incompleto");
                        try { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); }
                        catch (NumberFormatException ex) { throw new JsonException("Escape \\u inválido"); }
                        i += 4;
                    }
                    default -> b.append(e);
                }
            }
        }
        Number number() {
            int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (st == i) throw new JsonException("Valor inesperado en " + i);
            String t = s.substring(st, i);
            try {
                if (t.matches("-?\\d+") && t.length() < 18) return Long.parseLong(t);
                return Double.parseDouble(t);
            } catch (NumberFormatException e) { throw new JsonException("Número inválido: " + t); }
        }
    }
}
