package socs.network.node;

import socs.network.util.Configuration;
import socs.network.message.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Vector;
import java.lang.Thread;


public class Router {

  CommunicationLayer comm = new CommunicationLayer();

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  //assuming that all routers are with 4 ports
  Link[] ports = new Link[4];
  int portIdx = 0;

  public Router(Configuration config) {
    rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    try {
        InetAddress ip = InetAddress.getLocalHost();
        rd.processIPAddress = ip.getHostAddress();
    } catch (Exception e) {
        e.printStackTrace();
    }
    Random rnd = new Random();
    int x = 10 + rnd.nextInt(5000);
    rd.processPortNumber = (short) x; // exclude numbers in ports list
    lsd = new LinkStateDatabase(rd);
    System.out.print(rd.toString());
    try {
        Thread server = new Thread(this.comm.new Server(rd.processPortNumber, this));
        server.start();
    } catch (Exception e) {
        e.printStackTrace();
    }
    rd.status = RouterStatus.INIT;
    System.out.println(this.lsd.toString()); // remove

  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {


  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {
	  if (portNumber < 0 || portNumber >= ports.length) 
		  System.out.println("Port number out of range.");
	  else if (ports[portNumber] == null) 
		  System.out.println("No link on desired port to disconnect.");
	  else {
		  Link link = ports[portNumber]; //get link data
		  portIdx--;
		  ports[portNumber] = null; //remove link data from port
		  RouterDescription r2 = link.router2;
		  //notify r2 of link to disconnect
          SOSPFPacket message = new SOSPFPacket(rd.getProcessIPAddress(), rd.getProcessPortNumber(), rd.getSimulatedIPAddress(), r2.simulatedIPAddress, (short) 4, rd.getSimulatedIPAddress(), r2.simulatedIPAddress);
          
          try {
              CommunicationLayer.client(message, r2.processIPAddress, r2.processPortNumber, r2.simulatedIPAddress, 1);
          } catch (Exception e) {
        	  System.out.println("Error connecting to remote router. See error below:");
              e.printStackTrace();
          }
          
		  //do lsa update somewhere
	  }
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP) {
      
      if (this.portIdx < ports.length - 1) {
          SOSPFPacket message = new SOSPFPacket(rd.getProcessIPAddress(), rd.getProcessPortNumber(), rd.getSimulatedIPAddress(), simulatedIP, (short) 0, rd.getSimulatedIPAddress(), simulatedIP);
         
          try {
              CommunicationLayer.client(message, processIP, processPort, simulatedIP, 1);
              System.out.println("Your attach request has been accepted;");
          } catch (Exception e) {
        	  System.out.println("Error connecting to remote router. See error below:");
              e.printStackTrace();
          }
      } else {
          System.out.println("Routers ports are full");
      }
  }


  /**
   * process request from the remote router. 
   * For example: when router2 tries to attach router1. Router1 can decide whether it will accept this request. 
   * The intuition is that if router2 is an unknown/anomaly router, it is always safe to reject the attached request from router2.
   */
  private void requestHandler() {
	  //for bonus points (see ed), can implement this so that a router can reject an untrustworthy attach attempt (i assume untrustworthy would be defined by us)
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
	  //change router status as per assignment doc describes
	  for (Link link : ports) {
		  if (link != null && link.router2.status == RouterStatus.ATTACHED) { //only attempt to shake hands with attached routers, no need for those already on TwoWay connections
			  link.router2.status = RouterStatus.INIT;
			  SOSPFPacket message = new SOSPFPacket(rd.getProcessIPAddress(), rd.getProcessPortNumber(), rd.getSimulatedIPAddress(), link.router2.simulatedIPAddress, (short) 0, rd.getSimulatedIPAddress(), link.router2.simulatedIPAddress);
			  try {
				  CommunicationLayer.client(message, link.router2.processIPAddress, link.router2.processPortNumber, link.router2.simulatedIPAddress, 1);
                  try {
                      Thread.sleep(1000); // remove
                  } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                  }
                  System.out.println(this.lsd.toString()); // remove
			  } catch (IOException e) {
				  // TODO Auto-generated catch block
				  e.printStackTrace();
			  }
		  }
	  }
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP) {
	  //if i understand correctly the other router must already be started, but idk, will work on it later today
	  processAttach(processIP, processPort, simulatedIP);
	  try {
		Thread.sleep(100);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  processStart(); //should trigger database sync
  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
	  //might be useful to have this return a list of neighbours instead of void, and leave the outputting/handling to the caller
	  //prints all relevant neighbor info instead of just their ips
	  for (Link link : ports) {
		  if (link != null && link.router2.status == RouterStatus.TWO_WAY) {
			  System.out.println();
			  System.out.print(link.router2.toString());
		  }
	  }
	  System.out.println();
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {
	  //get all neighbours
	  //call disconnect on all neighbours, update database(s)
	  for (short i = 0; i < ports.length; i++) {
		  if (ports[i] != null)
			  processDisconnect(i); //pretty sure its using the simulated 4 ports id and not the actual port id?
	  }
	  //quitting the terminal is handled by breaking from the terminal loop
  }

    public LSA newLSA() { // move to LSA w/ rd
        LSA lsa = new LSA();
        int num = lsd._store.get(rd.simulatedIPAddress).lsaSeqNumber;
        lsa.lsaSeqNumber = (num == Integer.MIN_VALUE) ? 0 : num + 1;

        lsa.linkStateID = rd.simulatedIPAddress;
        lsa.links = new LinkedList<LinkDescription>();

        int i = 0;
        while (i < ports.length) {
            if (ports[i] != null && ports[i].router2.status != null) {
                System.out.println("sim IP: "+this.rd.simulatedIPAddress); // remove
                System.out.println("ports[i]: "+ports[i]);
                System.out.println("ports[i].router2.status: "+ports[i].router2.status);
                LinkDescription ld = new LinkDescription(ports[i].router2.simulatedIPAddress,
                        ports[i].router2.processPortNumber);
                System.out.println("hereA");
                lsa.links.add(ld);
                System.out.println("hereB");
            }
            i++;
            System.out.println("hereC");
        }
        System.out.println("hereD");
        //lsd._store.put(rd.simulatedIPAddress, lsa);
        lsd.add(rd.simulatedIPAddress, lsa);
        System.out.println("end of lsa, returning..");
        return lsa;
    }

    public void broadcastLSU(LSA lsaToUpdate) {
        System.out.println("here1");
        Vector<LSA> broadcastList = new Vector<LSA>();
        int index = 0;
        while (index < ports.length) {
            System.out.println("here2");
            if (ports[index] != null) {
                try {
                    System.out.println("here3");
                    SOSPFPacket message = new SOSPFPacket(
                            rd.getProcessIPAddress(),
                            rd.getProcessPortNumber(),
                            rd.getSimulatedIPAddress(),
                            ports[index].router2.simulatedIPAddress,
                            (short) 1,
                            rd.getSimulatedIPAddress(),
                            ports[index].router2.simulatedIPAddress
                    );
                    // Add current LSA to the broadcast list
                    System.out.println("here4");

                    broadcastList.add(lsaToUpdate);
                    message.lsaArray = broadcastList;
                    CommunicationLayer.client(
                            message,
                            ports[index].router2.processIPAddress,
                            ports[index].router2.processPortNumber,
                            ports[index].router2.simulatedIPAddress,
                            1
                    );
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }
            index++; // Move to the next port
        }
    }

    public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      
      BufferedReader br = new BufferedReader(isReader);
      while (true) {
          System.out.print(">> ");
          String command = br.readLine();
    	  
        if (command.startsWith("detect")) {
          String[] cmdLine = command.split(" ");
          if (cmdLine.length != 2) {
        	  System.out.println("Incorrect number of arguments for detect.");
        	  continue;
          }
          processDetect(cmdLine[1]);
        
        } else if (command.startsWith("disconnect")) {
          String[] cmdLine = command.split(" ");
          if (cmdLine.length != 2) {
        	  System.out.println("Incorrect number of arguments for disconnect.");
        	  continue;
          }
          processDisconnect(Short.parseShort(cmdLine[1]));
        
        } else if (command.startsWith("quit")) {
          processQuit();
          break;
        
        } else if (command.startsWith("attach")) {
          String[] cmdLine = command.split(" ");
          if (cmdLine.length != 4) {
        	  System.out.println("Incorrect number of arguments for attach.");
        	  continue;
          }
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3] );
        
        } else if (command.equals("start")) {
          processStart();
        
        } else if (command.startsWith("connect")) {
          String[] cmdLine = command.split(" ");
          if (cmdLine.length != 4) {
        	  System.out.println("Incorrect number of arguments for connect.");
        	  continue;
          }
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3]);
        
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        
        } else if (command.equals("print lsd")) {
            System.out.println(this.lsd.toString()); // remove

        } else {
          //invalid command
        	System.out.println("Invalid command, try again.");
        }
      }
      isReader.close();
      br.close();
      System.exit(0);
    
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
