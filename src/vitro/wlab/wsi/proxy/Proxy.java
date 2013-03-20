package vitro.wlab.wsi.proxy;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;
import org.ws4d.coap.connection.BasicCoapSocketHandler;


public class Proxy {
	static Logger logger = Logger.getLogger(BasicCoapSocketHandler.class);

	public static void main(String[] args) {
		CommandLineParser cmdParser = new GnuParser();
		Options options = new Options();
		/* Add command line options */
		options.addOption("c", "default-cache-time", true, "Default caching time in seconds");
		CommandLine cmd = null;
		try {
			cmd = cmdParser.parse(options, args);
		} catch (ParseException e) {
			logger.error( "Unexpected exception:" + e.getMessage() );
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "jCoAP-Proxy", options );
			System.exit(-1);
		}
		
		/* evaluate command line */
		if(cmd.hasOption("c")) {
			try {
				Mapper.getInstance().setDefaultCacheTime(Integer.parseInt(cmd.getOptionValue("c")));
				logger.info("Set caching time to " + cmd.getOptionValue("c") + " seconds");
			} catch (NumberFormatException e) {
				logger.error( "Unexpected exception:" + e.getMessage() );
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "jCoAP-Proxy", options );
				System.exit(-1);
			}
		}
	
		HttpServerNIO httpserver = new HttpServerNIO();
		HttpClientNIO httpclient = new HttpClientNIO();
		CoapClientProxy coapclient = new CoapClientProxy();
		CoapServerProxy coapserver = new CoapServerProxy();	

		
		Mapper.getInstance().setHttpServer(httpserver);
		Mapper.getInstance().setHttpClient(httpclient);
		Mapper.getInstance().setCoapClient(coapclient);
		Mapper.getInstance().setCoapServer(coapserver);
	
		httpserver.start();
		httpclient.start();
		
		ProxyUdpForwarder udpForwarder = new ProxyUdpForwarder();
		Thread threadUdpForwarder = new Thread(udpForwarder);
		threadUdpForwarder.start();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.info("===END===");
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		});

		ProxyRestInterface restInterface = new ProxyRestInterface();
		restInterface.start();
	}
}
