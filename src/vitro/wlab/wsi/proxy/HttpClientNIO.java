package vitro.wlab.wsi.proxy;

import java.io.IOException;
import java.util.Hashtable;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.concurrent.FutureCallback;
import org.apache.log4j.Logger;


public class HttpClientNIO extends Thread {

	private Logger logger = Logger.getLogger(getClass());

	//the queue receives the requests to send to an origin-server
	static ArrayBlockingQueue<ProxyMessageContext> httpInQueue = new ArrayBlockingQueue<ProxyMessageContext>(100);

	//hashtable is used to keep message-id, there is no way to include message-id in request
	//message-id of request is used for message-id of corresponding response, important for proxy to work!
	static private Hashtable<FutureCallback<HttpResponse>, ProxyMessageContext> requestContextMap = new Hashtable<FutureCallback<HttpResponse>, ProxyMessageContext>(100);

	//interface-function to let other classes/modules access the queue
	public void makeHttpRequest(ProxyMessageContext context) {
		try {
			httpInQueue.put(context);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void run() {			

		this.setName("Http_Client");

		try {

			//create an instance of HttpAsyncClient and start it. Uses by default two worker-threads (dispatcher)!
			HttpAsyncClient httpclient = new DefaultHttpAsyncClient();	        	
			httpclient.start();

			while (!Thread.interrupted()) {

				ProxyMessageContext context = httpInQueue.take();		//blocking operation, waits for requests

				//cast HttpRequest manually, framework don't work with class HttpRequest
				//first get the request-method
				HttpRequestBase request = null;		       	
				String method = context.getHttpRequest().getRequestLine().getMethod().toLowerCase();

				if (method.equals("get")){
					request = new HttpGet(context.getHttpRequest().getRequestLine().getUri());
				}
				else if (method.equals("put")) {
					request = new HttpPut(context.getHttpRequest().getRequestLine().getUri());
				}
				else if (method.equals("post")) {
					request = new HttpPost(context.getHttpRequest().getRequestLine().getUri());
				}
				else if (method.equals("delete")) {
					request = new HttpDelete(context.getHttpRequest().getRequestLine().getUri());
				}
				else if (method.equals("options")) {
					request = new HttpOptions(context.getHttpRequest().getRequestLine().getUri());
				}
				else if (method.equals("trace")) {
					request = new HttpTrace(context.getHttpRequest().getRequestLine().getUri());
				}
				else if (method.equals("head")) {
					request = new HttpHead(context.getHttpRequest().getRequestLine().getUri());
				}
				else {
					logger.error("Error in Http-ClientNIO, bad request-method!");
				}
				//TODO: add connect method
				//TODO: convert to async classes -> HttpAsyncGet...

				request.setHeaders(context.getHttpRequest().getAllHeaders());
				request.setParams(context.getHttpRequest().getParams());

				//future is used to receive response asynchronous, without blocking anything
				FutureCallback<HttpResponse> fc = new FutureCallback<HttpResponse>() {

					//this is called when response is received
					public void completed(final HttpResponse response) {
						/* */
						ProxyMessageContext context = requestContextMap.remove(this);
						if (context != null){
							context.setHttpResponse(response);
							Mapper.getInstance().putHttpResponse(context);
						}
					}

					public void failed(final Exception ex) {
						logger.warn("HTTP Client Request failed");
						ProxyMessageContext context = requestContextMap.remove(this);
						requestContextMap.remove(this);
						if (context != null){
							/* null indicates no response */
							context.setHttpResponse(null);
							Mapper.getInstance().putHttpResponse(context);
						}
					}

					public void cancelled() {
						logger.info("HTTP Client Request cancelled");
						ProxyMessageContext context = requestContextMap.remove(this);
						requestContextMap.remove(this);
						if (context != null){
							/* null indicates no response */
							context.setHttpResponse(null);
							Mapper.getInstance().putHttpResponse(context);
						}
					}

				};

				requestContextMap.put(fc, context);
				httpclient.execute(request, fc);

			}

		} 

		catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			// httpclient.getConnectionManager().shutdown();
		}
	}
}
