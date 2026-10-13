package io.github.kituin.chatimage.legacy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Network-only chat image metadata. Parsing never downloads or accesses local files. */
public final class ImageLink {
    private static final Pattern TOKENS = Pattern.compile(
            "\\[\\[CICode,.*?\\]\\]|\\[CQ:image,.*?\\]|https?://[^\\s<>\\[\\]]+",
            Pattern.CASE_INSENSITIVE);
    public final String url;
    public final String name;
    public final String prefix;
    public final String suffix;
    public final boolean nsfw;

    private ImageLink(String url, String name, String prefix, String suffix, boolean nsfw) {
        this.url = url;
        this.name = name;
        this.prefix = prefix;
        this.suffix = suffix;
        this.nsfw = nsfw;
    }

    public static boolean isNetworkUrl(String value) {
        try {
            URI uri = new URI(value);
            return value.length() <= 2048 && uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (Exception e) {
            return false;
        }
    }

    static ImageLink network(String url) {
        if (!isNetworkUrl(url)) throw new IllegalArgumentException("Invalid HTTP(S) URL");
        return new ImageLink(url, null, "[", "]", false);
    }

    public static List<Object> parse(String text, boolean detectUrls, boolean detectCq) {
        List<Object> result = new ArrayList<>();
        Matcher matcher = TOKENS.matcher(text);
        int position = 0;
        while (matcher.find()) {
            String token = matcher.group();
            ImageLink link = null;
            int end = matcher.end();
            if (token.startsWith("[[CICode,")) {
                link = fromArguments(token.substring(9, token.length() - 2), false);
            } else if (token.startsWith("[CQ:image,") && detectCq) {
                link = fromArguments(token.substring(10, token.length() - 1), true);
            } else if (detectUrls && token.toLowerCase(Locale.ROOT).startsWith("http")) {
                // Sentence punctuation is outside a bare URL; explicit CICode preserves it.
                while (!token.isEmpty() && ".,;!，。；！）\"”".indexOf(token.charAt(token.length() - 1)) >= 0) {
                    token = token.substring(0, token.length() - 1);
                    end--;
                }
                while (token.endsWith(")") && count(token, ')') > count(token, '(')) {
                    token = token.substring(0, token.length() - 1);
                    end--;
                }
                if (isNetworkUrl(token)) {
                    String path = URI.create(token).getPath().toLowerCase(Locale.ROOT);
                    if (path.matches(".*\\.(png|jpe?g|jfif|gif|bmp)(!thumbnail)?$")) {
                        link = new ImageLink(token, null, "[", "]", false);
                    }
                }
            }
            if (link != null) {
                if (matcher.start() > position) result.add(text.substring(position, matcher.start()));
                result.add(link);
                position = end;
            }
        }
        if (position < text.length()) result.add(text.substring(position));
        if (result.isEmpty()) result.add(text);
        return result;
    }

    private static ImageLink fromArguments(String arguments, boolean cq) {
        String url = null;
        String name = null;
        String prefix = "[";
        String suffix = "]";
        boolean nsfw = false;
        for (String argument : arguments.split(",")) {
            String[] pair = argument.split("=", 2);
            if (pair.length != 2) return null;
            String value = cq ? unescapeCq(pair[1].trim()) : pair[1].trim();
            switch (pair[0].trim()) {
                case "url": url = value; break;
                case "name": name = value; break;
                case "nsfw": nsfw = Boolean.parseBoolean(value); break;
                case "pre": prefix = value; break;
                case "suf": suffix = value; break;
                default: break;
            }
        }
        return url != null && isNetworkUrl(url) ? new ImageLink(url, name, prefix, suffix, nsfw) : null;
    }

    private static String unescapeCq(String value) {
        return value.replace("&#44;", ",").replace("&#91;", "[")
                .replace("&#93;", "]").replace("&amp;", "&");
    }

    private static int count(String value, char character) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) == character) count++;
        return count;
    }
}
