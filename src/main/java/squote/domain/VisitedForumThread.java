package squote.domain;
import java.util.Date;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;

public class VisitedForumThread {	
	@Id	
	private final String url;
	
	private final Date visitOn;
		
	public VisitedForumThread(String url, Date visitOn) {
		this.url = url;
		this.visitOn = visitOn;
	}

	public String getUrl() { return url;}
	public Date getVisitOn() { 	return visitOn;	}

	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
	
}
