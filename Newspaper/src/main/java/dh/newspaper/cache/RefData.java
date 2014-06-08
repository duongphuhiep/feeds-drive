package dh.newspaper.cache;

import android.util.Log;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import dh.newspaper.model.generated.*;

import javax.inject.Inject;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Referential data, Always cache in memory
 * Created by hiep on 3/06/2014.
 */
public class RefData {
	private static final String TAG = RefData.class.getName();
	private final DaoSession mDaoSession;
	private List<PathToContent> mPathToContents;
	private TreeSet<String> mTags;

	private boolean pathToContentsStale = false;
	private boolean mTagsStale = false;

	@Inject
	public RefData(DaoSession daoSession) {
		mDaoSession = daoSession;
	}

	/**
	 * Return list of enabled PathToContent order by priority
	 */
	public synchronized List<PathToContent> pathToContentList() {
		if (mPathToContents==null || pathToContentsStale) {
			Stopwatch sw = Stopwatch.createStarted();
			mPathToContents = mDaoSession.getPathToContentDao().queryBuilder()
					.where(PathToContentDao.Properties.Enable.eq(Boolean.TRUE))
					.orderDesc(PathToContentDao.Properties.Priority)
					.list();
			Log.i(TAG, "Get PathToContent returned "+mPathToContents.size()+" records ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms)");
		}
		return mPathToContents;
	}

	/**
	 * Get all possible tags from active subscription (alphabetic order)
	 */
	public synchronized TreeSet<String> loadTags() {
		Stopwatch sw = Stopwatch.createStarted();
		List<Subscription> subscriptions = mDaoSession.getSubscriptionDao().queryBuilder()
				.where(SubscriptionDao.Properties.Enable.eq(Boolean.TRUE))
				.list();
		mTags = new TreeSet<String>();
		for (Subscription sub : subscriptions) {
			Iterable<String> subTags = Splitter.on('|').omitEmptyStrings().split(sub.getTags());
			for (String tag : subTags) {
				mTags.add(tag);
			}
		}
		Log.i(TAG, "Found " + mTags.size() + " tags from " + subscriptions.size() + " active subscription ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms)");

		return mTags;
	}

	public boolean isTagsAvailableInMemory() {
		return mTags!=null && !mTagsStale;
	}

	public TreeSet<String> getTags() {
		return mTags;
	}
}