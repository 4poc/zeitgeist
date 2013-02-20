package org.geekosphere.zeitgeist.net;

import java.net.URL;

import org.geekosphere.zeitgeist.activity.ZGPreferenceActivity;
import org.geekosphere.zeitgeist.processor.ZGItemProcessor;
import org.geekosphere.zeitgeist.processor.ZGSingleItemProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import at.diamonddogs.data.dataobjects.WebRequest;
import at.diamonddogs.data.dataobjects.WebRequest.Type;

public class WebRequestBuilder {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebRequestBuilder.class);

	private WebRequest wr;
	private String URL;

	public WebRequestBuilder(Context c) {
		URL = PreferenceManager.getDefaultSharedPreferences(c).getString(ZGPreferenceActivity.KEY_HOST, "http://zeitgeist.li");
	}

	public WebRequestBuilder getItems() {
		wr = createDefaultWebRequest();
		wr.setUrl(URL);
		wr.setRequestType(Type.GET);
		wr.setProcessorId(ZGItemProcessor.ID);
		return this;
	}

	public WebRequestBuilder before(int before) {
		String url = addParameterToUrl(wr.getUrl(), "before", String.valueOf(before));
		wr.setUrl(url);
		return this;
	}

	public WebRequestBuilder after(int after) {
		String url = addParameterToUrl(wr.getUrl(), "after", String.valueOf(after));
		wr.setUrl(url);
		return this;
	}

	public WebRequestBuilder page(int page) {
		String url = addParameterToUrl(wr.getUrl(), "page", String.valueOf(page));
		wr.setUrl(url);
		return this;
	}

	public WebRequestBuilder withId(int id) {
		String url = appendPath(wr.getUrl(), String.valueOf(id));
		wr.setUrl(url);
		wr.setProcessorId(ZGSingleItemProcessor.ID);
		return this;
	}

	public WebRequest build() {
		return wr;
	}

	public void reset() {
		wr = null;
	}

	private WebRequest createDefaultWebRequest() {
		WebRequest wr = new WebRequest();
		wr.addHeaderField("Accept", "application/json");
		return wr;
	}

	private String appendPath(URL url, String path) {
		Uri u = Uri.parse(url.toString());
		u = u.buildUpon().appendEncodedPath(path).build();
		try {
			return u.toString();
		} catch (Throwable tr) {
			LOGGER.error("Unable to create URL", tr);
			return null;
		}
	}

	private String addParameterToUrl(URL url, String key, String value) {
		Uri u = Uri.parse(url.toString());
		u = u.buildUpon().appendQueryParameter(key, value).build();
		try {
			return u.toString();
		} catch (Throwable tr) {
			LOGGER.error("Unable to create URL", tr);
			return null;
		}
	}
}
