package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Website implements Runnable {
	private static final Logger LOGGER = Logger.getLogger(Website.class.getName());

	final String url;
	final String searchTerm;
	final ConcurrentHashMap<String, Boolean> map;
	final BlockingQueue<String> queue;
	final CountDownLatch latch;

	Website(final String url, final String searchTerm, final ConcurrentHashMap<String, Boolean> map,
			final BlockingQueue<String> queue, final CountDownLatch latch) {
		this.url = url;
		this.searchTerm = searchTerm;
		this.map = map;
		this.queue = queue;
		this.latch = latch;
	}

	@Override
	public void run() {

		try {
			queue.put(this.url);

			// Get website content
			URL uri = new URL("https://" + this.url);
			LOGGER.info("Fetching: " + uri);
			HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(10000);
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder a = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				a.append(inputLine).append("\n");
			}
			in.close();

			// Determine if the website contains searchTerm
			Pattern pattern = Pattern.compile(searchTerm, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(a.toString());
			map.put(this.url, matcher.find());

		} catch (InterruptedException | IOException e) {
			LOGGER.log(Level.SEVERE, this.url + " " + e.getMessage());
		} finally {
			queue.remove(this.url);
			latch.countDown();
		}
	}
}
