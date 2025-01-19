package com.crawler.metadata.crawl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

@Service
public class MetadataExtractorService {
	
	public Map<String, String> extractMetadataStatic(String url) throws IOException {
		Document doc = Jsoup.connect(url).get();
		Map<String, String> metadata = new HashMap<>();
		
		doc.select("meta").forEach(meta -> {
			String name = meta.attr("name");
			String content = meta.attr("content");
			if (!name.isEmpty()) {
				metadata.put(name, content);
			}
		});
		return metadata;
	}
	
	public Map<String, String> extractMetadataDynamic(String url) throws IOException {
		try (Playwright playwright = Playwright.create()) {
			Browser browser = playwright.chromium().launch();
			Page page = browser.newPage();
			page.navigate(url);
			String content = page.content();
			
			Document doc = Jsoup.parse(content);
			Map<String, String> metadata = new HashMap<>();
			
			doc.select("meta").forEach(meta -> {
				String name = meta.attr("name");
				String contentValue = meta.attr("content");
				if (!name.isEmpty()) {
					metadata.put(name, contentValue);
				}
			});
			return metadata;
		}
		
	}

}
