package com.pnunes.sabproxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadProxy extends Thread {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	private Socket sClient;
	private final String SERVER_URL;
	private final int SERVER_PORT;
	private AdServers adServers;

	ThreadProxy(Socket sClient, String ServerUrl, int ServerPort, AdServers adServers) {
		this.SERVER_URL = ServerUrl;
		this.SERVER_PORT = ServerPort;
		this.sClient = sClient;
		this.adServers = adServers;
		this.start();
	}

	@Override
	public void run() {

		try {
			//final byte[] request = new byte[1024];
			final byte[] request = new byte[2048];
			//byte[] reply = new byte[4096];
			byte[] reply = new byte[8192];
			final InputStream inFromClient = sClient.getInputStream();
			final OutputStream outToClient = sClient.getOutputStream();
			Socket client = null, server = null;

			// connects a socket to the proxy server
			try {
				server = new Socket(SERVER_URL, SERVER_PORT);
			} catch (IOException e) {
				PrintWriter out = new PrintWriter(new OutputStreamWriter(outToClient));
				out.flush();
				log.error("Failed to make connection to proxy server: "+SERVER_URL+":"+SERVER_PORT);
				log.error("Reason: "+e.getMessage());
				throw new RuntimeException(e);
			}


			// Stream client --> proxy
			final InputStream inFromServer = server.getInputStream();
			final OutputStream outToServer = server.getOutputStream();
			// a new thread for uploading to the server
			new Thread() {
				public void run() {
					int bytes_read;
					try {
						while ((bytes_read = inFromClient.read(request)) != -1) {
							String contents = new String(request);                        	
							String httpReqHost = getHostFromHTTPReq(contents);
							if(httpReqHost == null){
								httpReqHost = "";
							}
							if(adServers.contains(httpReqHost)){
								log.info("Blocking Ad from: "+httpReqHost);
								outToServer.flush();
								outToServer.close();
							}else{ // business as usual
								outToServer.write(request, 0, bytes_read);
								outToServer.flush();
							}

						}
					} catch (IOException e) {
					}
					try {
						outToServer.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}.start();



			// Stream proxy --> client
			int bytes_read;
			try {
				while ((bytes_read = inFromServer.read(reply)) != -1) {
					outToClient.write(reply, 0, bytes_read);
					outToClient.flush();
				}
			} catch (IOException e) {
				// We wont output error message here as this exception will be "Socket clsed" (if ad request is made the socket is closed)
			} finally {
				try {
					if (server != null)
						server.close();
					if (client != null)
						client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			outToClient.close();
			sClient.close();
		} catch (IOException e) {
			log.error("Error communicating with proxy: "+e.getMessage());
		}
	}


	private String getHostFromHTTPReq(String httpReq){
		if(!httpReq.startsWith("CONNECT ") && !httpReq.startsWith("GET ")){
			return "";
		}

		String[] lines = httpReq.split(System.getProperty("line.separator"));
		for(int i=0;i<lines.length;i++){
			if(lines[i].startsWith("Host: ")){
				String hostEntry = lines[i].replaceAll("Host: ", "").trim();
				hostEntry = hostEntry.replace(":80", "");
				hostEntry = hostEntry.replace(":443", "");
				return hostEntry;
			}
		}
		log.warn("Could not get host from request: "+lines[0]);
		return "";

	}
}