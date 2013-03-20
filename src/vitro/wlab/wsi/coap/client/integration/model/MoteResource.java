package vitro.wlab.wsi.coap.client.integration.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import vitro.wlab.wsi.coap.Functions;
import vitro.wlab.wsi.coap.client.integration.exception.VitroGatewayException;


public class MoteResource {
	
	private static Logger logger = Logger.getLogger(MoteResource.class);
	
	public static String TEMPERATURE = "st";
	public static String HUMIDITY = "sh";
	//public static String LIGHT = "sl";
	public static String WIND_SPEED = "sw";
	public static String CO = "co";
	public static String CO2 = "co2";
	public static String PRESSURE = "sp";
	public static String BAROMETRIC_PRESSURE = "sbp";
	public static String RESOURCE_DISCOVERY = ".well-known/core";
	
	private static Map<String, Resource> moteResourceMap = new HashMap<String, Resource>();
	static {
		try {
			moteResourceMap.put(MoteResource.TEMPERATURE, Resource.getResource(Resource.PHENOMENOM_TEMPERATURE));
			moteResourceMap.put(MoteResource.HUMIDITY, Resource.getResource(Resource.PHENOMENOM_HUMIDITY));
//			moteResourceMap.put(MoteResource.LIGHT, Resource.getResource(Resource.PHENOMENOM_LIGHT));
			moteResourceMap.put(MoteResource.WIND_SPEED, Resource.getResource(Resource.PHENOMENOM_WIND_SPEED));
			moteResourceMap.put(MoteResource.CO, Resource.getResource(Resource.PHENOMENOM_CO));
			moteResourceMap.put(MoteResource.CO2, Resource.getResource(Resource.PHENOMENOM_CO2));
			moteResourceMap.put(MoteResource.PRESSURE, Resource.getResource(Resource.PHENOMENOM_PRESSURE));
			moteResourceMap.put(MoteResource.BAROMETRIC_PRESSURE, Resource.getResource(Resource.PHENOMENOM_BAROMETRIC_PRESSURE));
		} catch (VitroGatewayException e) {
			logger.error(e);
		}
	}
	
	public static Resource getResource(String moteResourceName) throws VitroGatewayException {
		Resource result = moteResourceMap.get(moteResourceName);
		if(result == null){
			throw new VitroGatewayException("No mote resource mapping for " + moteResourceName);
		}
		return result;
	}
	
	public static String getMoteUriResource(Resource resource) throws VitroGatewayException {
		String result = Functions.getKeyByValue(moteResourceMap, resource);
		if(result == null) {
			throw new VitroGatewayException("No mote resource mapping for " + resource.getName());
		}
		return result;
	}
	
	public static boolean containsKey(String resourceName) {
		boolean isPresent = false;
		if(moteResourceMap.containsKey(resourceName)) {
			isPresent = true;
		}
		return isPresent;
	}
	
	public static boolean containsValue(Resource resource) {
		boolean isPresent = false;
		if(moteResourceMap.containsValue(resource)) {
			isPresent = true;
		}
		return isPresent;
	}
}
