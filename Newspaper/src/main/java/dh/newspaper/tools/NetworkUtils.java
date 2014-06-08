package dh.newspaper.tools;

import android.net.http.AndroidHttpClient;
import android.util.Log;
import com.google.common.base.Stopwatch;
import com.squareup.okhttp.OkHttpClient;
import dh.newspaper.Constants;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Created by hiep on 7/05/2014.
 */
public class NetworkUtils {
	private static final String TAG = NetworkUtils.class.getName();
	public static final String MOBILE_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.0.3; ko-kr; LG-L160L Build/IML74K) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30";
	public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 Safari/537.36";

	private static final OkHttpClient okHttpClient = new OkHttpClient();

	/**
	 * Get network stream (mobile user-agent) use HttpGet
	 * @param address
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public static InputStream getStreamFromUrl3(String address, String userAgent) throws IOException {
		Stopwatch sw;
		if (Constants.DEBUG) {
			sw = Stopwatch.createStarted();
			Log.v(TAG, "Downloading " + address);
		}

		AndroidHttpClient client = AndroidHttpClient.newInstance(userAgent);
		HttpGet request = new HttpGet(address);
		byte[] rawData = null;
		try {
			HttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			rawData = EntityUtils.toByteArray(entity);
			return new ByteArrayInputStream(rawData);
		} finally {
			client.close();

			if (Constants.DEBUG) {
				sw.stop();
				int size = rawData == null ? 0 : rawData.length;
				Log.v(TAG, "Downloaded OK "+size+" bytes ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms) "+address);
			}
		}
	}

//	/**
//	 * Get network stream (mobile user-agent) use HttpURLConnection
//	 * @param address
//	 * @return
//	 * @throws IOException
//	 */
//	static InputStream getStreamFromUrl2(String address, String userAgent) throws IOException {
//		Log.d(TAG, "Create HttpURLConnection to " + address);
//
//		URL url = new URL(address);
//		HttpURLConnection httpConnection_ = (HttpURLConnection)url.openConnection();
//		httpConnection_.addRequestProperty("User-Agent", userAgent);
//		int responseCode = httpConnection_.getResponseCode();
//		if (responseCode == HttpURLConnection.HTTP_OK)
//			return httpConnection_.getInputStream();
//		else
//			throw new IllegalStateException("Failed to connect to "+address+": "+ httpConnection_.getResponseMessage()+" ("+ responseCode+")");
//	}

	/**
	 * Get network stream (mobile user-agent) use HttpURLConnection
	 * @param address
	 * @return
	 * @throws IOException
	 */
	public static InputStream getStreamFromUrl(String address, String userAgent) throws IOException {
		Stopwatch sw;
		if (Constants.DEBUG) {
			sw = Stopwatch.createStarted();
		}

		ByteArrayOutputStream baos = null;
		InputStream input = null;
		HttpURLConnection httpConnection = null;
		try {
			httpConnection = okHttpClient.open(new URL(address));
			httpConnection.addRequestProperty("User-Agent", userAgent);

			int responseCode = httpConnection.getResponseCode();
			input = httpConnection.getInputStream();

			//download all the page to other InputStream
			baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len;
			while ((len = input.read(buffer)) > -1) {
				baos.write(buffer, 0, len);
			}
			baos.flush();

			if (responseCode == HttpURLConnection.HTTP_OK) {
				return new ByteArrayInputStream(baos.toByteArray());
			} else {
				throw new IllegalStateException("Failed to connect to " + address + ": " + httpConnection.getResponseMessage() + " (" + responseCode + ")");
			}
		}
		finally {
			if (input != null) {
				input.close();
			}
			if (input != null) {
				baos.close();
			}
			if (Constants.DEBUG) {
				sw.stop();
				int size = baos == null ? 0 : baos.size();
				Log.v(TAG, "Downloaded end "+size+" bytes ("+sw.elapsed(TimeUnit.MILLISECONDS)+" ms) "+address);
			}
		}
	}


}






//	/**
//	 * Get network stream (mobile user-agent) use HttpURLConnection
//	 * @param address
//	 * @return
//	 * @throws IOException
//	 */
//	static InputStream getStreamFromUrl2(String address) throws IOException {
//		Log.d(TAG, "Create HttpURLConnection to " + address);
//
//		URL url = new URL(address);
//		HttpURLConnection httpConnection_ = (HttpURLConnection)url.openConnection();
//		httpConnection_.addRequestProperty("User-Agent", MOBILE_USER_AGENT);
//		int responseCode = httpConnection_.getResponseCode();
//		if (responseCode == HttpURLConnection.HTTP_OK)
//			return httpConnection_.getInputStream();
//		else
//			throw new IllegalStateException("Failed to connect to "+address+": "+ httpConnection_.getResponseMessage()+" ("+ responseCode+")");
//	}

//	/**
//	 * Get network stream (mobile user-agent) use HttpGet
//	 * @param address
//	 * @return
//	 * @throws IllegalStateException
//	 * @throws IOException
//	 */
//	public static InputStream getStreamFromUrl(String address) throws IllegalStateException, IOException {
//		Log.d(TAG, "Create HttpClient to "+address);
//
//		HttpGet httpGet_ = new HttpGet(address);
//		HttpClient httpclient = AndroidHttpClient.newInstance(MOBILE_USER_AGENT);
//
//		// Execute HTTP Get Request
//		HttpResponse response = httpclient.execute(httpGet_);
//
//		//ByteArrayInputStream
//
//		return response.getEntity().getContent();
//
//	}