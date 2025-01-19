package com.crawler.metadata.crawl;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class URLQueueService {
	
	private final Queue<String> urlQueue = new LinkedList<>();
	private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
	
	public synchronized void addUrl(String url) {
		if (!visitedUrls.contains(url)) {
			urlQueue.offer(url);
		}
	}
	
	public synchronized String getNextUrl() {
		return urlQueue.poll();
	}
	
	public synchronized void markAsVisited(String url) {
		visitedUrls.add(url);
	}

}
