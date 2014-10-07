package squote.controller;


public class AbstractController {
	protected String path;
	
	public AbstractController(String path) {
		this.path = path;
	}
	
	public String page(String subPath) {
		if (org.apache.commons.lang3.StringUtils.isBlank(subPath)) throw new IllegalArgumentException("subPath cannot be blank");
		return path + subPath;
	}
}
