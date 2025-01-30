package com.crawler.metadata.crawl;


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.tomcat.util.buf.StringUtils;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.impl.Connection;
import com.microsoft.playwright.options.LoadState;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MetadataExtractorService {
	
	private final Logger log = LoggerFactory.getLogger(getClass());

	/**
	 * 페이지의 링크 내 url 수집
	 * 
	 * @param jsoupDoc
	 */
	public List<String> extractLinksFromPage(String url, Document jsoupDoc) {
		List<String> links = jsoupDoc.select("a[href]").stream().map(element -> element.attr("abs:href"))
				.filter(link -> isValidLink(url, link)) // 이 함수 수정 필요... ( link 가 상대 경로 문자열일 수도 있음. 지금 로직 수정 필요. )
				.distinct().collect(Collectors.toList());
		log.debug("[extractLinksFromPage] links ====> {}", links);
		return links;
	}

	/**
	 * @param domainUrl seedUrl에서 추출한 원 도메인 url
	 * @param url       유효성 검사의 target url
	 */
	public boolean isValidLink(String domainUrl, String url) {
		if (url.isBlank()) { //StringUtils.isBlank(url)
			return false;
		}

//		    url = normalizeUrl(url);

		// domain url 비교 대신 이런 식으로 해야 할듯.
//		    String urlHost = UrlUtils.getDomainName(domainUrl);
//		    if (false == url.contains(urlHost)) {
//		    	return false;
//		    }

//		    if (!isValidUrlFormat(url) || !isValidProtocol(url) || containsSpamPatterns(url) || isMediaFile(url)) {
//		        return false;
//		    }

//		    if (isDuplicate(url)) {
//		        return false;
//		    }

//		    if (!isDepthValid(url, 5)) { // !isAllowedDomain(url) || 
//		        return false;
//		    }

//		    if (!isAllowedByRobotsTxt(url)) {
//		        return false;
//		    }

		return true;
		//
//		//공백 확인
//		if (StringUtils.isEmpty(url)) {
//			return false;
//		}
//		
//		// bloom filter
//		
//		// 중복 제거
//		
//		// 스팸 data 제거
//		
//		//  robots.txt 에 포함된 url 이 아닌지 확인
//		
//		return true;
	}

	private boolean isValidUrlFormat(String url) {
		try {
			new URL(url);
			return true;
		} catch (MalformedURLException e) {
			return false;
		}
	}

	private String normalizeUrl(String url) {
		try {
			URI uri = new URI(url);
			return uri.normalize().toString();
		} catch (URISyntaxException e) {
			return url; // Return original if normalization fails
		}
	}

	private final List<String> blockedPatterns = Arrays.asList("utm_", "facebook.com", "twitter.com", "ads", "login",
			"subscribe");

	private boolean containsSpamPatterns(String url) {
		return blockedPatterns.stream().anyMatch(url::contains);
	}

//	public boolean isAllowedByRobotsTxt(String url) {
//	    try {
//	        SimpleRobotRulesParser parser = new SimpleRobotRulesParser();
//	        SimpleRobotRules rules = parser.parse(new URL(url).openStream(), url);
//	        return rules.isAllowed(url);
//	    } catch (IOException e) {
//	        return false;  // If robots.txt cannot be read, assume restricted access
//	    }
//	}

	private final List<String> blockedExtensions = Arrays.asList(".jpg", ".png", ".gif", ".pdf", ".zip", ".mp4");

	private boolean isMediaFile(String url) {
		return blockedExtensions.stream().anyMatch(url::endsWith);
	}

	private boolean isDepthValid(String url, int maxDepth) {
		int depth = url.split("/").length;
		return depth <= maxDepth;
	}

	private boolean isValidProtocol(String url) {
		return url.startsWith("http://") || url.startsWith("https://");
	}

	private String getCanonicalUrl(String url) {
		return url.toLowerCase().replaceAll("www.", "").replaceAll("#.*", "");
	}

	/**
	 * URL 의 web page를 jsoup의 document 객체로 변환하여 반환
	 * 
	 * @param url
	 * @return document
	 */
	public Document convertPageIntoDoc(String url) {
		if (isDynamicPage(url)) {
			try (Playwright playwright = Playwright.create()) {
				// headless mode 로 웹 페이지 로드
				Browser browser = playwright.chromium().launch();
				Page page = browser.newPage();
				page.navigate(url);
				page.waitForLoadState(LoadState.NETWORKIDLE);
				String content = page.content();
				Document doc = Jsoup.parse(content);
				return doc;
			}
		} else {
			try {
				Document doc = Jsoup.connect(url).get();
				return doc;
			} catch (IOException e) {
				log.error("[convertPageIntoDoc] error ===> {}", e.getMessage());
			}
		}
		return null;
	}

	/**
	 * 정적 페이지의 meta tag 데이터 수집
	 * 
	 * @param url
	 */
	public Map<String, String> extractMetadataStatic(String url) throws IOException {
		log.debug("extractMetadataStatic start");
		Document doc = Jsoup.connect(url).get();
		Map<String, String> metadata = new HashMap<>();

		doc.select("meta").forEach(meta -> {
			String name = meta.attr("name");
			String content = meta.attr("content");
			if (!name.isEmpty()) {
				metadata.put(name, content);
			}
		});

		log.debug("[extractMetadataStatic] metadata ====> {}", metadata);
		return metadata;
	}

	/**
	 * Jsoup 의 document 객체의 meta tag 수집
	 * 
	 * @param url
	 * @return metadata
	 */
	public Map<String, String> extractMetadataStatic(Document jsoupDoc) {
		Map<String, String> metadata = new HashMap<>();
		jsoupDoc.select("meta").forEach(meta -> {
			String name = meta.attr("name");
			String content = meta.attr("content");
			if (!name.isEmpty()) {
				metadata.put(name, content);
			}
		});
		log.debug("[extractMetadataStatic] metadata ====> {}", metadata);
		return metadata;
	}

	/**
	 * 동적 페이지의 meta tag 데이터 수집
	 * 
	 * @param url
	 */
	public Map<String, String> extractMetadataDynamic(String url) throws IOException {
		log.debug("extractMetadataDynamic start");
		try (Playwright playwright = Playwright.create()) {

			// headless mode로 브라우저 탐색 - 기본 설정 default ()

			Browser browser = playwright.chromium().launch();
			Page page = browser.newPage();
			page.navigate(url);

			page.waitForLoadState(LoadState.NETWORKIDLE);

			String content = page.content();
//			log.debug("extractMetadataDynamic content ===> {}", content);

			// playwright
			Map<String, String> metadata = new HashMap<>();
			Document doc = Jsoup.parse(content);

			doc.select("meta").forEach(meta -> {
				String name = meta.attr("name");
				String contentValue = meta.attr("content");
				if (!name.isEmpty()) {
					metadata.put(name, contentValue);
				}
			});

			log.debug("[extractMetadataDynamic] metadata ====> {}", metadata);
			return metadata;
		}

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
