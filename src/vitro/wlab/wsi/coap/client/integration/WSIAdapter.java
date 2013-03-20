package vitro.wlab.wsi.coap.client.integration;



import java.util.List;

import vitro.wlab.wsi.coap.client.integration.exception.WSIAdapterException;
import vitro.wlab.wsi.coap.client.integration.model.Node;
import vitro.wlab.wsi.coap.client.integration.model.Observation;
import vitro.wlab.wsi.coap.client.integration.model.Resource;


/*
 * Interface describing the underlying component to interact with the sensor networks.
 * 
 * Its implementation is the actual interaction protocol with the networks (e.g serial, http, ....)
 * 
 * */
public interface WSIAdapter {

	/*
	 * This method returns the list of all Nodes managed by VGW.
	 * 
	 * If a discovery process is not available on the underlying WSNs, a configuration file could be used 
	 * 
	 * */
	List<Node> getAvailableNodeList() throws WSIAdapterException;
	
	/*
	 * Used to retrieve a sensor node's managed resources
	 * */
	List<Resource> getResources(Node node) throws WSIAdapterException;
	
	/*
	 * Used to get an observation of a resource from a node
	 * */
	Observation getNodeObservation(Node node, Resource resource) throws WSIAdapterException;
	
	
	
}
