package org.techschulung.wikipeaks;

/**
 * Created by alompo on 26.01.18.
 */
public class PageContentPart {
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
}
