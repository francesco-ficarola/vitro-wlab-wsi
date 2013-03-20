package vitro.wlab.wsi.proxy;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.Logger;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapServer;
import org.ws4d.coap.interfaces.CoapServerChannel;
import org.ws4d.coap.messages.BasicCoapResponse;
import org.ws4d.coap.messages.CoapResponseCode;

import vitro.wlab.wsi.coap.Constants;


public class CoapServerProxy implements CoapServer{
	
	private Logger logger = Logger.getLogger(getClass());
    private static final int LOCAL_PORT = 5683;					//port on which the server is listening
    
    //coapOUTq_ receives a coap-response from mapper in case of coap-http
    private ArrayBlockingQueue<ProxyMessageContext> coapOutQueue = new ArrayBlockingQueue<ProxyMessageContext>(100);
    CoapChannelManager channelManager;
    
    //this class sends the response back to the client in case coap-http
    public class CoapSender extends Thread {
    	public void run() {
    		this.setName("CoapSender");
    		while (!Thread.interrupted()) {
    			try {
					ProxyMessageContext context = coapOutQueue.take();	
					/* TODO: make cast safe */
					CoapServerChannel channel = (CoapServerChannel) context.getCoapRequest().getChannel();
					/* we need to cast to allow an efficient header copy */
					BasicCoapResponse clientResponse = (BasicCoapResponse) context.getCoapResponse();
					if(clientResponse != null) {
						BasicCoapResponse response = (BasicCoapResponse) channel.createResponse(context.getCoapRequest(), clientResponse.getResponseCode());
						/* copy header and payload */
						response.copyHeaderOptions(clientResponse);
						response.setPayload(clientResponse.getPayload());
	
						channel.sendMessage(response);
					} else {
						logger.warn("The response is null!");
					}
					channel.close();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
    		}
    	}   	
    }
    
    //constructor of coapserver-class, initiates the jcoap-components and starts CoapSender
    public CoapServerProxy() {

        channelManager = BasicCoapChannelManager.getInstance();
        channelManager.createServerListener(this, LOCAL_PORT);
        CoapSender sender = new CoapSender();
        sender.start();
    }
    
    //interface-function for the message-queue
    public void receivedResponse(ProxyMessageContext context) {
    	try {
    		
			coapOutQueue.put(context);
         
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

    @Override
    public CoapServer onAccept(CoapRequest request) {
        logger.info("Accept connection: " + request.toString());
        /* accept every incomming connection */
        return this;
    }

    @Override
	public void handleRequest(CoapServerChannel channel, CoapRequest request) {
    	/* draft-08:
    	 *  CoAP distinguishes between requests to an origin server and a request
   			made through a proxy.  A proxy is a CoAP end-point that can be tasked
   			by CoAP clients to perform requests on their behalf.  This may be
   			useful, for example, when the request could otherwise not be made, or
   			to service the response from a cache in order to reduce response time
   			and network bandwidth or energy consumption.
   			
   			CoAP requests to a proxy are made as normal confirmable or non-
			confirmable requests to the proxy end-point, but specify the request
   			URI in a different way: The request URI in a proxy request is
   			specified as a string in the Proxy-Uri Option (see Section 5.10.3),
   			while the request URI in a request to an origin server is split into
   			the Uri-Host, Uri-Port, Uri-Path and Uri-Query Options (see
   			Section 5.10.2).
    	*/
    	URI proxyUri = null;
        
        //-------------------in this case we want a translation to http----------------------------

		try {
			proxyUri = new URI(request.getProxyUri());
		} catch (Exception e) {
			proxyUri = null;
		}

    	if (proxyUri == null){
    		/* PROXY URI MUST BE AVAILABLE */
    		logger.error("Received a Non Proxy CoAP Request, send error");
    		/*FIXME What is the right error code for this case?*/
    		channel.sendMessage(channel.createResponse(request, CoapResponseCode.Not_Found_404));
    		channel.close();
    		return;    		
    	}
    	
    	/*check scheme, should we translate */
    	boolean translate;
    	if (proxyUri.getScheme().compareToIgnoreCase("http") == 0){
    		translate = true;
    	} else if (proxyUri.getScheme().compareToIgnoreCase("coap") == 0){
    		translate = false;
    	} else {
    		/*unknown scheme, TODO send error*/
    		logger.error("Unknown Proxy Uri Scheme, send error");
    		/*FIXME What is the right error code for this case?*/
    		channel.sendMessage(channel.createResponse(request, CoapResponseCode.Not_Found_404));
    		channel.close();
    		return;
    	}
    	
    	/* parse URL */
    	try {
    		InetAddress remoteAddress = InetAddress.getByName(proxyUri.getHost());
    		int remotePort = proxyUri.getPort();
    		if (remotePort == -1){
    			remotePort = Constants.COAP_DEFAULT_PORT;
    		}
    		ProxyMessageContext context = new ProxyMessageContext(request, translate, remoteAddress, remotePort, proxyUri);
    		Mapper.getInstance().putCoapRequest(context);
		} catch (UnknownHostException e) {
			/*bad proxy URI*/
			logger.error("Invalid Proxy Uri Scheme, send error");
    		/*FIXME What is the right error code for this case?*/
    		channel.sendMessage(channel.createResponse(request, CoapResponseCode.Not_Found_404));
    		channel.close();
			e.printStackTrace();
		}
    	
    }

	@Override
	public void separateResponseFailed(CoapServerChannel channel) {
		// TODO Auto-generated method stub
		
	}
   
}

