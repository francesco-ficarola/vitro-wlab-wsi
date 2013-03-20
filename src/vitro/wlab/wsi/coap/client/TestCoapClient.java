package vitro.wlab.wsi.coap.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClient;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.CoapEmptyMessage;
import org.ws4d.coap.messages.CoapMediaType;
import org.ws4d.coap.messages.CoapRequestCode;

import vitro.wlab.wsi.coap.Constants;
import vitro.wlab.wsi.coap.Functions;

public class TestCoapClient implements CoapClient {
	/**
	 * SERVER_ADDRESS: Proxy IP or CoAP Server Node IPv6 (without Proxy)
	 * PORT: Proxy port or CoAP Server Node port (without Proxy)
	 */
	private static final String SERVER_ADDRESS = "wiserver.dis.uniroma1.it";
    private static final int PORT = Constants.COAP_DEFAULT_PORT;
    
    CoapChannelManager channelManager = null;
    CoapClientChannel clientChannel = null;

    public static void main(String[] args) {
        System.out.println("Start CoAP Client");
        TestCoapClient client = new TestCoapClient();
        client.channelManager = BasicCoapChannelManager.getInstance();
        
        BufferedReader key = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
        	try {
				key.readLine();
				client.runTestClient();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }
    
    public void runTestClient(){
    	try {
			clientChannel = channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
			CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
			coapRequest.setContentType(CoapMediaType.octet_stream);
//			coapRequest.setUriPath("/sh");
//			coapRequest.setUriPath("/.well-known/core");
			coapRequest.setProxyUri("coap://[fec0::2]:61616/st");
			clientChannel.sendMessage(coapRequest);
			System.out.println("Sent Request: " + coapRequest.toString());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
    }

	@Override
	public void onConnectionFailed(CoapClientChannel channel, boolean notReachable, boolean resetByServer) {
		System.out.println("Connection Failed");
		if(notReachable) {
			System.out.println("The server is not reachable.");
		}
	}

	@Override
	public void onResponse(CoapClientChannel channel, CoapResponse response) {
		System.out.println("Received response code: " + response.getResponseCode());
		System.out.println("CoAP Message ID: " + response.getMessageID());
		System.out.println("CoAP Server address: " + response.getChannel().getRemoteAddress());
		
		if(response.getPayload().length > 0) {
		byte[] payloadBytes = response.getPayload();
			if(payloadBytes.length > 2) {
				System.out.println(new String(payloadBytes));
			} else {
				short payload = Functions.byteArraytoShort(response.getPayload());
				System.out.println(payload);
			}
		} else {
			System.out.println("The payload is empty.");
		}
	}

	@Override
	public void onSeparateResponseAck(CoapClientChannel channel, CoapEmptyMessage message) {
		System.out.println("Received Ack of Separate Response");
	}

	@Override
	public void onSeparateResponse(CoapClientChannel channel, CoapResponse message) {
		System.out.println("Received Separate Response");
	}
}
