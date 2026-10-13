package io.github.kituin.chatimage.legacy;

import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import org.junit.Test;
import static org.junit.Assert.*;

public class ImageDownloadTest {
    @Test
    public void limitsStreamsWithoutContentLength() throws Exception {
        assertEquals(4, ImageDownload.readBounded(new ByteArrayInputStream(new byte[4]), 4).length);
        try {
            ImageDownload.readBounded(new ByteArrayInputStream(new byte[5]), 4);
            fail("Expected byte limit");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("size limit"));
        }
    }

    @Test
    public void followsRelativeRedirectsAndRejectsHttpErrors() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/image");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.createContext("/image", exchange -> {
            exchange.sendResponseHeaders(200, 0); // Chunked, no Content-Length.
            exchange.getResponseBody().write(new byte[] {1, 2, 3});
            exchange.close();
        });
        server.createContext("/missing", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        try {
            assertArrayEquals(new byte[] {1, 2, 3}, ImageDownload.read(base + "/redirect", 3, 1000));
            try {
                ImageDownload.read(base + "/image", 2, 1000);
                fail("Expected chunked response byte limit");
            } catch (IOException expected) {
                assertTrue(expected.getMessage().contains("size limit"));
            }
            try {
                ImageDownload.read(base + "/missing", 3, 1000);
                fail("Expected HTTP error");
            } catch (IOException expected) {
                assertEquals("HTTP 404", expected.getMessage());
            }
        } finally {
            server.stop(0);
        }
    }
}
