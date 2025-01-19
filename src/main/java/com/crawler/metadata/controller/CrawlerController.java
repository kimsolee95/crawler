package com.crawler.metadata.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.crawler.metadata.crawl.CrawlerService;

@RestController
@RequestMapping("/crawler/metadata")
public class CrawlerController {

	private final CrawlerService crawlerService;

	public CrawlerController(CrawlerService crawlerService) {
		this.crawlerService = crawlerService;
	}
	
	@GetMapping("/tag")
	public ResponseEntity<String> getCrawlingData(@RequestParam String seedUrl) {
	
		crawlerService.crawl(seedUrl);
		return ResponseEntity.ok("seed URL: ======> " + seedUrl);
	}
	
}
