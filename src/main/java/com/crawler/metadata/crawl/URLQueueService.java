package com.crawler.metadata.crawl;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Service;

@Service
public class URLQueueService {

	// ---------------------------------------------------------------

	// 다중 url 동시 처리에 대해서 사용

	// seedUrl 마다 방문해야 할 url queue list

	private final Map<String, BlockingQueue<String>> urlQueues = new ConcurrentHashMap<>();

	// seedUrl 마다 방문한 rul set

	private final Map<String, Set<String>> visitedUrlSets = new ConcurrentHashMap<>();

	public void initializeQueue(String seedUrl) {
		urlQueues.putIfAbsent(seedUrl, new LinkedBlockingQueue<>()); // 대기열 목록 초기화
		visitedUrlSets.putIfAbsent(seedUrl, ConcurrentHashMap.newKeySet()); // 방문 url set 초기화
	}

	/**
	 * 
	 * seedUrl 에 할당된 queue link list 내부에 방문해야 할 url 추가
	 * @param seedUrl
	 * @param url
	 * 
	 */

	public void addUrl(String seedUrl, String url) {
		if (!visitedUrlSets.get(seedUrl).contains(url)) {
			urlQueues.get(seedUrl).offer(url);
		}
	}

	/**
	 * 
	 * seedUrl 에 할당된 queue link list 의 다음 순서에 방문할 url 반환
	 * 
	 * @param seedUrl
	 * 
	 * @return nextUrl
	 * 
	 */

	public String getNextUrl(String seedUrl) {
		return urlQueues.get(seedUrl).poll();
	}

	/**
	 * 
	 * 방문한 url 에 대해서, seedUrl 에 할당된 방문 url set에 저장
	 * 
	 * @param seedUrl
	 * 
	 * @param url
	 * 
	 */

	public void markAsVisited(String seedUrl, String url) {
		visitedUrlSets.get(seedUrl).add(url);
	}

	/**
	 * 
	 * seedUrl에 대해서, 현재 방문 순서에 해당하는 url 을 확인
	 * 
	 * @param seedUrl
	 * 
	 * @return currentUrl
	 * 
	 */

	public String pickCurrentUrl(String seedUrl) {
		return urlQueues.get(seedUrl).peek();
	}

	/**
	 * 
	 * seedUrl 의 방문해야 할 queue list 내부에 더 방문해야 할 url 이 존재하는지 확인
	 * @param seedUrl
	 * @return isMoreUrl flag
	 * 
	 */

	public boolean hasMoreUrls(String seedUrl) {
		return !urlQueues.get(seedUrl).isEmpty();
	}

	/**
	 * 
	 * 해당 seedUrl 의 queue 대기열에서 이전에 처리된 url(방문했는지 여부)인지 확인
	 * @param seedUrl
	 * @param url
	 * 
	 */

	public boolean isVisitedUrl(String seedUrl, String url) {
		return visitedUrlSets.get(seedUrl).contains(url);
	}

	// ---------------------------------------------------------------

	// 단일 url 처리에 대해서 사용.

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

	public synchronized String pickCurrentUrl() {
		return urlQueue.peek();
	}

}
