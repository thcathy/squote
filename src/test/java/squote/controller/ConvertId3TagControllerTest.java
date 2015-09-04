package squote.controller;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ModelMap;

import squote.SpringQuoteWebApplication;

import com.mpatric.mp3agic.AbstractID3v2Tag;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = SpringQuoteWebApplication.class)
@ActiveProfiles("dev")
public class ConvertId3TagControllerTest {
	@Autowired ConvertId3TagController controller;
	
	private MockMvc mockMvc;
	   
    @Before
    public void setup() {    	
    	this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();        
    }
			
	@Test
	public void convert_givenNoInput_shouldReturnWithDefaultValue() throws Exception {
		ModelMap modelMap = mockMvc.perform(get("/convertid3"))
				.andExpect(status().isOk())				
				.andExpect(view().name("id3tag")).andReturn().getModelAndView().getModelMap();
		
		assertEquals("/tmp/mp3", modelMap.get("folder"));
		assertEquals("big5", modelMap.get("fromEncoding"));
		assertEquals("utf-8", modelMap.get("toEncoding"));
	}	
	
	@Test
	public void convert_givenPreviewWithBig5Mp3_shouldReturnWithPreviewMap() throws Exception {
		URL uri = this.getClass().getClassLoader().getResource("big5.mp3");
		
		ModelMap modelMap = mockMvc.perform(
				get("/convertid3")
				.param("action", "preview")
				.param("folder", uri.getPath())
				.param("fromEncoding", "BIG5")
				.param("toEncoding", "UTF-8")
		).andExpect(status().isOk())
		.andExpect(view().name("id3tag")).andReturn().getModelAndView().getModelMap();
		
		assertEquals(uri.getPath().replace("/big5.mp3", ""), modelMap.get("folder"));
		assertEquals("BIG5", modelMap.get("fromEncoding"));
		assertEquals("UTF-8", modelMap.get("toEncoding"));
		
		List<Map<String, String>> tagList = (List) modelMap.get("tagList");
		assertEquals(1, tagList.size());
		assertEquals(uri.getPath(), tagList.get(0).get("FilePath"));
		assertEquals("李克勤", tagList.get(0).get(AbstractID3v2Tag.ID_ARTIST));
		assertEquals("我克勤", tagList.get(0).get(AbstractID3v2Tag.ID_ALBUM));
		assertEquals("你最重要", tagList.get(0).get(AbstractID3v2Tag.ID_TITLE));
		
	}
}
