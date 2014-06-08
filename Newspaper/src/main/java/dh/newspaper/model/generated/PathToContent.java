package dh.newspaper.model.generated;

// THIS CODE IS GENERATED BY greenDAO, EDIT ONLY INSIDE THE "KEEP"-SECTIONS

// KEEP INCLUDES - put your custom includes here
// KEEP INCLUDES END
/**
 * Entity mapped to table PATH_TO_CONTENT.
 */
public class PathToContent {

    private Long id;
    /** Not-null value. */
    private String urlPattern;
    /** Not-null value. */
    private String xpath;
    private String language;
    private Integer priority;
    private Boolean enable;
    private java.util.Date lastUpdate;

    // KEEP FIELDS - put your custom fields here
    // KEEP FIELDS END

    public PathToContent() {
    }

    public PathToContent(Long id) {
        this.id = id;
    }

    public PathToContent(Long id, String urlPattern, String xpath, String language, Integer priority, Boolean enable, java.util.Date lastUpdate) {
        this.id = id;
        this.urlPattern = urlPattern;
        this.xpath = xpath;
        this.language = language;
        this.priority = priority;
        this.enable = enable;
        this.lastUpdate = lastUpdate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /** Not-null value. */
    public String getUrlPattern() {
        return urlPattern;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    /** Not-null value. */
    public String getXpath() {
        return xpath;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public java.util.Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(java.util.Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    // KEEP METHODS - put your custom methods here
	@Override
	public String toString() {
		return String.format("[%s: #%d, pattern='%s', xpath='%s', priority=%d]", this.getClass().getSimpleName(), this.id, this.urlPattern, this.xpath, this.priority);
	}
    // KEEP METHODS END

}