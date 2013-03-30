package vitro.wlab.wsi.coap.client.integration;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClient;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.CoapEmptyMessage;
import org.ws4d.coap.messages.CoapRequestCode;

import vitro.wlab.wsi.coap.Constants;
import vitro.wlab.wsi.coap.Functions;
import vitro.wlab.wsi.coap.client.integration.exception.VitroGatewayException;
import vitro.wlab.wsi.coap.client.integration.exception.WSIAdapterException;
import vitro.wlab.wsi.coap.client.integration.model.MoteResource;
import vitro.wlab.wsi.coap.client.integration.model.Network;
import vitro.wlab.wsi.coap.client.integration.model.Node;
import vitro.wlab.wsi.coap.client.integration.model.Observation;
import vitro.wlab.wsi.coap.client.integration.model.Resource;

public class WSIAdapterCoap implements WSIAdapter, CoapClient, Observer {
	
	/**
	 * @author Francesco Ficarola (ficarola<at>dis.uniroma1<dot>it)
	 */
	
	private Logger logger = Logger.getLogger(getClass());
	private CountDownLatch signal;
	
	private final int COAP_PORT = Constants.COAP_DEFAULT_PORT;
	
	private CoapChannelManager channelManager;
    private CoapClientChannel clientChannel;
    
    private List<Resource> resourceList;
    private String resourceValue;
    
    private DTNResponder dtnServer;
    private Thread threadDTN;
    private Random random;
    private boolean isDtnEnabled;
    private final String RESOURCE_REQ = "1";
    
    private String exceptionError;
    
    private List<Node> nodesList;
	
	
	public WSIAdapterCoap() {
		signal = null;
		channelManager = null;
		clientChannel = null;
		resourceList = new ArrayList<Resource>();
		resourceValue = null;
		exceptionError = "";
		random = new Random();
		dtnServer = new DTNResponder(Constants.DTN_VGW_PORT, this);
		threadDTN = new Thread(dtnServer);
		isDtnEnabled = false;
		channelManager = BasicCoapChannelManager.getInstance();
		
		nodesList = new LinkedList<Node>();
	}

	
	/**
	 * WSIAdapter interface
	 */
	
	@Override
	public List<Node> getAvailableNodeList() throws WSIAdapterException {
		
		logger.info("Getting available nodes...");
		
		nodesList = new ArrayList<Node>();
		
		List<String> wsnProxyList = new LinkedList<String>();
		wsnProxyList.add(Network.WLAB_OFFICE_PROXY_ADDRESS);
		wsnProxyList.add(Network.WLAB_LAB_PROXY_ADDRESS);
		
		DatagramSocket serverSocket = null;
		DatagramSocket clientSocket = null;
		String cmdString = "route";
		
		for(int i=0; i < wsnProxyList.size(); i++) {
			try {
				serverSocket = new DatagramSocket(Constants.UDPSHELL_VGW_PORT);
			
				String hostProxyString = wsnProxyList.get(i);
				InetAddress hostProxy = InetAddress.getByName(hostProxyString);
								
				clientSocket = new DatagramSocket();
				byte[] bufCmd = new byte[10];
				bufCmd = cmdString.getBytes();
				DatagramPacket outcomingPacket = new DatagramPacket(bufCmd, bufCmd.length, hostProxy, Constants.PROXY_UDPFORWARDER_PORT);
				clientSocket.send(outcomingPacket);
				
				boolean otherPackets = false;
				
				serverSocket.setSoTimeout(5000);
				logger.info("Quering " + hostProxyString);
				try {
					byte[] bufAck = new byte[10];
					DatagramPacket ackPacket = new DatagramPacket(bufAck, bufAck.length);
					serverSocket.receive(ackPacket);
					String ackString = new String(ackPacket.getData()).trim();
					if(ackString.equals("ack")) {
						otherPackets = true;
					}
				} catch (SocketTimeoutException e) {
					logger.warn(e.getMessage());
				}
				
				serverSocket.setSoTimeout(0);
				
		        while(otherPackets) {
		        	try {
		        		byte[] bufIncoming = new byte[1000];
		        		DatagramPacket incomingPacket = new DatagramPacket(bufIncoming, bufIncoming.length);
		        		serverSocket.receive(incomingPacket);
		        		String currentNodeIP = new String(incomingPacket.getData()).trim();
		        		if(!currentNodeIP.equals("end")) {
			        		logger.info("Node: " + currentNodeIP);
		        			nodesList.add(new Node(currentNodeIP));
		        		} else {
		        			otherPackets = false;
		        			logger.info("No other nodes from " + hostProxyString);
		        		}
		        	} catch (IOException e) {
		        		logger.error(e.getMessage());
					}
		        }
			
			
			} catch (UnknownHostException e) {
				logger.warn(e.getMessage() + " is not reachable.");
			} catch (SocketException e) {
				logger.error(e.getMessage());
			} catch (IOException e) {
				logger.error(e.getMessage());
			} finally {
				if(serverSocket != null) {
					serverSocket.close();
				}
				if(clientSocket != null) {
					clientSocket.close();
				}
			}
		}
		
		return nodesList;
	}
	

