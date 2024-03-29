import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.ArrayList;
import java.util.List;

public class Main {
	
	public static String host;
	
	public static void main(String[] args) throws RemoteException, UnknownHostException, MalformedURLException, NotBoundException, AlreadyBoundException {
		int[] ports = new int[] {1099,5000,5001,5002,5003,5004,5005,5006,5007,5008,5009,5010};
		host = InetAddress.getLocalHost().getHostAddress();
		
		if(args.length == 0) {					// If eclipse is used a testcase will be loaded, otherwise input from terminal will be used in format (port,id) for each component
			
			args = new TestCases(ports).getTestCase(2);
			
		}
		
		List<Component> components = readComponents(args);
		
		components = connectComponents(components);
		
		System.out.println("---------------------------------------------------------------------------------------------------------");
		System.out.print("Components in unidirectional ring: ");
		for(Component c : components) {
			if(components.indexOf(c) > 0) 
				System.out.print("-"+c.id);
			else
				System.out.print(c.id);
		}
		System.out.println();
		System.out.println("---------------------------------------------------------------------------------------------------------");
		System.out.println();
		
		Integer round = 1;
		
		// Perform rounds until only one component is still active
		while(components.size() > 1) {
		
			System.out.println("-------------------------------------------  Round " + round + " started  -------------------------------------------");
			
			components = executeRound(components);
			
			System.out.println("-------------------------------------------  Round " + round + " finished -------------------------------------------");
			System.out.println();
			
			round++;
			
		}
		
		round--;
		
		System.out.println("Component with tid " + components.get(0).getTID() + " is elected in a total of " + round + " round(s)!");
		
	}
	
	public static List<Component> readComponents(String[] args) throws RemoteException, MalformedURLException, AlreadyBoundException {
		
		List<Component> components = new ArrayList<>();
		
		for(int i = 0; i < args.length; i+=2) {

			Integer port = Integer.parseInt(args[i]);
			Integer id = Integer.parseInt(args[i+1]);

			Component c = new Component(id,port,host);
			components.add(c);

			LocateRegistry.createRegistry(port);
			Naming.bind("rmi://"+host+":"+port+"/component", c);
		}
		
		return components;
		
	}
	
	public static List<Component> connectComponents(List<Component> components) throws RemoteException {
		
		for(Component c : components) {
			
			Integer index = components.indexOf(c);
			Integer thisPort = c.getPort();
			
			if(index < components.size()-1) {
				Component next = components.get(index+1);
				c.setNextPort(next.getPort());
				next.setLastPort(thisPort);
			}
		}
		
		Component first = components.get(0);
		Component last = components.get(components.size()-1);
		
		first.setLastPort(last.getPort());
		last.setNextPort(first.getPort());
		
		return components;
		
	}
	
	public static List<Component> executeRound(List<Component> components) throws RemoteException, MalformedURLException, NotBoundException {
		
		// Let each component send his TID to next component
		System.out.println("-----------------  Phase 1: sending TID from first and receiving NTID on next component  ----------------");
		for(Component c : components) {
			c.sendTID();
			System.out.println("Component " + c.getTID() + " sends TID and is received as NTID at next component " + c.getNextComponent().getTID());
		}
		
		// Let each component send his NNTID to next component
		System.out.println("-----------  Phase 2: compute max(TID,NTID) for each component and sent it to next component  -----------");
		for(Component c : components) {
			c.sendNNTID();
			System.out.println("Component " + c.getTID() + " computes max("+c.getTID() +","+ c.getNTID() +")="+c.getNextComponent().getNNTID()+" and sends it to component " + c.getNextComponent().getTID());
		}
		
		System.out.println("------------  Phase 3: check condition to become passive or stay active for each component  -------------");
		
		for(Component c : components) {
			c.checkCondition();
		}
		
		components.removeIf(n -> n.status == false);
		
		components = connectComponents(components);
		
		return components;
	}

}
