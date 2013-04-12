package vitro.wlab.wsi.proxy;

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
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import vitro.wlab.wsi.coap.Constants;
import vitro.wlab.wsi.coap.client.integration.model.Network;

public class ProxyUdpForwarder implements Runnable {
	
	private Logger logger = Logger.getLogger(getClass());
	
	private DatagramSocket serverSocket;
	private DatagramSocket clientSocket;
	
	private String ipv6ToNodeID(String ipv6) {
		String lastPart = ipv6;
		
		if(lastPart.contains("::")) {
			StringTokenizer st = new StringTokenizer(ipv6, "::");
			while(st.hasMoreTokens()) {
				lastPart = st.nextToken().trim();
			}
		}
				
		if(lastPart.contains(":")) {
			StringTokenizer st = new StringTokenizer(":");
			while(st.hasMoreTokens()) {
				lastPart = st.nextToken().trim();
			}
		}
		
		Integer id = Integer.parseInt(lastPart, 16);
		return String.valueOf(id);
	}
	
	private String nodeIdToIPv6(String nodeID, String networkPrefix) {
		String destIP = "";
		
		if(networkPrefix.contains(Network.WLAB_LAB_IPV6_PREFIX)) {
			destIP = Network.WLAB_LAB_IPV6_PREFIX + "::" + nodeID;
		} else
		if(networkPrefix.contains(Network.WLAB_OFFICE_IPV6_PREFIX)) {
			destIP = Network.WLAB_OFFICE_IPV6_PREFIX + "::" + nodeID;
		}
		
		return destIP;
	}