	@Override
	public List<Resource> getResources(Node node) throws WSIAdapterException {
		
		signal = new CountDownLatch(1);
		List<Resource> resourceList = new ArrayList<Resource>();

		try {
			
			if(isDtnEnabled) {
				dtnResourcesRequest(node);
				signal.await(5, TimeUnit.MINUTES);
			} else {
				coapResourcesRequest(node);
				signal.await();
			}
			

			if(this.resourceList.size() > 0) {
				
				resourceList.addAll(this.resourceList);
				this.resourceList.clear();
				
			} else {
				
				String error = "";
				if(!exceptionError.equals("")) {
					error = exceptionError;
					exceptionError = "";
				} else {
					error = "No available resources for Node " + node.getId();
				}
				
				throw new WSIAdapterException(error);
			}
			
		} catch (InterruptedException e) {
			throw new WSIAdapterException(e.getMessage(), e);
		} catch (UnknownHostException e) {
			throw new WSIAdapterException(e.getMessage(), e);
		} catch (IOException e) {
			throw new WSIAdapterException(e.getMessage(), e);
		}
		
		return resourceList;
	}

	@Override
	public Observation getNodeObservation(Node node, Resource resource) throws WSIAdapterException {
		
		signal = new CountDownLatch(1);
		Observation observation = new Observation();
		
		try {
			
			if(isDtnEnabled) {
				dtnObservationRequest(node, resource);
				signal.await(5, TimeUnit.MINUTES);
			} else {
				coapObservationRequest(node, resource);
				signal.await();
			}
			
			
			if(resourceValue != null) {
				
				observation.setNode(node);
				observation.setResource(resource);
				observation.setValue(formatResourceValue(resourceValue, resource));
				observation.setTimestamp(System.currentTimeMillis());
				resourceValue = null;
				
			} else {
				
				String error = "";
				if(!exceptionError.equals("")) {
					error = exceptionError;
					exceptionError = "";
				} else {
					error = "No available resources for Node " + node.getId() + " and Resource " + resource.getName();
				}
				
				throw new WSIAdapterException(error);
			}
			
		} catch (UnknownHostException e) {
			throw new WSIAdapterException(e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new WSIAdapterException(e.getMessage(), e);
		} catch (VitroGatewayException e) {
			throw new WSIAdapterException(e.getMessage(), e);
		} catch (IOException e) {
			throw new WSIAdapterException(e.getMessage(), e);
		}
		
		return observation;
	}
	


	/**
	 * DTN methods
	 */
	
	@Override
	public void update(Observable obs, Object obj) {
		String dtnMsg = (String)obj;
		try {
			onDtnResponse(dtnMsg);
		} catch (VitroGatewayException e) {
			logger.error(e.getMessage());
		}
	}
	
	private void onDtnResponse(String dtnMsg) throws VitroGatewayException {
		List<String> msgElements = new LinkedList<String>();
		StringTokenizer st = new StringTokenizer(dtnMsg, "#");
		while(st.hasMoreTokens()) {
			msgElements.add(st.nextToken().trim());
		}
		
		if(msgElements.size() != 4) {
			logger.error("Malformed DTN message: " + dtnMsg);
			return;
		}
		
		String packetID = msgElements.get(0);
		String serverID = msgElements.get(1);
		String packetType = msgElements.get(2);
		String packetBody = msgElements.get(3);
		
		logger.info("DTN Packet ID: " + packetID);
		logger.info("DTN Server ID: " + serverID);
		logger.info("DTN Packet Type: " + packetType);
		logger.info("DTN Packet Body: " + packetBody);
		
		List<String> bodyElements = new LinkedList<String>();
		st = new StringTokenizer(packetBody, "*");
		while(st.hasMoreTokens()) {
			bodyElements.add(st.nextToken().trim());
		}
		
		if(bodyElements.get(0).equals("wkc")) {
			for(int i = 1; i < bodyElements.size(); i++) {
				String resourceName = bodyElements.get(i);
				if(MoteResource.containsKey(resourceName)) {
					logger.debug("Resource name: " + resourceName);
					Resource resource = MoteResource.getResource(resourceName);
					resourceList.add(resource);
				}
			}
		} else
		if(bodyElements.get(0).equals("st")) {
			Integer readingValue = Integer.parseInt(bodyElements.get(1));
			int rawValue = 23355 + readingValue - 200;
			resourceValue = String.valueOf(rawValue);
		} else
		if(bodyElements.get(0).equals("sh")) {
			Integer readingValue = Integer.parseInt(bodyElements.get(1));
			int rawvalue = -204 + readingValue*4 - (readingValue*readingValue)*100/628931;
			resourceValue = String.valueOf(rawvalue);
		}
		
		signal.countDown();
	}
	
	public boolean getDtnPolicy() {
		return isDtnEnabled;
	}
	
	public void setDtnPolicy(boolean value) {
		isDtnEnabled = value;
		
		if(isDtnEnabled) {
			if(!threadDTN.isAlive()) {
				threadDTN.start();
			}
		}
	}
	
	private void dtnResourcesRequest(Node node) throws WSIAdapterException, IOException {
		String proxyAddress = getProxyAddress(node);
		if(proxyAddress != null) {
			int packetID = random.nextInt(65535) + 1;
			String msgString = packetID + "#" + node.getId() + "#" + RESOURCE_REQ + "#wkc";
			byte[] msgBytes = new byte[Constants.DTN_MESSAGE_SIZE];
			msgBytes = msgString.getBytes();
			DatagramPacket sendPacket = new DatagramPacket(msgBytes, msgBytes.length, InetAddress.getByName(proxyAddress), Constants.PROXY_UDPFORWARDER_PORT);
			DatagramSocket clientSocket = new DatagramSocket();
			clientSocket.send(sendPacket);
			clientSocket.close();
			logger.info("Sent Request: " + msgString);
		} else {
			logger.warn("No available proxy for Node " + node.getId() + " is found");
			throw new WSIAdapterException("No available proxy for Node " + node.getId() + " is found");
		}
	}
	
	private void dtnObservationRequest(Node node, Resource resource) throws VitroGatewayException, IOException {
		String proxyAddress = getProxyAddress(node);
		if(proxyAddress != null) {
			String moteUriResource = "";
			if(MoteResource.containsValue(resource)) {
				moteUriResource += MoteResource.getMoteUriResource(resource);
				int packetID = random.nextInt(65535) + 1;
				String msgString = packetID + "#" + node.getId() + "#" + RESOURCE_REQ + "#" + moteUriResource;
				byte[] msgBytes = new byte[Constants.DTN_MESSAGE_SIZE];
				msgBytes = msgString.getBytes();
				DatagramPacket sendPacket = new DatagramPacket(msgBytes, msgBytes.length, InetAddress.getByName(proxyAddress), Constants.PROXY_UDPFORWARDER_PORT);
				DatagramSocket clientSocket = new DatagramSocket();
				clientSocket.send(sendPacket);
				clientSocket.close();
				logger.info("Sent Request: " + msgString);
			} else {
				logger.warn("No resource mapping for Node " + node.getId() + " and Resource " + resource.getName());
				throw new WSIAdapterException("No resource mapping for Node " + node.getId() + " and Resource " + resource.getName());
			}
		} else {
			logger.warn("No available proxy for Node " + node.getId() + " is found");
			throw new WSIAdapterException("No available proxy for Node " + node.getId() + " is found");
		}
	}

	
	
	/**
	 * CoapClient interface
	 */
	
	@Override
	public void onConnectionFailed(CoapClientChannel channel, boolean notReachable, boolean resetByServer) {
		if(notReachable) {
			logger.warn("Connection failed: server is not reachable");
			exceptionError = "Connection failed: server is not reachable";
		} else {
			logger.warn("Connection failed");
			exceptionError = "Connection failed";
		}
		signal.countDown();
	}

	@Override
	public void onResponse(CoapClientChannel channel, CoapResponse response) {
		try {
			manageResponse(response);
		} catch (VitroGatewayException e) {
			logger.error(e.getMessage());
		}
		signal.countDown();
	}

	@Override
	public void onSeparateResponse(CoapClientChannel channel, CoapResponse message) {
		logger.info("Received Separate Response");
		//TODO: no implementation in TinyOS
		signal.countDown();
	}

	@Override
	public void onSeparateResponseAck(CoapClientChannel channel, CoapEmptyMessage message) {
		logger.info("Received Ack of Separate Response");
		//TODO: no implementation in TinyOS
		signal.countDown();
	}
	
	
	
	/**
	 * Private CoAP methods
	 */

	private void coapResourcesRequest(Node node) throws UnknownHostException, WSIAdapterException {
		String proxyAddress = getProxyAddress(node);
		if(proxyAddress != null) {
			clientChannel = channelManager.connect(this, InetAddress.getByName(proxyAddress), COAP_PORT);
//			clientChannel = channelManager.connect(this, InetAddress.getByName("localhost"), PORT);
			CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
			coapRequest.setProxyUri("coap://[" + node.getId() + "]:61616/" + MoteResource.RESOURCE_DISCOVERY);
//			coapRequest.setUriPath("/" + MoteResource.RESOURCE_DISCOVERY);
			clientChannel.sendMessage(coapRequest);
			logger.info("Sent Request: " + coapRequest.toString());
		} else {
			logger.warn("No available proxy for Node " + node.getId() + " is found");
			throw new WSIAdapterException("No available proxy for Node " + node.getId() + " is found");
		}
	}
	
	
	private void coapObservationRequest(Node node, Resource resource) throws UnknownHostException, VitroGatewayException {
		String proxyAddress = getProxyAddress(node);
		
		if(proxyAddress != null) {
			String moteUriResource = "";
			if(MoteResource.containsValue(resource)) {
				moteUriResource += MoteResource.getMoteUriResource(resource);
				clientChannel = channelManager.connect(this, InetAddress.getByName(proxyAddress), COAP_PORT);
//				clientChannel = channelManager.connect(this, InetAddress.getByName("localhost"), PORT);
				CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
				coapRequest.setProxyUri("coap://[" + node.getId() + "]:61616/" + moteUriResource);
//				coapRequest.setUriPath(moteUriResource);
				clientChannel.sendMessage(coapRequest);
				logger.info("Sent Request: " + coapRequest.toString());
			} else {
				logger.warn("No resource mapping for Node " + node.getId() + " and Resource " + resource.getName());
				throw new WSIAdapterException("No resource mapping for Node " + node.getId() + " and Resource " + resource.getName());
			}
//			
		} else {
			logger.warn("No available proxy for Node " + node.getId() + " is found");
			throw new WSIAdapterException("No available proxy for Node " + node.getId() + " is found");
		}
	}
	
	
	private String getProxyAddress(Node node) {
		String nodeIP = node.getId();
		
		if(nodeIP.contains(Network.WLAB_OFFICE_IPV6_PREFIX) || nodeIP.contains(Network.WLAB_OFFICE_IPV6_PREFIX_SHORT)) {
			return Network.WLAB_OFFICE_PROXY_ADDRESS;
		} else
		if(nodeIP.contains(Network.WLAB_LAB_IPV6_PREFIX) || nodeIP.contains(Network.WLAB_LAB_IPV6_PREFIX_SHORT)) {
			return Network.WLAB_LAB_PROXY_ADDRESS;
		}
		
		return null;
	}


	private void manageResponse(CoapResponse response) throws VitroGatewayException {
		logger.info("Received response code: " + response.getResponseCode());
		logger.info("CoAP Message ID: " + response.getMessageID());
		logger.info("Content Type: " + response.getContentType());
		logger.info("CoAP Server address: " + response.getChannel().getRemoteAddress());
		
		if(response.getPayload() == null) {
			return;
		}
		
		byte[] payloadBytes = response.getPayload();
		if(payloadBytes.length > 0) {
			switch(response.getContentType().getValue()) {
			case 0: {
				manageTextPlain(payloadBytes); //textplain
				break;
			}
			case 40: {
				manageLinkFormat(payloadBytes); //linkformat --> /.well-known/core
				break;
			}
			case 41: //TODO: xml (required???)
				break;
			case 42: {
				manageOctetStream(payloadBytes); //octetstream --> resource outcomes
				break;
			}
			case 47: //TODO: exi (required???)
				break;
			case 50: //TODO: json (required???)
				default: logger.warn("Unknown Content Type");
			}
		} else {
			logger.warn("The payload is empty.");
		}
	}

	
	private void manageLinkFormat(byte[] payloadBytes) throws VitroGatewayException {
		
		String payloadString = new String(payloadBytes);
		
		/** 
		 * Manage /.well-known/core resource 
		 */
		
		// Pattern example: </st>;ct=42,</sh>;ct=42
		Pattern p = Pattern.compile("(</\\w+>);?(ct=\\d+)?", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(payloadString);
		
		while(m.find()) {
			logger.debug("Regex Result: " + m.group());
			
			/** Resource Name */
			if(m.group(1) != null) {
				String resourceName = m.group(1).replaceAll("<|/|>", "");
				if(MoteResource.containsKey(resourceName)) {
					logger.debug("Resource name: " + resourceName);
					Resource resource = MoteResource.getResource(resourceName);
					resourceList.add(resource);
				}
			}
			
			/** Resource Content Type */
			if(m.group(2) != null) {
				// Do Nothing: not necessary
			}
		}
	}


	private void manageTextPlain(byte[] payloadBytes) {
		String payloadString = new String(payloadBytes);
		resourceValue = payloadString;
	}

	
	private void manageOctetStream(byte[] payloadBytes) {
		if(payloadBytes.length == 2) {
			short payloadShort = Functions.byteArraytoShort(payloadBytes);
			resourceValue = String.valueOf(payloadShort);
		} else {
			resourceValue = new String(payloadBytes);
		}
	}
	
	
	private String formatResourceValue(String resourceValue, Resource resource) {
		
		String result = resourceValue;
		
		int resourceValueLength = resourceValue.length();
		String integerPart = resourceValue.substring(0, resourceValueLength - 2);
		String decimalPart = resourceValue.substring(resourceValueLength - 2, resourceValueLength);
		
		if(resource.getName().equals(Resource.PHENOMENOM_TEMPERATURE) ||
				resource.getName().equals(Resource.PHENOMENOM_HUMIDITY)) {
			
			result = integerPart + "." + decimalPart;
			
		}
		
		return result;
	}
}
