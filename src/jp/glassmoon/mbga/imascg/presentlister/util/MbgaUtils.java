package jp.glassmoon.mbga.imascg.presentlister.util;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Random;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import android.net.Uri;
import android.util.Log;

public class MbgaUtils {

	public static String createUrl(final String rootUrl, final String indivisualHost, final String featurePath, final int index, List<NameValuePair> pairs) {
		Uri.Builder builder = new Uri.Builder();
		try {
			URI uri = new URI(rootUrl);
			String urlQuery = createQueryUrl(indivisualHost, featurePath, index, pairs);

			builder.scheme(uri.getScheme());
			builder.authority(uri.getAuthority());
			builder.path(uri.getPath() + "/");
			builder.appendQueryParameter("guid", "ON");
			builder.appendQueryParameter("url", urlQuery);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return builder.build().toString();	
	}
	
	public static String createQueryUrl(final String indivisualHost, final String featurePath, final int index, List<NameValuePair> pairs) {
		String path = featurePath;
		if (path.endsWith("/")) path += "/";
		if (index > 0) {
			path = path + Integer.toString(index);
		}
		
		Uri.Builder builder = new Uri.Builder();
		
		
		builder.scheme("http");
		builder.authority(indivisualHost);
		builder.path(path);
		for (NameValuePair pair : pairs) {
			builder.appendQueryParameter(pair.getName(), pair.getValue());
		}
		builder.appendQueryParameter("rnd", Integer.toString(new Random().nextInt(100000000)));

		try {
			String uri = builder.build().toString();
			Log.d("MbgaUtils", "QueryUrl: " + uri);
			URI uriQuery = new URI(uri);
			return uriQuery.toASCIIString();
		}
		catch (Exception e) {
			// TODO: handle exception
			Log.d("MbgaUtils", e.getMessage());
			return "";
		}
	}
	
	public static URL getQueryUrl(final String url) {
		URL queryUrl = null;
		try {
			List<NameValuePair> queries = URLEncodedUtils.parse(new URI(url), "UTF-8");
			for (NameValuePair pair : queries) {
				if ("url".equals(pair.getName())) {
					queryUrl = new URL(pair.getValue());
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return queryUrl;
	}
}
