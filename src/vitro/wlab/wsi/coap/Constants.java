package vitro.wlab.wsi.coap;

public class Constants {
    public final static int COAP_DEFAULT_PORT = 5683;
    public final static int COAP_DEFAULT_MAX_AGE_S = 0;
    public final static int COAP_DEFAULT_MAX_AGE_MS = COAP_DEFAULT_MAX_AGE_S * 1000;
    
    public final static int PROXY_UDPFORWARDER_PORT = 10210;
    
    public final static int DTN_WSI_PORT = 10210;
    public final static int DTN_VGW_PORT = 10211;
    public final static int DTN_MESSAGE_SIZE = 100;
    public final static int DTN_REQUEST_TIMEOUT = 5;
    
    public final static int UDPSHELL_GATEWAY_PORT = 2000;
    public final static int UDPSHELL_VGW_PORT = 2001;
    public final static int PROXY_RESPONSE_TIMEOUT = 5000;
    
    public final static int HTTP_PROXY_PORT = 8080;
}
