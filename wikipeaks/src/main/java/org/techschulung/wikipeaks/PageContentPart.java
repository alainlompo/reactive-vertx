package org.techschulung.wikipeaks;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.Serializable;

/**
 * Created by alompo on 26.01.18.
 */
public class PageContentPart implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PageContentPart.class);

    private String pageName;
    private String contentPart;

    public PageContentPart(String pageName, String contentPart) {
        this.pageName = pageName;
        this.contentPart = contentPart;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getContentPart() {
        return contentPart;
    }

    public void setContentPart(String contentPart) {
        this.contentPart = contentPart;
    }

    public String toString() {
        return String.format("Name: %s, content: %s", pageName, contentPart);
    }

    public String toJsonString() {
        Gson gsonifier = new Gson();
        return gsonifier.toJson(this);
    }

    public static PageContentPart parseJson(String jsonData) {

        // TODO: there is a JsonParser in vertx. Use it once this part is running correctly
        JsonElement element = new JsonParser().parse(jsonData);
        JsonObject object = element.getAsJsonObject();
        return new PageContentPart(
                object.get("pageName").getAsString(),
                object.get("contentPart").getAsString()
        );
    }
}
