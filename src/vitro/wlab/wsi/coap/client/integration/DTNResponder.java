package vitro.wlab.wsi.coap.client.integration;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.Logger;

import vitro.wlab.wsi.coap.Constants;

public class DTNResponder extends Observable implements Runnable {
	private Logger logger = Logger.getLogger(getClass());
	
	private int port;
	private DatagramSocket serverSocket;
	
	private Observer adapterListener;
	
	public DTNResponder(int port, Observer adapterListener) {
		this.port = port;
		this.adapterListener = adapterListener;
	}
	
	@Override
	public void run() {
		
		try {
			
			this.addObserver(adapterListener);
			
			serverSocket = new DatagramSocket(port);
			while(true) {
				byte[] receiveData = new byte[Constants.DTN_MESSAGE_SIZE];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				String msgString = new String(receivePacket.getData()).trim();
				logger.info("DTN response: " + msgString);
				
				/** Notify received messages to the Observer */
				setChanged();
				notifyObservers(msgString);
			}
			
		} catch (IOException e) {
			
			logger.error(e.getMessage());
			
		} finally {
			
			if(serverSocket != null) {
				serverSocket.close();
			}
			
			this.deleteObservers();
		}
	}

}
