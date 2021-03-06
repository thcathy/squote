package squote.domain;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigInteger;
import java.util.Date;

public class ForumThread {
	@Id
    private BigInteger id;
	
    private String url;
    private String title;
    private String source;
    private boolean isVisited = false;
    private boolean isWished = false;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
    public Date createdDate;

	public ForumThread() {}

    public ForumThread(String url, String title, String source, Date createdDate) {
		this.url = url;
		this.title = title;
		this.source = source;
		this.createdDate = createdDate;
	}
	
    @Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public String getUrl() { return url; }

	public String getTitle() { 	return title; }

	public String getSource() {	return source;}
	
	public Date getCreatedDate() {	return createdDate;}

	public boolean isVisited() {return isVisited;}
	public void setVisited(boolean isVisited) { this.isVisited = isVisited;}
	
	public boolean isWished() {return isWished;}
	public void setWished(boolean isWished) { this.isWished = isWished;}
}
