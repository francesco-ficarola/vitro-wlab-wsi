package vitro.wlab.wsi.proxy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.interfaces.CoapChannel;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClient;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.AbstractCoapMessage.CoapHeaderOptionType;
import org.ws4d.coap.messages.BasicCoapRequest;
import org.ws4d.coap.messages.CoapEmptyMessage;

import vitro.wlab.wsi.coap.Constants;

public class CoapClientProxy {
	private Logger logger = Logger.getLogger(getClass());

	// queue is used to receive coap-requests from mapper
	private ArrayBlockingQueue<ProxyMessageContext> coapClientRequestQueue = new ArrayBlockingQueue<ProxyMessageContext>(100);
	private HashMap<CoapChannel, ProxyMessageContext> coapContextMap = new HashMap<CoapChannel, ProxyMessageContext>(100);

	private CoapRequestListenerThread coapRequestListener;

	public CoapClientProxy() {
		super();
        logger.addAppender(new ConsoleAppender(new SimpleLayout()));
        // ALL | DEBUG | INFO | WARN | ERROR | FATAL | OFF:
        logger.setLevel(Level.WARN);
		
		CoapRequestListenerThread coaprequestlistenerthread = new CoapRequestListenerThread();
		this.coapRequestListener = coaprequestlistenerthread;
		coaprequestlistenerthread.start();
	}

	// access-function for other classes to pass a message
	public void makeRequest(ProxyMessageContext context) {
		try {
			coapClientRequestQueue.put(context);

			synchronized (coapRequestListener) {
				coapRequestListener.notify();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
		
	private boolean checkRemoteAddress(ProxyMessageContext context) {
		CoapRequest request = context.getCoapRequest();

		/* Check remote Host */
		if (context.getRemoteAddress() == null) {
			/* no remote address set */
			InetAddress remoteAddress = null;

			/* retrieve Host from CoapHeader Option */
			try {
				remoteAddress = InetAddress.getByName(request.getUriHost());
				context.setRemoteAddress(remoteAddress);
			} catch (UnknownHostException e) {
				remoteAddress = null;
			}

			/* check if Host could be found */
			if (remoteAddress == null) {
				logger.warn("Invalid Uri Host, request will be dropped!");
				return false;
			}
			
		}

		/* Check remote port */
		if (context.getRemotePort() == 0) {
			int remotePort = request.getUriPort();
			if (remotePort > 0) {
				/* set port from Coap Header */
				context.setRemotePort(remotePort);
			} else {
				context.setRemotePort(Constants.COAP_DEFAULT_PORT);
			}
		}		

		
		
		return true;
	}
		
		//this thread waits for a message in coapINq_ and sends it
		class CoapRequestListenerThread extends Thread implements CoapClient{
	        
	        public void run() {

			this.setName("CoapRequestListenerThread");

			// start jcoap-framework
			CoapChannelManager connectionManager = BasicCoapChannelManager
					.getInstance();

			while (!Thread.interrupted()) {
				ProxyMessageContext context = null;
				try {

					synchronized (this) {
						while (coapClientRequestQueue.isEmpty())
							try {
								wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						context = coapClientRequestQueue.take(); // blocking
					}
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				} 


				if (checkRemoteAddress(context)){
					// create channel
					CoapClientChannel channel;
					channel = connectionManager.connect(this, context.getRemoteAddress(), context.getRemotePort());
					if (channel != null) {
						/* save the request in a hashmap to assign the response to the right request */
						coapContextMap.put(channel, context);
						// send message
						/* casting to BasicCoapRequest is necessary to allow an efficient header duplication*/
						BasicCoapRequest originRequest = (BasicCoapRequest) context.getCoapRequest();
						BasicCoapRequest request= (BasicCoapRequest) channel.createRequest(originRequest.isReliable(), originRequest.getRequestCode());
						request.copyHeaderOptions(originRequest); 

						if (!context.isTranslate()){
							/* CoAP to CoAP */
							/* check path: if this is a coap-coap proxy request than the proxy uri needs to be translated to path options
							 * and the proxy uri needs to be removed as this is no longer a proxy request */
							request.setUriPath(context.getUri().getPath());
							request.removeOption(CoapHeaderOptionType.Proxy_Uri);
					}
						request.setPayload(originRequest.getPayload());
						channel.sendMessage(request);
					}
				} else {
					/* could not determine the final destination */
					logger.error("Error: unknown host: " + context.getRemoteAddress().getHostName());
				}
			}
		}
	        
		@Override
		public void onResponse(CoapClientChannel channel, CoapResponse response) {
			ProxyMessageContext context = coapContextMap.get(channel);
			channel.close();
			if (context != null){
				context.setCoapResponse(response);
				Mapper.getInstance().putCoapResponse(context);
			}
		}

		@Override
		public void onConnectionFailed(CoapClientChannel channel,
				boolean notReachable, boolean resetByServer) {
			ProxyMessageContext context = coapContextMap.get(channel);
			channel.close();
			if (context != null){
				logger.error("Coap Client Connection failed!");
				context.setCoapResponse(null); //null indicates no response
				Mapper.getInstance().putCoapResponse(context);
			}
		}

		@Override
		public void onSeparateResponseAck(CoapClientChannel channel,
				CoapEmptyMessage message) {
			logger.error("Separate Response not implemented!");
			
		}

		@Override
		public void onSeparateResponse(CoapClientChannel channel,
				CoapResponse message) {
			logger.error("Separate Response not implemented!");
			
		}
	}
}
