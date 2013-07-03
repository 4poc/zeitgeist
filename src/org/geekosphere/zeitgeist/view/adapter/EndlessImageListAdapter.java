package org.geekosphere.zeitgeist.view.adapter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.geekosphere.zeitgeist.R;
import org.geekosphere.zeitgeist.activity.ZGActivity;
import org.geekosphere.zeitgeist.activity.ZGPreferenceActivity;
import org.geekosphere.zeitgeist.data.ZGItem;
import org.geekosphere.zeitgeist.net.WebRequestBuilder;
import org.geekosphere.zeitgeist.processor.ZGItemProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import at.diamonddogs.data.dataobjects.CacheInformation;
import at.diamonddogs.data.dataobjects.WebRequest;
import at.diamonddogs.service.net.HttpService.WebRequestReturnContainer;
import at.diamonddogs.service.net.HttpServiceAssister;
import at.diamonddogs.service.processor.ImageProcessor;

import com.commonsware.cwac.endless.EndlessAdapter;

public class EndlessImageListAdapter extends EndlessAdapter {
	private static final Logger LOGGER = LoggerFactory.getLogger(EndlessImageListAdapter.class);

	private HttpServiceAssister assister;
	private Pair<ZGItem, Bitmap> cachedItem;
	private boolean thumbnail = true;

	private final AtomicInteger currentPage = new AtomicInteger(1);
	private final AtomicInteger listPointer = new AtomicInteger(0);
	private List<ZGItem> zgItems;

	public EndlessImageListAdapter(Context context) {
		super(context, new ZGAdapter(context), -1);
		assister = new HttpServiceAssister(context);
		assister.bindService();
	}

	public EndlessImageListAdapter(Context context, boolean thumbnail) {
		super(context, new ZGAdapter(context), -1);
		this.thumbnail = thumbnail;
		assister = new HttpServiceAssister(context);
		assister.bindService();
	}

	@Override
	protected View getPendingView(ViewGroup parent) {
		// return dummy view
		ImageView v = new ImageView(getContext());
		v.setImageResource(R.drawable.app_icon);
		return v;
	}

	@Override
	protected boolean cacheInBackground() throws Exception {
		LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(ZGActivity.INTENT_ACTION_LOADING));
		cachedItem = getNextItem();
		return cachedItem != null;
	}

	private Pair<ZGItem, Bitmap> getNextItem() {

		if (zgItems == null || listPointer.get() == (zgItems.size() - 1)) {
			WebRequestBuilder wrb = new WebRequestBuilder(getContext());
			listPointer.set(0);
			WebRequest wr;
			LOGGER.info("getNextItem: Getting items for page " + currentPage);
			wr = wrb.getItems().page(currentPage.getAndIncrement()).build();
			ZGItem[] items = (ZGItem[]) ((WebRequestReturnContainer) assister.runSynchronousWebRequest(wr, new ZGItemProcessor()))
					.getPayload();
			zgItems = Collections.synchronizedList(Arrays.asList(items));
			LOGGER.info("getNextItem: got " + zgItems.size());
		}

		ZGItem item = zgItems.get(listPointer.getAndIncrement());
		LOGGER.info("getNextItem: using imageurl of " + item.getId());
		String imageUrl;
		if (thumbnail) {
			imageUrl = item.getRelativeThumbnailPath();
		} else {
			imageUrl = item.getRelativeImagePath();
		}
		if (imageUrl != null) {
			SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());
			String url = p.getString(ZGPreferenceActivity.KEY_HOST, "") + imageUrl;

			LOGGER.info("Getting Image: " + url + " thumb? " + thumbnail);
			// TODO: put in WebRequestBuilder
			WebRequest imageWr = new WebRequest();
			imageWr.setUrl(url);
			imageWr.setProcessorId(ImageProcessor.ID);
			imageWr.setCacheTime(CacheInformation.CACHE_FOREVER);
			Bitmap b = (Bitmap) ((WebRequestReturnContainer) assister.runSynchronousWebRequest(imageWr, new ImageProcessor())).getPayload();
			Pair<ZGItem, Bitmap> returnPair = new Pair<ZGItem, Bitmap>(item, b);

			return returnPair;
		} else {
			return getNextItem();
		}

	}

	@Override
	protected void appendCachedData() {
		((ZGAdapter) getWrappedAdapter()).add(cachedItem);
		LocalBroadcastManager.getInstance(getContext()).sendBroadcast(new Intent(ZGActivity.INTENT_ACTION_LOADING_DONE));

	}

	public Pair<ZGItem, Bitmap> getZGItem(int position) {
		return (Pair<ZGItem, Bitmap>) getWrappedAdapter().getItem(position);
	}

	private static final class ZGAdapter extends ArrayAdapter<Pair<ZGItem, Bitmap>> {

		public ZGAdapter(Context context) {
			super(context, -1);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new ImageView(getContext());
				((ImageView) convertView).setScaleType(ScaleType.CENTER_INSIDE);
			}
			Pair<ZGItem, Bitmap> item = getItem(position);
			ImageView imageView = (ImageView) convertView;
			imageView.setImageBitmap(item.second);
			return convertView;
		}
	}

}
