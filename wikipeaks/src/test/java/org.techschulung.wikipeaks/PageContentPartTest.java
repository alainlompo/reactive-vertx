package org.techschulung.wikipeaks;
import org.junit.Test;
import org.junit.Assert;


public class PageContentPartTest {

    @Test
    public void parseJsonTest() {
        String json = String.format("{'pageName': '%s', 'contentPart': '%s'}", "mango", "Lots of mangos here");
        PageContentPart contentPart = PageContentPart.parseJson(json);
        Assert.assertTrue(contentPart != null);
        String expected = "Name: mango, content: Lots of mangos here";
        Assert.assertEquals(expected, contentPart.toString());
    }
}