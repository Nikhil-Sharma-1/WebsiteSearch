package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebsiteSearcher {
	
	private static final Logger LOGGER = Logger.getLogger(WebsiteSearcher.class.getName());

	public static void main(String args[]) throws IOException, InterruptedException {

		URL urlSource = new URL("https://s3.amazonaws.com/fieldlens-public/urls.txt");
		final String inputFile = "input.txt";
		final String outputFile = "results.txt";
		String searchTerm = "hello";
		
		//Fetch the data from S3 and write it locally.
		fetchUrlsFromS3(urlSource, inputFile);

		// this is being used to capture whether the website contains the search term or not, across different threads.
		ConcurrentHashMap<String, Boolean> resultMap = new ConcurrentHashMap<>();		
		
		// blocking queue is being used to limit the number of threads ( http requests ) to 20
		BlockingQueue<String> queue = new ArrayBlockingQueue<>(20);
		
		List<String> urls = 
				Files.lines(Paths.get(inputFile))
				.skip(1)
				.map(line -> line.split(","))
				.map(arr -> arr[1])
				.map(line -> line.replace("\"", ""))
				.map(String::trim)
				.collect(Collectors.toList());

		// this is being used to make the main thread wait until all other threads complete.
		CountDownLatch latch = new CountDownLatch(urls.size());

		urls.forEach(url -> new Thread(new Website(url, searchTerm, resultMap, queue, latch)).start());
		latch.await();
		
		// Writes all the results from the map to the output file results.txt
		compileSearchResults(resultMap, outputFile, searchTerm);		
	}
	
	/***
	 * Fetch the file from S3 and store it locally
	 * @param urlSource location
	 * @param inputFile
	 * @throws IOException
	 */
	static void fetchUrlsFromS3(URL urlSource, String inputFile) throws IOException {
		URLConnection conn = urlSource.openConnection();
		InputStream is = conn.getInputStream();

		if (is != null) {
			Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inputFile), "utf-8"));
			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				writer.close();
				is.close();
			}

			LOGGER.log(Level.INFO, "Connection Established");

		} else {
			LOGGER.log(Level.INFO, "Connection could not be established");
		}

	}
	
	/**
	 * Write the results from map to the output file
	 * @param resultMap 
	 * @param outputFile
	 * @param searchTerm 
	 * @throws IOException
	 */
	static void compileSearchResults(ConcurrentHashMap<String, Boolean> resultMap, String outputFile, String searchTerm)
			throws IOException {
		Writer writer = new BufferedWriter(new FileWriter(outputFile, false));
		writer.write("Url" + "," + "SearchTerm" + "," + "IsPresent" + "\n");
		try {
			 for (Object key : resultMap.keySet()) {
				writer.write(key.toString()  + "," + searchTerm + "," + resultMap.get(key) + "\n");
			}
			
		} finally {
			writer.flush();
			writer.close();
		}
	}
}
