package squote.controller.repository;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import squote.domain.WishList;
import squote.domain.repository.WishListRepository;

@RequestMapping("/forum/wishlist")
@Controller
public class WishListController {	
	private static Logger log = LoggerFactory.getLogger(WishListController.class);
		
	@Autowired WishListRepository wishListRepo;
			
	@RequestMapping(value = "/")	
	public String wishlist(
			@RequestParam(value="newItem", required=false) String newItem, 
			ModelMap modelMap) 
	{
		log.info("WishList: newItem[{}]", newItem);
		
		if (StringUtils.isNotBlank(newItem)) createNewItem(newItem);
		
		modelMap.put("items", wishListRepo.findAll());
		return "forum/wishlist";
	}
	
	@RequestMapping(value = "/delete")
	public String delete(
			@RequestParam(value="item", required=false, defaultValue="") String item,
			ModelMap modelMap) {
		log.info("WishList: delete [{}]", item);
		
		wishListRepo.delete(item);
		modelMap.put("items", wishListRepo.findAll());
		return "forum/wishlist";
	}

	private void createNewItem(String newItem) {
		wishListRepo.save(new WishList(newItem));		
	}		
}
