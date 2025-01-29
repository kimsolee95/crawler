package com.crawler.metadata.crawl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.crawler.metadata.util.UrlUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CrawlerService {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final URLQueueService urlQueueService;

	private final MetadataExtractorService metadataExtractorService;

	private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public CrawlerService(
			URLQueueService urlQueueService, MetadataExtractorService metadataExtractorService,
			@Qualifier("crawlerExecutor") ThreadPoolTaskExecutor threadPoolTaskExecutor) {

		this.urlQueueService = urlQueueService;
		this.metadataExtractorService = metadataExtractorService;
		this.threadPoolTaskExecutor = threadPoolTaskExecutor;

	}

	/**
	 * 
	 * seed url list에 대해서 병렬처리를 통한
	 * 각각의 url 목록을 탐색해서 반환
	 * @param seedUrl list
	 * @return seedUrl map
	 * 
	 */

	public Map<String, List<String>> processParallelAllSeedUrls(List<String> seedUrls) {

		Map<String, List<String>> seedUrlsPageLinks = new ConcurrentHashMap<>();

		List<CompletableFuture<Void>> futures = seedUrls.stream()
				.map(seedUrl -> CompletableFuture.runAsync(() -> {
					// seedUrl 의 도메인만 추출해서 해당 부분부터 탐색해야 함.
					// https 가 안 붙으면 오류 난다;;;
//					String domainUrl = UrlUtils.getDomainName(seedUrl); // https 를 떼버림.
					// 탐색 시, domainUrl 을 parameter로 지정.
					List<String> urls = makeQueueListToBfs(seedUrl);
					seedUrlsPageLinks.put(seedUrl, urls); // 입력 data: 사용자로부터 받아온 seedUrl 을 그대로 사용하도록 한다.
				}, threadPoolTaskExecutor))
				.collect(Collectors.toList());

		// 동시에 실행
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		// 실행 후 down
		threadPoolTaskExecutor.shutdown();
		return seedUrlsPageLinks;
	}

	/**
	 * 
	 * seed url 을 진입점으로 하여 BFS 탐색을 통한 url 목록 구성 후, 반환
	 * @param seedUrl
	 * @return url List
	 * 
	 */
	public List<String> makeQueueListToBfs(String seedUrl) {
		// 1. robots.txt 파일 확인
		// 1-1. robots.txt 내부에 비허용하는 url 이 존재하면 filter 에 반영
		// 2. Queue를 이용한 BFS 탐색
		// 2-1. seedUrl 이 유효한 url 인지 확인
		// 2-2. queue 초기화
		urlQueueService.initializeQueue(seedUrl); // 받아온 값 그대로 queue map 에 입력해서 초기화.

		// 반환할 값 초기화
		List<String> collectedUrls = new ArrayList<>();
		// seed Url 을 대기열 list에 추가 - 진입점
		urlQueueService.addUrl(seedUrl, seedUrl);
		// BFS 탐색
		while (urlQueueService.hasMoreUrls(seedUrl)) {
			String currentUrl = urlQueueService.getNextUrl(seedUrl);
			log.debug("makeQueueListToBfs current url ---> {}", currentUrl);
			// jsoup connect -> html 문서
			Document jsoupDoc = metadataExtractorService.convertPageIntoDoc(currentUrl);
			// link 추출
			// 상대경로로 해놓은 거는 못 가져오고 있음
			// 유효성 검사 고쳐야 함...;;;
			// 고치면서 상대경로느느 domain url 을 붙여서 넣어주기
			List<String> urlList = metadataExtractorService.extractLinksFromPage(currentUrl, jsoupDoc);

			for (String urlLink : urlList) {

				// 1. link 유효성 check
				boolean isValidLink = metadataExtractorService.isValidLink(seedUrl, urlLink);
				if (isValidLink == false) {
					continue;
				}

				if (urlQueueService.isVisitedUrl(seedUrl, urlLink) == true) {
					continue;
				}

				// urlQueueService.markAsVisited(seedUrl, currentUrl); // 현재 url 을 방문한 url set에
				// 추가

				urlQueueService.markAsVisited(seedUrl, urlLink);
				urlQueueService.addUrl(seedUrl, urlLink); // seedUrl 의 대기열 목록에 추출한 url 을 추가
				collectedUrls.add(urlLink); // 반환할 List에 추출한 url 을 추가
			}
		}

		log.debug("[makeQueueListToBfs] collectedUrls ===> {}", collectedUrls);
		return collectedUrls;
	}

	/**
	 * 
	 * 2단계의 프로세스로 이뤄진 크롤링 작업 
	 * [1] url link 수집 - queue list 
	 * [2] 해당 url meta data 수집
	 * @param seedUrls
	 * 
	 */

	public void crawlParallel(List<String> seedUrls) { // seedUrlList

		// initialize.
		// [1] domain 만 가져오기
		// [2] robots.txt 찾아보기
		// [3] robots.txt 반영

		// 1. url 탐색 ( 동기 - BFS )
		// 여러 url에 대한 작업을 멀티스레드로 하되, BFS 자체는 단일로..
		// chunk 단위 지정 필요함.
		// blocking queue ------------->>> 어떠게 queue 를 제대로 전달할지?....
		// 해당 라인에서 계속해서 빈 배열 도출.... 동시성 문제???
		Map<String, List<String>> seedUrlsMap = processParallelAllSeedUrls(seedUrls);

		// 2. meta tag data 수집 ( 비동기 )
		for (Map.Entry<String, List<String>> seedMap : seedUrlsMap.entrySet()) {
			log.debug("seedMap ====> {}", seedMap);
		}

		// 3. kafka 같은 외부 대기열...
	}

	/**
	 * 
	 * 단일 패스로 입력받은 seedUrl에 대해서 link 탐색 + meta tag data 수집.
	 * @param seedUrl
	 * 
	 */

	public void crawl(String seedUrl) {

		urlQueueService.addUrl(seedUrl); // 시작점 url 을 queue 에 add
		Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
		// 리팩토링 버전 2.
		// exception, 재시도 로직, 지수백오프 추가 필요
		// 멀티 스레드 를 위한 list
		List<CompletableFuture<Void>> futures = new ArrayList<>();
		String domainUrl = UrlUtils.getDomainName(seedUrl);
		while (true) {

			String url = urlQueueService.getNextUrl(); // queue 내부 url 을 꺼내면서 자료구조 내에서 지우기
			if (url == null) { // 만약에 빈 자료구조 라면...
				log.info("[url list is empty. checking active tasks] =====");
				if (threadPoolTaskExecutor.getActiveCount() == 0) {
					log.info("[url list is empty. checking active tasks] ===== activeCount ===> 0");
					break;
				}

				try {
					Thread.sleep(5000); // 5초간 작업을 기다려보기...
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} // 메인 스레드 멈춘 후, 5초 경과 후 다시 확인
				continue;
			}

			if (visitedUrls.contains(url)) { // 방문한 적 있으면 그냥 다음 루프절로 순회
				continue;
			}

			// CompletableFuture 활용하여 멀티 스레드 사용
			CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
				try {
					// 1. Jsoup 의 document 객체로 웹 페이지를 변환하여 meta tag, link data 추출
					log.debug("url================== {}", url);
					Document jsoupDoc = metadataExtractorService.convertPageIntoDoc(url);
					// 1-2. meta tag 추출 - seed url
					Map<String, String> metadata = metadataExtractorService.extractMetadataStatic(jsoupDoc);
					// 1-3. link 추출 - seed url
					List<String> links = metadataExtractorService.extractLinksFromPage(domainUrl, jsoupDoc);
					// 1-4. 작업이 끝났으니 visited link 에 담아주기 - 비동기 내부에 하긴 했는데... 시점에 따라 그전에 해야할수도..
					// 있음...
					visitedUrls.add(url);
					// 2. 해당 page link 태그를 통한 나머지 방문할 url 탐색 ---> 리팩토링 시, 이 부분이 먼저 실행되어야 함.
					// domain page 그래프 탐색 - BFS ( 해당 도메인 queue link list에 방문할 url 추가 )
					for (String urlLink : links) {

						boolean isValidLink = metadataExtractorService.isValidLink(domainUrl, urlLink);
						if (false == isValidLink) {
							// 제외할 url 보관소가 따로 있다면 거기에 해당 url을 저장해주어야 할 거 같음;;
							continue; // 아예 다음 roop 로 보내기...
						}

						if (false == visitedUrls.contains(urlLink)) {
							urlQueueService.addUrl(urlLink);
						}
					}
				} catch (Exception e) {

					log.error("Error Processing URL : {}", url, e);

				}

			}, threadPoolTaskExecutor);

			futures.add(future);

		}

		// 모든 크롤링 작업 완료 대기
		// allOf ---> 동시에 모든 스레드 작업을 실행
		// join ---> 완료 될때까지 스레드 차단
		// test 결과, 성공적으로 추출된 웹사이트는 1개임. 프로세스 간 경쟁상태... 중복 방문 체크가 제대로 되지 않는 사례가 많음.
		// 어쩔 수 없이 2개의 단계로 나눠서 (해당 단계는 순차적으로 진행되어ㅇ야 함.) 해야 할 거 같음
		// 대기열( 방문해야 하는 list ) 목록을 만들고, 그걸 병렬로 실행해서 meta data를 뽑아내게 해야 rdb에 중복으로 data가
		// insert 되는 사례 예방
		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		log.info("All crawling tasks completed. Shutting down executor...");
		threadPoolTaskExecutor.shutdown();
	}

	private void processMetadata(String url, Map<String, String> metadata) {
		log.info("[processMetadata] Metadata for url : {}", url);
		metadata.forEach((key, value) -> log.error("key : {}  value: {}", key, value));
	}

	private boolean isDynamicPage(String url) {

		try {
			Response response = Jsoup.connect(url).execute();
			String content = response.body();
			boolean hasScript = content.contains("<script");
			boolean hasPlaceholder = content.contains("{{") || content.contains("ng-");
			return hasScript || hasPlaceholder || response.header("X-Powered-By") != null;
		} catch (IOException e) {
			return true; // If an error occurs, assume dynamic page
		}
	}

}