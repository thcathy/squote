package squote.domain;
import java.util.Date;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.springframework.data.annotation.Id;

public class WishList {	
	@Id	
	public final String text;
	
	public final Date createdDate;
		
	public WishList(String text) {
		this.text = text;
		this.createdDate = new Date();
	}
	
	@Override
	public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
	
}
