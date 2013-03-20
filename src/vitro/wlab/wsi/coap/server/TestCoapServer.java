package vitro.wlab.wsi.coap.server;

import org.apache.log4j.Logger;
import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapMessage;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapServer;
import org.ws4d.coap.interfaces.CoapServerChannel;
import org.ws4d.coap.messages.CoapMediaType;
import org.ws4d.coap.messages.CoapResponseCode;

import vitro.wlab.wsi.coap.Constants;
import vitro.wlab.wsi.coap.Functions;

public class TestCoapServer implements CoapServer {
	
	private static Logger logger = Logger.getLogger(TestCoapServer.class);
    private static final int PORT = Constants.COAP_DEFAULT_PORT;

    public static void main(String[] args) {
    	logger.info("Start CoAP Server on port " + PORT);
        TestCoapServer server = new TestCoapServer();

        CoapChannelManager channelManager = BasicCoapChannelManager.getInstance();
        channelManager.createServerListener(server, PORT);
    }

	@Override
	public CoapServer onAccept(CoapRequest request) {
		logger.info("Accept connection...");
		return this;
	}

	@Override
	public void handleRequest(CoapServerChannel channel, CoapRequest request) {
		logger.info("Received message: " + request.toString());
		
		CoapMessage response = channel.createResponse(request, CoapResponseCode.Content_205);
		String req = request.getUriPath();
		logger.info(req);
		
		if(req.contains("well-known/core")) {
			response.setContentType(CoapMediaType.text_plain);
			response.setPayload("</st>;ct=42,</sh>;ct=42".getBytes());
		} else {
			short data;
			if(req.equals("/st")) {
				data = 30194;
			} else
			if(req.equals("/sh")) {
				data = 4756;
			} else {
				data = 1234;
			}
			response.setContentType(CoapMediaType.octet_stream);
			byte[] payload = Functions.shortToByteArray(data);
			response.setPayload(payload);
		}
		
		channel.sendMessage(response);
	}

	@Override
	public void separateResponseFailed(CoapServerChannel channel) {
		// TODO Auto-generated method stub
		
	}
}
