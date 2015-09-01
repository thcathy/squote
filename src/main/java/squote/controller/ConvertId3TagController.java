package squote.controller;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

@Controller
public class ConvertId3TagController {
	private static Logger log = LoggerFactory.getLogger(ConvertId3TagController.class);
	
	private static final String DEFAULT_FOLDER = "/tmp/mp3";
	private static final String DEFAULT_FROM_ENCODING = "big5";
	private static final String DEFAULT_TO_ENCODING = "utf-8";	
	
	@RequestMapping(value="/convertid3")
	public String convert(
			@RequestParam(value="action", required=false, defaultValue="") String action,
			@RequestParam(value="folder", required=false, defaultValue="") String folder,
			@RequestParam(value="fromEncoding", required=false, defaultValue="") String fromEncoding,
			@RequestParam(value="toEncoding", required=false, defaultValue="") String toEncoding,
			ModelMap modelMap) throws IOException {
		
		log.debug("convert: action[{}], folder[{}], encoding[{}>{}]", action, folder, fromEncoding, toEncoding);
		if (noInput(action, folder)) {
			givenDefaultValue(modelMap);
			return "id3tag";
		}
		
		Collection<File> files = FileUtils.listFiles(new File(folder), new String[]{"mp3"}, true);
		List<Map<String, String>> tags = files.stream()
			.map(f -> previewConvert(f, fromEncoding, toEncoding))
			.collect(Collectors.toList());
		modelMap.put("tags", tags);
		
		return "id3tag";
	}
	
	private Map<String, String> previewConvert(File f, String fromEncoding, String toEncoding) {
		Map<String, String> tags = new HashMap<>();
		tags.put("FilePath", f.getAbsolutePath());
				
		try {
			Mp3File mp3file = new Mp3File(f);
			if (mp3file.hasId3v1Tag()) {
	        	ID3v1 id3v1Tag = mp3file.getId3v1Tag();
	        	tags.put("Track"	, new String(id3v1Tag.getTrack().getBytes(fromEncoding), toEncoding));
	        	tags.put("Artist"	, new String(id3v1Tag.getArtist().getBytes(fromEncoding), toEncoding));
	        	tags.put("Title"	, new String(id3v1Tag.getTitle().getBytes(fromEncoding), toEncoding));
	        	tags.put("Album"	, new String(id3v1Tag.getAlbum().getBytes(fromEncoding), toEncoding));
	        	tags.put("Year"		, new String(id3v1Tag.getYear().getBytes(fromEncoding), toEncoding));	        	
	        	tags.put("GenreDesc", new String(id3v1Tag.getGenreDescription().getBytes(fromEncoding), toEncoding));
	        	tags.put("Comment"	, new String(id3v1Tag.getComment().getBytes(fromEncoding), toEncoding));
	        }
			
			if (mp3file.hasId3v2Tag()) {
	        	ID3v2 id3v2Tag = mp3file.getId3v2Tag();
	        	tags.put("Track2"			, new String(id3v2Tag.getTrack().getBytes(fromEncoding), toEncoding));
	        	tags.put("Artist2"			, new String(id3v2Tag.getArtist().getBytes(fromEncoding), toEncoding));
	        	tags.put("Title2"			, new String(id3v2Tag.getTitle().getBytes(fromEncoding), toEncoding));
	        	tags.put("Album2"			, new String(id3v2Tag.getAlbum().getBytes(fromEncoding), toEncoding));
	        	tags.put("Year2"			, new String(id3v2Tag.getYear().getBytes(fromEncoding), toEncoding));	        	
	        	tags.put("GenreDesc2"		, new String(id3v2Tag.getGenreDescription().getBytes(fromEncoding), toEncoding));
	        	tags.put("Composer"			, new String(id3v2Tag.getComposer().getBytes(fromEncoding), toEncoding));
	        	tags.put("Publisher"		, new String(id3v2Tag.getPublisher().getBytes(fromEncoding), toEncoding));
	        	tags.put("Original artist"	, new String(id3v2Tag.getOriginalArtist().getBytes(fromEncoding), toEncoding));
	        	tags.put("Album artist"		, new String(id3v2Tag.getAlbumArtist().getBytes(fromEncoding), toEncoding));
	        	tags.put("Copyright"		, new String(id3v2Tag.getCopyright().getBytes(fromEncoding), toEncoding));
	        	tags.put("URL"				, new String(id3v2Tag.getUrl().getBytes(fromEncoding), toEncoding));
	        	tags.put("Encoder"			, new String(id3v2Tag.getEncoder().getBytes(fromEncoding), toEncoding));	        	
	        }	        	        
		} catch (Exception e) {
			log.warn("Cannot process mp3: " + f.getAbsolutePath(), e);			
			tags.put("Exception", e.getMessage());
		}
		
		return tags;
	}

	private boolean noInput(String action, String folder) {		
		return StringUtils.isBlank(action) || StringUtils.isBlank(folder);
	}

	private void givenDefaultValue(ModelMap modelMap) {
		modelMap.addAttribute("folder", DEFAULT_FOLDER);
		modelMap.addAttribute("fromEncoding", DEFAULT_FROM_ENCODING);
		modelMap.addAttribute("toEncoding", DEFAULT_TO_ENCODING);		
	}	
}