	@Override
	public void run() {
		try {
			serverSocket = new DatagramSocket(Constants.PROXY_UDPFORWARDER_PORT);
		} catch (SocketException e) {
			logger.error(e.getMessage(), e);
			if(serverSocket != null) {
				serverSocket.close();
			}
		}
		
		while(true) {
			try {
				byte[] receiveData = new byte[Constants.DTN_MESSAGE_SIZE];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				serverSocket.receive(receivePacket);
				InetAddress fromIP = receivePacket.getAddress();
				String fromIPString = fromIP.getHostAddress();
				String msgString = new String(receivePacket.getData()).trim();
				
				if(msgString.equals("route")) {
					/** Route command */
					logger.info("Command: " + msgString + " - from: " + fromIPString);
					
					/** ACK */
					DatagramSocket ackSocket = new DatagramSocket();
					String ack = "ack";
					byte[] sendData = new byte[10];
					sendData = ack.getBytes();
					
					InetAddress IPAddress = InetAddress.getByName(Network.VGW_ADDRESS);
					
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Constants.UDPSHELL_VGW_PORT);
					ackSocket.send(sendPacket);
					ackSocket.close();
					
					/** Managing route command */
					manageRoutingTable();
					
				} else {
					/** DTN Messages */
					logger.info("DTN message: " + msgString + " - from: " + fromIPString);
					manageDtnMessages(msgString, fromIPString, fromIP);
				}
			} catch (IOException e) {
				logger.error(e.getMessage());
				if(clientSocket != null) {
					clientSocket.close();
				}
			}
		}
	}
	
	private void manageRoutingTable() throws IOException {
		List<String> nodeIPs = new ArrayList<String>();
		
		String ipGatewayNode1String = "fec0:0:0:1::0";
		String ipGatewayNode2String = "fec0:0:0:2::0";
		
		List<String> ipGatewayList = new LinkedList<String>();
		ipGatewayList.add(ipGatewayNode1String);
		ipGatewayList.add(ipGatewayNode2String);
		
		DatagramSocket socket = null;
		
		for(int i=0; i<ipGatewayList.size(); i++) {
			
			String ipGatewayNodeString = ipGatewayList.get(i);
			
			try {
				InetAddress ipGatewayNode = InetAddress.getByName(ipGatewayNodeString);
				
				socket = new DatagramSocket();
				byte[] buf = new byte[1000];
		        DatagramPacket incomingPacket = new DatagramPacket(buf, buf.length);
		        
		        String cmdString = "route\n";
		        buf = cmdString.getBytes();
		        
		        DatagramPacket outcomingPacket = new DatagramPacket(buf, buf.length, ipGatewayNode, Constants.UDPSHELL_GATEWAY_PORT);
		        socket.send(outcomingPacket);
		        
		        socket.setSoTimeout(1000);
		        
		        boolean otherPackets = true;
		        while(otherPackets) {
		        	try {
			        	socket.receive(incomingPacket);
			        	String rcvd = new String(incomingPacket.getData()).trim();
			        	logger.debug(rcvd);
			        	
			        	List<String> lineRoutingTable = new LinkedList<String>();
			        	StringTokenizer st = new StringTokenizer(rcvd, "\t");
			        	while(st.hasMoreTokens()) {
			        		lineRoutingTable.add(st.nextToken().trim());
			        	}
			        	
			        	for(int j=0; j < lineRoutingTable.size(); j++) {
			        		if(lineRoutingTable.get(j).contains("fec0")) {
			        			String networkString = lineRoutingTable.get(j);
			        			logger.debug("Network IP: " + networkString);
			        			StringTokenizer stIP = new StringTokenizer(networkString, "/");
			        			String nodeIPString = stIP.nextToken().trim();
			        			logger.debug("Node IP: " + nodeIPString);
			        			
			        			Process ping6;
			        			int returnVal = 2; // initialized to not reachable
								try {
									logger.debug("ping6 -c 3 " + nodeIPString);
									ping6 = java.lang.Runtime.getRuntime().exec("ping6 -c 3 " + nodeIPString);
									returnVal = ping6.waitFor();
								} catch (InterruptedException e) {
									logger.error(e.getMessage());
								} catch (IOException e) {
									logger.error(e.getMessage());
								}
								
			        			boolean reachable = (returnVal==0);
			        			if(reachable) {
			        				logger.info(nodeIPString + " added to the available nodes list.");
			        				nodeIPs.add(nodeIPString);
			        			} else {
			        				logger.warn(nodeIPString + " not reachable.");
			        			}
			        		}
			        	}
			        	
		        	} catch (SocketTimeoutException e) {
		                logger.info("Timeout reached: no other nodes from the " + ipGatewayNodeString + " routing table are available.");
		                nodeIPs.add("end");
		                otherPackets = false;
		                socket.close();
		            } catch (IOException e) {
						logger.error(e.getMessage());
					}
		        }
		        
			} catch (SocketException e) {
				logger.error("[" + ipGatewayNodeString + "] " + e.getMessage());
			} catch (IOException e) {
				logger.error("[" + ipGatewayNodeString + "] " + e.getMessage());
			} finally {
				if(socket != null ) {
					socket.close();
				}
			}
		}
		/** Sending back node IPs */
		clientSocket = new DatagramSocket();
		
		for(int i=0; i<nodeIPs.size(); i++) {
			String ipNode = nodeIPs.get(i);						
			byte[] sendData = new byte[1000];
			sendData = ipNode.getBytes();
			
			InetAddress IPAddress = InetAddress.getByName(Network.VGW_ADDRESS);
			
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Constants.UDPSHELL_VGW_PORT);
			clientSocket.send(sendPacket);
		}
		
		clientSocket.close();
	}
	
	private void manageDtnMessages(String msgString, String fromIPString, InetAddress fromIP) throws UnknownHostException, IOException {		
		List<String> elements = new LinkedList<String>();
		StringTokenizer st = new StringTokenizer(msgString, "#");
		while(st.hasMoreTokens()) {
			elements.add(st.nextToken());
		}
		
		if(elements.size() == 4) {
			
			clientSocket = new DatagramSocket();
			
			String pppNodeIP = "";
			if(elements.get(1).contains(Network.WLAB_LAB_IPV6_PREFIX)
			|| elements.get(1).contains(Network.WLAB_LAB_IPV6_PREFIX_SHORT)) {
				pppNodeIP = Network.WLAB_LAB_IPV6_PREFIX + "::" + Network.PPP_NODE_ID;
			} else
			if(elements.get(1).contains(Network.WLAB_OFFICE_IPV6_PREFIX)
			|| elements.get(1).contains(Network.WLAB_OFFICE_IPV6_PREFIX_SHORT)) {
				pppNodeIP = Network.WLAB_OFFICE_IPV6_PREFIX + "::" + Network.PPP_NODE_ID;
			}
			
			/** Messages from node gateway to VGW */
			if(fromIP.equals(InetAddress.getByName(Network.WLAB_LAB_IPV6_PREFIX + "::" + Network.PPP_NODE_ID))
			|| fromIP.equals(InetAddress.getByName(Network.WLAB_OFFICE_IPV6_PREFIX + "::" + Network.PPP_NODE_ID))) {
				
				String dtnString = elements.get(0) + "#" + nodeIdToIPv6(elements.get(1), fromIP.getHostAddress()) + "#" + elements.get(2) + "#" + elements.get(3);						
				byte[] sendData = new byte[Constants.DTN_MESSAGE_SIZE];
				sendData = dtnString.getBytes();
				
				InetAddress IPAddress = InetAddress.getByName(Network.VGW_ADDRESS);
				
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Constants.DTN_VGW_PORT);
				clientSocket.send(sendPacket);
				
			} else
				
			/** Messages from VGW to node gateway */
			if(!pppNodeIP.equals("")) {
				String dtnString = elements.get(0) + "#" + ipv6ToNodeID(elements.get(1)) + "#" + elements.get(2) + "#" + elements.get(3);
				byte[] sendData = new byte[Constants.DTN_MESSAGE_SIZE];
				sendData = dtnString.getBytes();
				
				InetAddress IPAddress = InetAddress.getByName(pppNodeIP);
										
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, Constants.DTN_WSI_PORT);
				clientSocket.send(sendPacket);
				
			}
			
			clientSocket.close();
			
		} else {
			logger.error("Malformed DTN message: " + msgString);
		}
	}
}
