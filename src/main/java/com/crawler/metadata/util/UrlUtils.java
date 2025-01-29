package com.crawler.metadata.util;

import java.net.URL;

public class UrlUtils {
	
	public static String getDomainName(String url) {
		try {
			URL uri = new URL(url);
			String host = uri.getHost();

			// Remove "www." prefix if it exists
			if (host.startsWith("www.")) {
				host = host.substring(4);
			}
			return host;
		} catch (Exception e) {
			return null;
		}
	}

}
