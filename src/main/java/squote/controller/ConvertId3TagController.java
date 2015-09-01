package squote.controller;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
		modelMap.put("tagList", tags);
		
		return "id3tag";
	}
	
	private Map<String, String> previewConvert(File f, String fromEncoding, String toEncoding) {
		Map<String, String> tags = new HashMap<>();
		tags.put("FilePath", f.getAbsolutePath());
		
		try {
			Mp3File mp3file = new Mp3File(f);
			if (mp3file.hasId3v1Tag()) {
	        	ID3v1 id3v1Tag = mp3file.getId3v1Tag();
	        	tags.put("Track"	, convertEncoding(id3v1Tag.getTrack(),fromEncoding, toEncoding));
	        	tags.put("Artist"	, convertEncoding(id3v1Tag.getArtist(),fromEncoding, toEncoding));
	        	tags.put("Title"	, convertEncoding(id3v1Tag.getTitle(),fromEncoding, toEncoding));
	        	tags.put("Album"	, convertEncoding(id3v1Tag.getAlbum(),fromEncoding, toEncoding));
	        	tags.put("Year"		, convertEncoding(id3v1Tag.getYear(),fromEncoding, toEncoding));	        	
	        	tags.put("GenreDesc", convertEncoding(id3v1Tag.getGenreDescription(),fromEncoding, toEncoding));
	        	tags.put("Comment"	, convertEncoding(id3v1Tag.getComment(),fromEncoding, toEncoding));
	        }
			
			if (mp3file.hasId3v2Tag()) {
	        	ID3v2 id3v2Tag = mp3file.getId3v2Tag();
	        	tags.put("Track2"			, convertEncoding(id3v2Tag.getTrack(),fromEncoding, toEncoding));
	        	tags.put("Artist2"			, convertEncoding(id3v2Tag.getArtist(),fromEncoding, toEncoding));
	        	tags.put("Title2"			, convertEncoding(id3v2Tag.getTitle(),fromEncoding, toEncoding));
	        	tags.put("Album2"			, convertEncoding(id3v2Tag.getAlbum(),fromEncoding, toEncoding));
	        	tags.put("Year2"			, convertEncoding(id3v2Tag.getYear(),fromEncoding, toEncoding));	        	
	        	tags.put("GenreDesc2"		, convertEncoding(id3v2Tag.getGenreDescription(),fromEncoding, toEncoding));
	        	tags.put("Composer"			, convertEncoding(id3v2Tag.getComposer(),fromEncoding, toEncoding));
	        	tags.put("Publisher"		, convertEncoding(id3v2Tag.getPublisher(),fromEncoding, toEncoding));
	        	tags.put("Original artist"	, convertEncoding(id3v2Tag.getOriginalArtist(),fromEncoding, toEncoding));
	        	tags.put("Album artist"		, convertEncoding(id3v2Tag.getAlbumArtist(),fromEncoding, toEncoding));
	        	tags.put("Copyright"		, convertEncoding(id3v2Tag.getCopyright(),fromEncoding, toEncoding));
	        	tags.put("URL"				, convertEncoding(id3v2Tag.getUrl(),fromEncoding, toEncoding));
	        	tags.put("Encoder"			, convertEncoding(id3v2Tag.getEncoder(),fromEncoding, toEncoding));	        	
	        }	        	        
		} catch (Exception e) {
			log.warn("Cannot process mp3: " + f.getAbsolutePath(), e);			
			tags.put("Exception", e.getMessage());
		}
		log.debug(tags.toString());
		return tags;
	}
	
	private String convertEncoding(String input, String from, String to) throws UnsupportedEncodingException {
		if (StringUtils.isBlank(input)) return "";
		return new String(input.getBytes(from), to);
		
		//ByteBuffer bb = Charset.forName(from).encode(input);
        //return Charset.forName(to).decode(bb).toString();
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