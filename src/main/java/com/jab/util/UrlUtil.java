package com.jab.util;

import com.jab.config.JabConfig;

/**
 * URL sanitization and validation for screen displays.
 */
public final class UrlUtil {
	private UrlUtil() {}

	/**
	 * Sanitizes a user-provided URL string. Adds https:// if no scheme is present.
	 * Returns "about:blank" for null or empty input.
	 */
	public static String sanitize(String url) {
		if (url == null || url.isBlank()) return "about:blank";
		url = url.trim();
		if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("about:")) {
			url = "https://" + url;
		}
		return url;
	}

	/**
	 * Returns true if the URL is within the allowed length.
	 */
	public static boolean isValidLength(String url) {
		return url != null && url.length() <= 2048;
	}
}
