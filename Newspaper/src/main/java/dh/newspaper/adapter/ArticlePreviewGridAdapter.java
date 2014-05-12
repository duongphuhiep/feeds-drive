package dh.newspaper.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.common.base.Strings;
import de.greenrobot.event.EventBus;
import dh.newspaper.R;
import dh.newspaper.base.InjectingActivityModule;
import dh.newspaper.event.BaseEvent;
import dh.newspaper.event.BaseEventOneArg;
import dh.newspaper.parser.ContentParser;
import dh.newspaper.parser.RssItem;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by hiep on 8/05/2014.
 */
public class ArticlePreviewGridAdapter extends ArrayAdapter<RssItem> {
	private static final String TAG = ArticlePreviewGridAdapter.class.getName();
	private ExecutorService mExecutor;

	private final LayoutInflater mInflater;
	private ContentParser mContentParser;
	private String mSourceAddress;

	private ArticlePreviewGridAdapter(Context context, int resource) {
		super(context, resource);
		mInflater = LayoutInflater.from(context);
	}

	public ArticlePreviewGridAdapter(Context context, ContentParser contentParser) {
		this(context, R.layout.article_preview);
		mContentParser = contentParser;
	}

	public void fetchAddress(String url) {
		if (!Strings.isNullOrEmpty(url) && mSourceAddress != url) {
			if (mExecutor != null) {
				mExecutor.shutdownNow();
			}
			mExecutor = Executors.newSingleThreadExecutor();
			mSourceAddress = url;
			mExecutor.execute(mGetDataFunc);
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		try {
			/* create (or get) view */

			View v;
			ImageView imageView;
			TextView titleLabel;
			TextView dateLabel;

			if (convertView == null) {
				// create new view
				v = mInflater.inflate(R.layout.article_preview, parent, false);
				imageView = (ImageView) v.findViewById(R.id.article_image);
				titleLabel = (TextView) v.findViewById(R.id.article_title);
				dateLabel = (TextView) v.findViewById(R.id.article_date);
				v.setTag(new View[]{imageView, titleLabel, dateLabel});
			} else {
				v = convertView;
				View[] viewsHolder = (View[]) v.getTag();
				imageView = (ImageView) viewsHolder[0];
				titleLabel = (TextView) viewsHolder[1];
				dateLabel = (TextView) viewsHolder[2];
			}

			/* bind value to view */

			RssItem item = this.getItem(position);
			if (item != null) {
				titleLabel.setText(item.getTitle());
				dateLabel.setText(item.getPublishedDate());
			}

			return v;
		} catch (Exception ex) {
			Log.w(TAG, ex);
			return null;
		}
	}

	/**
	 * Parse rss items from mSourceAddress
	 */
	private Runnable mGetDataFunc = new Runnable() {
		@Override
		public void run() {
			try {
				try {
					//connect to the URL and fetch rss items
					final List<RssItem> rssItems = mContentParser.parseRssUrl(mSourceAddress, "UTF-8");

					//notify GUI
					EventBus.getDefault().post(new Event() {{ data = rssItems; }});
				} catch (Exception ex) {
					Log.w(TAG, ex);
				}
			}
			catch (Exception ex) {
				Log.w(TAG, ex);
			}
		}
	};

	public class Event extends BaseEvent<ArticlePreviewGridAdapter> {
		public List<RssItem> data;
		public Event() {
			super(ArticlePreviewGridAdapter.this);
		}
		public Event(String subject) {
			super(ArticlePreviewGridAdapter.this, subject);
		}
	}
}
