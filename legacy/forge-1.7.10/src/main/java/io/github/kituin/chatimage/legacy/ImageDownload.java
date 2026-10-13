package io.github.kituin.chatimage.legacy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class ImageDownload {
    private ImageDownload() {}

    static byte[] read(String url, int maxBytes, int timeoutMillis) throws IOException {
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (!ImageLink.isNetworkUrl(url)) throw new IOException("Only HTTP(S) images are supported");
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "ChatImage/1.7.10");
            try {
                int status = connection.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Redirect without Location");
                    url = new URL(new URL(url), location).toExternalForm();
                    continue;
                }
                if (status != 200) throw new IOException("HTTP " + status);
                if (connection.getContentLengthLong() > maxBytes) throw new IOException("Image exceeds size limit");
                try (InputStream stream = connection.getInputStream()) {
                    return readBounded(stream, maxBytes);
                }
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Too many redirects");
    }

    static byte[] readBounded(InputStream stream, int maxBytes) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = stream.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Image download cancelled");
            if (count > maxBytes - output.size()) throw new IOException("Image exceeds size limit");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
