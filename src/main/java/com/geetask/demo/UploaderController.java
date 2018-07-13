package com.geetask.demo;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.geetask.chunked.AbstractStorage;
import com.geetask.chunked.ChunkResponse;
import com.geetask.chunked.InitResponse;

@Controller
public class UploaderController {

	@Autowired
	private AbstractStorage storage;
	

	@GetMapping("/uploader")
	public String handle() {
		return "index";
	}
	
	@PostMapping("/init")
	@ResponseBody
	public InitResponse initUpload(HttpServletRequest request, HttpServletResponse response) {
		return storage.initUpload("test", request, response);
	}
	
	@PostMapping("/uploader")
	@ResponseBody
	public ChunkResponse handle(@RequestParam("file") MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws IOException {
		return storage.upload(file.getInputStream(),request, response);
	}
	
}
