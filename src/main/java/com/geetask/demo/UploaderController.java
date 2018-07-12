package com.geetask.demo;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.geetask.chunked.AbstractStorage;

@Controller
public class UploaderController {

	@Autowired
	private AbstractStorage storage;
	

	@GetMapping("/uploader")
	public String handle(ModelMap map) {
		map.addAttribute("uuid", UUID.randomUUID());
		return "index";
	}
	
	@PostMapping("/uploader")
	@ResponseBody
	public String handle(@RequestParam("file") MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws IOException {
		return storage.upload(file.getInputStream(),"test",request, response);
	}
}
