package org.microgoat.duckdns;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;
import java.util.Properties;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Request;

public class DuckDnsUpdate {

	static Writer writer;
	static String interval;
	static String token;
	static String domains;
	static String currentIP;
	static long ms;

	public static void main(String[] args) {

		System.out.println("Start: " + new Date());
		currentIP = "";

		try {
			writer = new BufferedWriter( new OutputStreamWriter (
					new FileOutputStream("./log.txt", true), "utf-8") );
		} catch (UnsupportedEncodingException e1) {
			System.out.println("'log.txt' unsupported encoding!");
			e1.printStackTrace();
			return;
		} catch (FileNotFoundException e1) {
			System.out.println("'log.txt' not found or inaccesible!");
			e1.printStackTrace();
			return;
		}
		
		writeToLog("--------------- Start ---------------");
		
		try {
			writeToLog("Loading properties...");
			loadProperties();
			writeToLog("... properties loaded.");
		} catch (FileNotFoundException e) {
			writeToLog("'config.properties' not found!");
			e.printStackTrace();
			return;
		} catch (IOException e) {
			writeToLog("Error loading 'config.properties'!");
			e.printStackTrace();
			return;
		}

		if ( token.trim().length() < 1 ) {
			writeToLog("Token is not specified!");
			return;
		}

		if ( domains.trim().length() < 1 ) {
			writeToLog("Domains are not specified!");
			return;
		}

		String url = "http://www.duckdns.org/update?domains=" + domains + "&token=" + token + "&ip=";
		writeToLog("URL: " + url);
		
		ms = Integer.parseInt(interval) * 60L * 1000L;
		writeToLog("Interval: " + ms + "ms");
		if ( ms <= 30000L ) {
			writeToLog("Interval must be greater than 30s!");
			return;		
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook()));
		
		// infinite loop:
		while (true) {
			try {
				Thread.sleep(ms);
			} catch (InterruptedException e) {
				writeToLog("InterruptedException?");
				e.printStackTrace();
			}

			checkIP();

			// TODO: update only if address changed..
			updateDuckDNS(url);
		}

	}

	private static void checkIP() {
		try {
			// Get current IP address:
			String response = Request.Get("http://checkip.amazonaws.com/").
					connectTimeout(2000).socketTimeout(1000).
					execute().returnContent().asString();
			writeToLog("IP: " + response);
			
			if (!response.equals(currentIP) ) {
				if (currentIP.equals("")) {
					writeToLog("IP address set first time!");
				} else {
					writeToLog("IP address change!");
				}
				currentIP = response;
			}
			
		} catch (ClientProtocolException e) {
//			writeToLog("Update - ClientProtocolException!");
			e.printStackTrace();
		} catch (IOException e) {
//			writeToLog("Update - IOException!");
			e.printStackTrace();
		}
		
	}
	
	private static void updateDuckDNS(String url) {
		writeToLog("Updating...");
		try {
			String response = Request.Get(url).connectTimeout(2000).socketTimeout(1000)
			.execute().returnContent().asString();
			writeToLog("... response: " + response);
		} catch (ClientProtocolException e) {
			writeToLog("Update - ClientProtocolException!");
			e.printStackTrace();
		} catch (IOException e) {
			writeToLog("Update - IOException!");
			e.printStackTrace();
		}
	}

	private static void writeToLog(String message) {
		try {
			writer.write( new Date() + " " + message + System.lineSeparator() );
			writer.flush();
		} catch (IOException e) {
			System.out.println("Error while logging: " + message);
//			e.printStackTrace();
		}
	}
	
	private static void loadProperties() throws FileNotFoundException, IOException {
		FileInputStream stream = null;
		Properties props = new Properties();

		stream = new FileInputStream("./config.properties");
		props.load(stream);

		interval = props.getProperty("interval", "5");
		token = props.getProperty("token");
		domains = props.getProperty("domains");

		stream.close();
	}

	public static class ShutdownHook implements Runnable {

		@Override
		public void run() {
			writeToLog("--------------- Stop ---------------");
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Error closing 'log.txt'!");
			}
			
			System.out.println("Stop: " + new Date());
			System.out.close();
		}

	}
}
