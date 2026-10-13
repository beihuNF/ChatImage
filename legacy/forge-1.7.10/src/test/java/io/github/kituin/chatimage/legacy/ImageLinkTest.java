package io.github.kituin.chatimage.legacy;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ImageLinkTest {
    @Test
    public void preservesAdjacentCodesAndSurroundingText() {
        List<Object> parts = ImageLink.parse("前[[CICode,url=https://example.org/a.png,name=图,nsfw=true]]"
                + "[[CICode,url=https://example.org/b.gif]]后", true, true);
        assertEquals(4, parts.size());
        assertEquals("前", parts.get(0));
        assertEquals("图", ((ImageLink) parts.get(1)).name);
        assertTrue(((ImageLink) parts.get(1)).nsfw);
        assertEquals("https://example.org/b.gif", ((ImageLink) parts.get(2)).url);
        assertEquals("后", parts.get(3));
    }

    @Test
    public void preservesQueryEqualsSignsAndCqEscapes() {
        ImageLink link = (ImageLink) ImageLink.parse(
                "[CQ:image,file=abc,url=https://example.org/a.png?x=a=b&amp;y=1&#44;2]", true, true).get(0);
        assertEquals("https://example.org/a.png?x=a=b&y=1,2", link.url);
    }

    @Test
    public void keepsLocalFilesAndInvalidCodesAsText() {
        String text = "[[CICode,url=file:///etc/passwd]][[CICode,name=bad]]";
        assertEquals(text, ImageLink.parse(text, true, true).get(0));
        assertFalse(ImageLink.isNetworkUrl("file:///etc/passwd"));
        assertFalse(ImageLink.isNetworkUrl("https:///no-host.png"));
    }

    @Test
    public void respectsDetectionSettingsAndSeparatesPunctuation() {
        String url = "https://example.org/image.PNG?token=a=b";
        List<Object> parts = ImageLink.parse("see " + url + "!", true, true);
        assertEquals(url, ((ImageLink) parts.get(1)).url);
        assertEquals("!", parts.get(2));
        assertEquals(url, ((ImageLink) ImageLink.parse("图片(" + url + ")。", true, true).get(1)).url);
        assertEquals(url, ImageLink.parse(url, false, true).get(0));
        String cq = "[CQ:image,url=https://example.org/a.gif]";
        assertEquals(cq, ImageLink.parse(cq, true, false).get(0));
        assertTrue(ImageLink.parse("[[CICode,url=https://example.org/no-extension]]", false, false).get(0) instanceof ImageLink);
    }
}
