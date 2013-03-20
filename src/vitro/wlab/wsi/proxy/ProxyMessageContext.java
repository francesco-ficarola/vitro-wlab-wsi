package vitro.wlab.wsi.proxy;

import java.net.InetAddress;
import java.net.URI;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.nio.protocol.NHttpResponseTrigger;
import org.apache.log4j.Logger;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;


public class ProxyMessageContext {
	
	private Logger logger = Logger.getLogger(getClass());
	
	/*unique for reqMessageID, remoteHost, remotePort*/

	/* is true if a translation was done (always true for incoming http requests)*/
	private boolean translate;  //translate from coap to http

	/*in case of incoming coap request */
	private CoapRequest coapRequest;  //the coapRequest of the origin client (maybe translated)
	
	/* in case of incoming http request */
	private HttpRequest httpRequest;	//the httpRequest of the origin client (maybe translated)
	NHttpResponseTrigger trigger;

	/* in case of a coap response */
	private CoapResponse coapResponse; //the coap response of the final server

	/* in case of a http response */
	private HttpResponse httpResponse; //the http response of the final server

	private URI uri;
	
	private InetAddress remoteAddress;
	private int remotePort;
	
	private boolean fromCache = false;
	
	public ProxyMessageContext(CoapRequest request, boolean translate,
			InetAddress remoteAddress, int remotePort, URI uri) {

		this.coapRequest = request;
		this.translate = translate;
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
		this.uri = uri;

	}
	
	public ProxyMessageContext(HttpRequest request,
			InetAddress remoteAddress, int remotePort, URI uri, NHttpResponseTrigger trigger) {

		this.httpRequest = request;
		this.remoteAddress = remoteAddress;
		this.remotePort = remotePort;
		this.translate = true; //always translate http
		this.uri = uri;
		this.trigger = trigger;
		
		logger.info("Created HTTP Context");
	}
	
	public boolean isCoapRequest(){
		return coapRequest != null;
	}

	public boolean isHttpRequest(){
		return httpRequest != null;
	}
	
	public CoapRequest getCoapRequest() {
		return coapRequest;
	}
	
	public HttpRequest getHttpRequest() {
		return httpRequest;
	}
	
	public CoapResponse getCoapResponse() {
		return coapResponse;
	}

	public void setCoapResponse(CoapResponse coapResponse) {
		this.coapResponse = coapResponse;
	}

	public HttpResponse getHttpResponse() {
		return httpResponse;
	}

	public void setHttpResponse(HttpResponse httpResponse) {
		this.httpResponse = httpResponse;
	}

	public InetAddress getRemoteAddress() {
		return remoteAddress;
	}

	public int getRemotePort() {
		return remotePort;
	}
	
	public void setRemoteAddress(InetAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public boolean isTranslate() {
		return translate;
	}

	public void setTranslatedCoapRequest(CoapRequest request) {
		this.coapRequest = request;
	}

	public void setTranslatedHttpRequest(HttpRequest request) {
		this.httpRequest = request;
	}

	public URI getUri() {
		return uri;
	}

	public NHttpResponseTrigger getTrigger() {
		return trigger;
	}

	public boolean isFromCache() {
		return fromCache;
	}

	public void setFromCache(boolean isFromCache) {
		this.fromCache = isFromCache;
	}

}
