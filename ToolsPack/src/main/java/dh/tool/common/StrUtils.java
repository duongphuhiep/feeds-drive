package dh.tool.common;

import com.google.common.base.Strings;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 * Created by hiep on 14/05/2014.
 */
public class StrUtils {
	private static final String TAG = StrUtils.class.getName();
	private final static int GLIMPSE_SIZE = 60;
	private final static String NON_THIN = "[^iIl1\\.,']";
	private final static char[] HEX_CHARS = "0123456789abcdef".toCharArray();

	private static int textWidth(String str) {
		return (int) (str.length() - str.replaceAll(NON_THIN, "").length() / 2);
	}

	private static String cut(String text, int max, String trail, boolean includeSize) {
		if (text == null) {
			return "null";
		}

		String suffix = trail;
		if (includeSize) {
			suffix = suffix + " (length="+text.length()+")";
		}
		int suffixLength = suffix.length();

		if (textWidth(text) <= max)
			return text;

		// Start by chopping off at the word before max
		// This is an over-approximation due to thin-characters...
		int end = text.lastIndexOf(' ', max-suffixLength);

		// Just one long word. Chop it off.
		if (end == -1)
			return text.substring(0, max-suffixLength) + suffix;

		// Step forward as long as textWidth allows.
		int newEnd = end;
		do {
			end = newEnd;
			newEnd = text.indexOf(' ', end + 1);

			// No more spaces.
			if (newEnd == -1)
				newEnd = text.length();

		} while (textWidth(text.substring(0, newEnd) + suffix) < max);

		return text.substring(0, end) + suffix;
	}

	/**
	 * Use to log a long string, give a quick glimpse at the first 60 characters and replace return line by space
	 */
	public static String glimpse(String text) {
		if (text == null) {
			return "null";
		}
		if (text.length()<GLIMPSE_SIZE) {
			return text;
		}
		return text.substring(0, GLIMPSE_SIZE).replace('\n', ' ').replace('\r', ' ')+".. (length="+text.length()+")";
	}

	public static String ellipsize(String text, int max) {
		return cut(text, max, "...", false);
	}

	public static String cut(String text, int max) {
		return cut(text, max, "", false);
	}

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX_CHARS[v >>> 4];
			hexChars[j * 2 + 1] = HEX_CHARS[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static String getChecksum(MessageDigest md, String s) throws UnsupportedEncodingException {
		if (Strings.isNullOrEmpty(s)) {
			return null;
		}
		return StrUtils.bytesToHex(md.digest(s.toUpperCase().replaceAll("\\s", "").getBytes("UTF-8")));
	}

	/**
	 * Remove double white space, and convert to upper case
	 */
	public static String normalizeUpper(String s) {
		return s.trim().replaceAll("\\s+", " ").toUpperCase();
	}

	/**
	 * Same as {@link String#equalsIgnoreCase(String)}
	 * prevent NullPointerException, return true if both a and b is Null or Empty
	 */
	public static boolean equalsIgnoreCases(String a, String b) {
		if (Strings.isNullOrEmpty(a)) {
			return Strings.isNullOrEmpty(b);
		}
		return a.equalsIgnoreCase(b);
	}

	/**
	 * Same as {@link String#equals(Object)}
	 * prevent NullPointerException, return true if both a and b is Null or Empty
	 */
	public static boolean equalsString(String a, String b) {
		if (Strings.isNullOrEmpty(a)) {
			return Strings.isNullOrEmpty(b);
		}
		return a.equals(b);
	}

	/**
	 * return true if str is shorter than percent of ref
	 */
	public static boolean tooShort(String str, String ref, int percent) {
		if (percent < 0) {
			return false;
		}
		int lenStr = Strings.isNullOrEmpty(str) ? 0 : str.length();
		int lenRef =  Strings.isNullOrEmpty(ref) ? 0 : ref.length();
		int shortestLenAllowed = lenRef*percent/100;
		return lenStr < shortestLenAllowed;
	}
}
