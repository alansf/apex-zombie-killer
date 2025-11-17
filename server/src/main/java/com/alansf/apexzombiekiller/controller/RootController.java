package com.alansf.apexzombiekiller.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {
	@GetMapping(value = {"/", "/index", "/app", "/ui"})
	public String index() {
		return "forward:/index.html";
	}
}


