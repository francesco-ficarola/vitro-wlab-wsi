package vitro.wlab.wsi.coap.client.integration.model;

import java.util.HashMap;
import java.util.Map;

import vitro.wlab.wsi.coap.client.integration.exception.VitroGatewayException;


/*
 * Available physical parameters on sensor nodes, e.g temperature, humidity, pressure, light,.... 
 * */
public class Resource {

	public static String PHENOMENOM_TEMPERATURE = "temperature";
	public static String PHENOMENOM_HUMIDITY = "humidity";
	//public static String PHENOMENOM_LIGHT = "light";
	public static String PHENOMENOM_WIND_SPEED = "windspeed";
	public static String PHENOMENOM_CO = "co";
	public static String PHENOMENOM_CO2 = "co2";
	public static String PHENOMENOM_PRESSURE = "pressure";
	public static String PHENOMENOM_BAROMETRIC_PRESSURE = "barometricpressure";
	
	private static Resource RES_TEMPERATURE = new Resource(PHENOMENOM_TEMPERATURE);
	private static Resource RES_HUMIDITY = new Resource(PHENOMENOM_HUMIDITY);
	private static Resource RES_WIND_SPEED = new Resource(PHENOMENOM_WIND_SPEED);
	private static Resource RES_CO = new Resource(PHENOMENOM_CO);
	private static Resource RES_CO2 = new Resource(PHENOMENOM_CO2);
	private static Resource RES_PRESSURE = new Resource(PHENOMENOM_PRESSURE);
	private static Resource RES_BAROMETRIC_PRESSURE = new Resource(PHENOMENOM_BAROMETRIC_PRESSURE);
	
	
	public static String UOM_CELSIUS = "celsius";
	public static String UOM_KELVIN = "kelvin";
	//private static String PHENOMENOM_LIGHT = "light";
	public static String UOM_PERCENT = "percent";
	public static String UOM_KMH = "kmH";
	public static String UOM_PARTS_PER_MILLION = "ppm";
	public static String UOM_PARTS_PER_BILLION = "ppb";
	public static String UOM_PASCAL = "pascal";
	public static String UOM_HECTO_PASCAL = "hectoPascal";
	public static String UOM_DIMENSIONLESS = "dimensionless";
	
	private static Map<String, String> defaultUomMap = new HashMap<String, String>();
	static{
		defaultUomMap.put(Resource.PHENOMENOM_TEMPERATURE, UOM_KELVIN);
		defaultUomMap.put(Resource.PHENOMENOM_HUMIDITY, UOM_PERCENT);
		defaultUomMap.put(Resource.PHENOMENOM_WIND_SPEED, UOM_KMH);
		defaultUomMap.put(Resource.PHENOMENOM_CO, UOM_PARTS_PER_MILLION);
		defaultUomMap.put(Resource.PHENOMENOM_CO2, UOM_PARTS_PER_MILLION);
		defaultUomMap.put(Resource.PHENOMENOM_PRESSURE, UOM_PASCAL);
		defaultUomMap.put(Resource.PHENOMENOM_BAROMETRIC_PRESSURE, UOM_HECTO_PASCAL);
	}
	
	private static Map<String, Resource> resourceMap = new HashMap<String, Resource>();
	static{
		resourceMap.put(Resource.PHENOMENOM_TEMPERATURE, RES_TEMPERATURE);
		resourceMap.put(Resource.PHENOMENOM_HUMIDITY, RES_HUMIDITY);
		resourceMap.put(Resource.PHENOMENOM_WIND_SPEED, RES_WIND_SPEED);
		resourceMap.put(Resource.PHENOMENOM_CO, RES_CO);
		resourceMap.put(Resource.PHENOMENOM_CO2, RES_CO2);
		resourceMap.put(Resource.PHENOMENOM_PRESSURE, RES_PRESSURE);
		resourceMap.put(Resource.PHENOMENOM_BAROMETRIC_PRESSURE, RES_BAROMETRIC_PRESSURE);
	}
	
	String name;

	public static Resource getResource(String resourceName) throws VitroGatewayException{
		Resource result = resourceMap.get(resourceName);
		if(result == null){
			throw new VitroGatewayException("No resource mapping for " + resourceName);
		}
		return result;
	}
	
	
	private Resource(String name) {
		super();
		this.name = name;
	}
	
	public String getUnityOfMeasure(){
		return defaultUomMap.get(name);
	}
	
	public String getName(){
		return name;
	}
	
	
	
	
	
}
