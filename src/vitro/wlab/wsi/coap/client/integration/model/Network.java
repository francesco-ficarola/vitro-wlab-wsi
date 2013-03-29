package vitro.wlab.wsi.coap.client.integration.model;

public class Network {
//	public static final String VGW_ADDRESS = "localhost"; // From thinkpad to thinkpad
//	public static final String VGW_ADDRESS = "192.168.0.120"; // From wiserver.dis.uniroma1.it to thinkpad (DIAG LAN)
	public static final String VGW_ADDRESS = "vgw.vitro.w-lab.it"; // From wiserver.dis.uniroma1.it to WLAB VGW
	
//	public static final String WLAB_OFFICE_PROXY_ADDRESS = "localhost";
	public static final String WLAB_OFFICE_PROXY_ADDRESS = "wiserver.dis.uniroma1.it";
	public static final String WLAB_OFFICE_IPV6_PREFIX = "fec0:0:0:1";
	public static final String WLAB_OFFICE_IPV6_PREFIX_SHORT = "fec0::1";
//	public static final String WLAB_LAB_PROXY_ADDRESS = "localhost";
	public static final String WLAB_LAB_PROXY_ADDRESS = "wsnserver.dis.uniroma1.it";
	public static final String WLAB_LAB_IPV6_PREFIX = "fec0:0:0:2";
	public static final String WLAB_LAB_IPV6_PREFIX_SHORT = "fec0::2";
	
	public final static String PPP_NODE_ID = "0";
}
