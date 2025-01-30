package com.crawler.metadata.controller;


import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.crawler.metadata.crawl.CrawlerService;

import lombok.extern.slf4j.Slf4j;

@RequestMapping("/crawler")
@RestController
@Slf4j
public class CrawlerController {

	private final CrawlerService crawlerService;
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	public CrawlerController(CrawlerService crawlerService) {
		this.crawlerService = crawlerService;
	}

	@GetMapping("/tag/chunk")
	public ResponseEntity<String> getCrawlingDataChunk(@RequestBody List<String> seedUrls) {
		crawlerService.crawlParallel(seedUrls);
		return ResponseEntity.ok("seed Url =====>>>>>" + seedUrls);
	}

	@GetMapping("/tag")
	public ResponseEntity<String> getCrawlingData(@RequestParam String seedUrl) {
		log.debug("crawl start-------------");
		crawlerService.crawl(seedUrl);
		return ResponseEntity.ok("seed URL: ======> " + seedUrl);
	}
}
