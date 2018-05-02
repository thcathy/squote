package squote.domain;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.TextIndexed;

import java.util.Date;

public class VisitedForumThread {	
	@Id	
	private final String url;

	@TextIndexed
	private final String title;
	
	private final Date visitOn;
		
	public VisitedForumThread(String url, String title, Date visitOn) {
		this.url = url;
		this.title = title;
		this.visitOn = visitOn;
	}

	public String getUrl() { return url;}
	public Date getVisitOn() { 	return visitOn;	}
	public String getTitle() { return title; }

	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
	
}
