package com.crawler.metadata.crawl;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
//@Slf4j
public class CrawlerService {
	
//	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private final URLQueueService urlQueueService;
	private final MetadataExtractorService metadataExtractorService;
	private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
	
    public CrawlerService(URLQueueService urlQueueService, MetadataExtractorService metadataExtractorService,
            ThreadPoolTaskExecutor threadPoolTaskExecutor) {
    		
    	this.urlQueueService = urlQueueService;
    	this.metadataExtractorService = metadataExtractorService;
    	this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }
	
	public void crawl(String seedUrl) {
		urlQueueService.addUrl(seedUrl);
		while (true) {
			String url = urlQueueService.getNextUrl();
			if (url == null) {
				break;
			}
			threadPoolTaskExecutor.submit(() -> {
				try {
					if (isDynamicPage(url)) {
						Map<String, String> metadata = metadataExtractorService.extractMetadataDynamic(seedUrl);
						processMetadata(url, metadata);
					} else {
						Map<String, String> metadata = metadataExtractorService.extractMetadataStatic(seedUrl);
						processMetadata(url, metadata);	
					}
				} catch (Exception e) {
//					logger.debug("{}  crawl error", e.getMessage());
				}
			});
		}
		
	}
	
    private void processMetadata(String url, Map<String, String> metadata) {
//        ("Metadata for URL: {}", url);
        metadata.forEach((key, value) -> System.out.println(key + ": " + value));
    }
	
	private boolean isDynamicPage(String url) {
        return url.contains("dynamic") || url.endsWith(".js");
	}
	
}
