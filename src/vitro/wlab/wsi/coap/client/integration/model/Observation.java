package vitro.wlab.wsi.coap.client.integration.model;


public class Observation {
	
private Node node;
	
	private Resource resource;
	
	private String value;
	private String uom;
	
	private long timestamp;

	public Resource getResource() {
		return resource;
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Node getNode() {
		return node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getUom() {
		if(uom == null){
			return resource.getUnityOfMeasure();
		}
		return uom;
	}

	public void setUom(String uom) {
		this.uom = uom;
	}
	
	

}
