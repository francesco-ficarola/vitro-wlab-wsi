package vitro.wlab.wsi.coap.client.integration.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import vitro.wlab.wsi.coap.client.integration.WSIAdapterCoap;
import vitro.wlab.wsi.coap.client.integration.exception.VitroGatewayException;
import vitro.wlab.wsi.coap.client.integration.exception.WSIAdapterException;
import vitro.wlab.wsi.coap.client.integration.model.MoteResource;
import vitro.wlab.wsi.coap.client.integration.model.Network;
import vitro.wlab.wsi.coap.client.integration.model.Node;
import vitro.wlab.wsi.coap.client.integration.model.Observation;
import vitro.wlab.wsi.coap.client.integration.model.Resource;

public class TestWSIAdapterCoap extends Thread {

	private static WSIAdapterCoap wsi;
	private static String nodeID = "1";
	private static int countCoapDtn = 0;

	public TestWSIAdapterCoap() {
		wsi = new WSIAdapterCoap();
	}

	public static void main(String[] args) {
		if(args.length > 0) {
			nodeID = args[0];
		}
		TestWSIAdapterCoap test1 = new TestWSIAdapterCoap();

		Thread t1 = new Thread(test1) {
			@Override
			public void run() {

				System.out.println("Start TestWSIAdapterCoap");
				BufferedReader key = new BufferedReader(new InputStreamReader(System.in));
				
				while(true) {
					
					try {
						key.readLine();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					if(countCoapDtn % 2 == 0) {
						wsi.setDtnPolicy(true);
					} else {
						wsi.setDtnPolicy(false);
					}
					
					countCoapDtn++;
					
//					System.out.println("*** Getting Available Nodes\n");
//					try {
//						List<Node> list = wsi.getAvailableNodeList();
//						System.out.println("\n*** Number of nodes: " + list.size());
//						for(int i=0; i<list.size(); i++) {
//							System.out.println("Node: " + list.get(i).getId());
//						}
//					} catch (WSIAdapterException e) {
//						e.printStackTrace();
//					}
					
					Node node1 = new Node(Network.WLAB_OFFICE_IPV6_PREFIX + "::" + nodeID);
//					Node node1 = new Node(Network.WLAB_LAB_IPV6_PREFIX + "::" + nodeID);
					System.out.println("\n\nNode IPv6: " + node1.getId() + "\n\n");
					try {
						System.out.println("*** Getting Resources\n");
						List<Resource> resources = wsi.getResources(node1);
						for(Resource res : resources) {
							System.out.println("\n*** Resource Name: " + res.getName() + "\n");
//							Resource resource = MoteResource.getResource("st");
							Observation obs = wsi.getNodeObservation(node1, res);
							System.out.println("\n*** Resource Value: " + obs.getValue() + " " + res.getUnityOfMeasure() + "\n");
						}

					} catch (WSIAdapterException e) {
						e.printStackTrace();
					} catch (VitroGatewayException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			}
		};

		t1.start();
	}

}
