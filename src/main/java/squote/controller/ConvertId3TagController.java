package squote.controller;

import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_ALBUM;
import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_ALBUM_ARTIST;
import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_ARTIST;
import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_COMPOSER;
import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_ORIGINAL_ARTIST;
import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_PUBLISHER;
import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_TITLE;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v2Frame;
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
		
		File folderObj = new File(folder);
		if (!folderObj.isDirectory()) folder = folderObj.getParent();
		Collection<File> files = FileUtils.listFiles(new File(folder), new String[]{"mp3"}, true);
		
		List<Map<String, String>> tags = files.stream()
			.map(f -> previewConvert(f, fromEncoding, toEncoding))
			.collect(Collectors.toList());
		
		modelMap.put("tagList", tags);
		modelMap.put("folder", folder);
		modelMap.put("fromEncoding", fromEncoding);
		modelMap.put("toEncoding", toEncoding);
		
		return "id3tag";
	}
	
	private Map<String, String> previewConvert(File f, String fromEncoding, String toEncoding) {
		Map<String, String> tags = new HashMap<>();
		tags.put("FilePath", f.getAbsolutePath());
		
		try {
			Mp3File mp3file = new Mp3File(f);
        	ID3v2 id3v2Tag = mp3file.getId3v2Tag();
        	tags.put(ID_ARTIST			, decodeText(id3v2Tag, ID_ARTIST,fromEncoding, toEncoding));
        	tags.put(ID_TITLE			, decodeText(id3v2Tag, ID_TITLE,fromEncoding, toEncoding));
        	tags.put(ID_ALBUM			, decodeText(id3v2Tag, ID_ALBUM,fromEncoding, toEncoding));
        	tags.put(ID_COMPOSER		, decodeText(id3v2Tag, ID_COMPOSER,fromEncoding, toEncoding));
        	tags.put(ID_PUBLISHER		, decodeText(id3v2Tag, ID_PUBLISHER,fromEncoding, toEncoding));
        	tags.put(ID_ORIGINAL_ARTIST	, decodeText(id3v2Tag, ID_ORIGINAL_ARTIST,fromEncoding, toEncoding));
        	tags.put(ID_ALBUM_ARTIST	, decodeText(id3v2Tag, ID_ALBUM_ARTIST,fromEncoding, toEncoding));
		} catch (Exception e) {
			log.warn("Cannot process mp3: " + f.getAbsolutePath(), e);			
			tags.put("Exception", e.getMessage());
		}
		log.debug(tags.toString());
		return tags;
	}
	
	private String decodeText(ID3v2 id3v2Tag, String tagId, String fromEncoding, String toEncoding) throws UnsupportedEncodingException {
		ID3v2Frame frame = id3v2Tag.getFrameSets().get(tagId).getFrames().get(0);
		return new String(new String(ArrayUtils.remove(frame.getData(), 0), fromEncoding).trim().getBytes(toEncoding), toEncoding);
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