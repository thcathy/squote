package squote.controller;

import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_ALBUM;
import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_ALBUM_ARTIST;
import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_ARTIST;
import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_COMPOSER;
import static com.mpatric.mp3agic.AbstractID3v2Tag.ID_ENCODER;
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

import com.mpatric.mp3agic.EncodedText;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v2Frame;
import com.mpatric.mp3agic.ID3v2FrameSet;
import com.mpatric.mp3agic.Mp3File;

@Controller
public class ConvertId3TagController {
	private static Logger log = LoggerFactory.getLogger(ConvertId3TagController.class);
	
	private static final String DEFAULT_INPUT_FOLDER = "/tmp/mp3";
	private static final String DEFAULT_OUTPUT_FOLDER = "/tmp/decoded";
	private static final String DEFAULT_FROM_ENCODING = "big5";
	private static final String DEFAULT_TO_ENCODING = "utf-8";	
	
	@RequestMapping(value="/convertid3")
	public String convert(
			@RequestParam(value="action", required=false, defaultValue="") String action,
			@RequestParam(value="inputFolder", required=false, defaultValue="") String inputFolder,
			@RequestParam(value="outputFolder", required=false, defaultValue="") String outputFolder,
			@RequestParam(value="fromEncoding", required=false, defaultValue="") String fromEncoding,
			@RequestParam(value="toEncoding", required=false, defaultValue="") String toEncoding,
			ModelMap modelMap) throws IOException {
		
		log.debug("convert: action[{}], folder[{}>{}], encoding[{}>{}]", action, inputFolder, outputFolder, fromEncoding, toEncoding);
		if (noInput(action, inputFolder)) {
			givenDefaultValue(modelMap);
			return "id3tag";
		}
		
		File folderObj = new File(inputFolder);
		if (!folderObj.isDirectory()) inputFolder = folderObj.getParent();
		Collection<File> files = FileUtils.listFiles(new File(inputFolder), new String[]{"mp3"}, true);
		
		List<Map<String, String>> tags = files.stream()
											.map(f -> convertTagsText(f, fromEncoding, toEncoding))
											.filter(f -> f != null)
											.map(f -> save("save".equalsIgnoreCase(action), f, outputFolder))
											.map(f -> convertToMap(f))
											.collect(Collectors.toList());
		
		modelMap.put("tagList", tags);
		modelMap.put("inputFolder", inputFolder);
		modelMap.put("outputFolder", outputFolder);
		modelMap.put("fromEncoding", fromEncoding);
		modelMap.put("toEncoding", toEncoding);
		
		return "id3tag";
	}
	
	private Mp3File save(boolean isSave, Mp3File mp3, String outputFolder) {
		if (isSave) {
			File outputFolderObj = new File(outputFolder);
			if (!outputFolderObj.exists()) outputFolderObj.mkdirs();
			
			String newFilePath = outputFolder + File.separator + new File(mp3.getFilename()).getName();
			try {
				mp3.save(newFilePath);
				mp3 = new Mp3File(newFilePath);
			} catch (ArrayIndexOutOfBoundsException e) {
				try {
					mp3.getId3v2Tag().clearAlbumImage();
					mp3.save(newFilePath);
					mp3 = new Mp3File(newFilePath);
				} catch (Exception e1) {
					log.error("Cannot save mp3: " + mp3.getFilename(), e);
				}
			} catch (Exception e) {
				log.error("Cannot save mp3: " + mp3.getFilename(), e);
			}
		}
			
		return mp3;
	}
	
	private Mp3File convertTagsText(File f, String fromEncoding, String toEncoding) {
		log.debug("convertTagsText file: {}", f.getAbsoluteFile());
		
		try {
			Mp3File mp3 = new Mp3File(f);
			ID3v2 id3v2Tag = mp3.getId3v2Tag();
			decodeText(id3v2Tag, ID_ARTIST,fromEncoding, toEncoding);
        	decodeText(id3v2Tag, ID_TITLE,fromEncoding, toEncoding);
        	decodeText(id3v2Tag, ID_ALBUM,fromEncoding, toEncoding);
        	decodeText(id3v2Tag, ID_COMPOSER,fromEncoding, toEncoding);
        	decodeText(id3v2Tag, ID_PUBLISHER,fromEncoding, toEncoding);
        	decodeText(id3v2Tag, ID_ORIGINAL_ARTIST,fromEncoding, toEncoding);
        	decodeText(id3v2Tag, ID_ALBUM_ARTIST,fromEncoding, toEncoding);
        	decodeText(id3v2Tag, ID_ENCODER, fromEncoding, toEncoding);
        	return mp3;
		} catch (Exception e) {
			log.warn("Cannot process mp3: " + f.getAbsolutePath(), e);
			return null;
		}
	}
	
	private Map<String, String> convertToMap(Mp3File f) {
		Map<String, String> tags = new HashMap<>();
		tags.put("FilePath", f.getFilename());
		
    	ID3v2 id3v2Tag = f.getId3v2Tag();
    	tags.put(ID_ARTIST			, id3v2Tag.getArtist());
    	tags.put(ID_TITLE			, id3v2Tag.getTitle());
    	tags.put(ID_ALBUM			, id3v2Tag.getAlbum());
    	tags.put(ID_COMPOSER		, id3v2Tag.getComposer());
    	tags.put(ID_PUBLISHER		, id3v2Tag.getPublisher());
    	tags.put(ID_ORIGINAL_ARTIST	, id3v2Tag.getOriginalArtist());
    	tags.put(ID_ALBUM_ARTIST	, id3v2Tag.getAlbumArtist());
    	tags.put(ID_ENCODER			, id3v2Tag.getEncoder());
    	
		log.debug(tags.toString());
		return tags;
	}
	
	private void decodeText(ID3v2 id3v2Tag, String tagId, String fromEncoding, String toEncoding) throws UnsupportedEncodingException {
		ID3v2FrameSet set = id3v2Tag.getFrameSets().get(tagId);
		if (set == null) return;
		ID3v2Frame frame = id3v2Tag.getFrameSets().get(tagId).getFrames().get(0);
		byte[] utfBytes = new String(ArrayUtils.remove(frame.getData(), 0),"BIG5").trim().getBytes("UTF-8");	// remove leading byte and convert
        frame.setData(ArrayUtils.add(utfBytes, 0, EncodedText.TEXT_ENCODING_UTF_8));		
	}

	private boolean noInput(String action, String folder) {		
		return StringUtils.isBlank(action) || StringUtils.isBlank(folder);
	}

	private void givenDefaultValue(ModelMap modelMap) {
		modelMap.addAttribute("inputFolder", DEFAULT_INPUT_FOLDER);
		modelMap.addAttribute("outputFolder", DEFAULT_OUTPUT_FOLDER);
		modelMap.addAttribute("fromEncoding", DEFAULT_FROM_ENCODING);
		modelMap.addAttribute("toEncoding", DEFAULT_TO_ENCODING);		
	}	
}