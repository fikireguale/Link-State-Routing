package socs.network.node;

import socs.network.message.*;
import socs.network.util.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.util.Scanner;

public class CommunicationLayer extends Thread

{
    public CommunicationLayer() { }

    public static void client(SOSPFPacket message, String processIP, short processPort, String simulatedIP, int retries) throws IOException
    {

        InetAddress ip = InetAddress.getByName(processIP);
        Socket s = new Socket(ip, processPort);

        ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
        ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
        int count = 0;
        while (count < retries)
        {
            count++;
            oos.writeObject(message);
            //System.out.println("Sent "+message.toString()+" to "+simulatedIP);

            if(message.sospfType == (short) 3)
            {
                s.close();
                break;
            }

        }
        ois.close();
        oos.close();
    }

    class ClientHandler extends Thread
    {
        final ObjectInputStream ois;
        final ObjectOutputStream oos;
        final Socket s;
        final Router router;

        public ClientHandler(Socket s, ObjectInputStream ois, ObjectOutputStream oos, Router router)
        {
            this.s = s;
            this.ois = ois;
            this.oos = oos;
            this.router = router;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    SOSPFPacket received = null;
                    received = (SOSPFPacket) ois.readObject();

                    if(received.equals("Exit")) {
                        this.s.close();
                        break;
                    }
                    switch (received.sospfType) {
                        case 0:
                            System.out.println("\nreceived HELLO from "+received.srcIP+";");
                        	boolean found = false;
                        	// check router ports
                        	// 	if message sender not listed, try and add them as attached and send message 3
                        	// 	if message sender listed as attached, change to init and resend hello
                        	// 	if message sender listed as init, change to TwoWay
                        	for (int i = 0; i < router.ports.length; i++) {
                        		Link link = router.ports[i];
                        		if (link != null && link.router2.simulatedIPAddress.equals(received.srcIP)) {
                        			found = true;
                        			switch (link.router2.status) {
                        				case INIT: {
                        					link.router2.status = RouterStatus.TWO_WAY;
                                            System.out.println("set "+received.srcIP+" STATE to TWO_WAY;");
                        					SOSPFPacket message = new SOSPFPacket(router.rd.getProcessIPAddress(), router.rd.getProcessPortNumber(), router.rd.getSimulatedIPAddress(), link.router2.simulatedIPAddress, (short) 0, router.rd.getSimulatedIPAddress(), link.router2.simulatedIPAddress);
                        					try {
                        						CommunicationLayer.client(message, link.router2.processIPAddress, link.router2.processPortNumber, link.router2.simulatedIPAddress, 1);
                                                router.broadcastLSU(router.newLSA());
                        					} catch (IOException e) {
                        						// TODO Auto-generated catch block
                        						e.printStackTrace();
                        					}
                        					break;
                        				}
                        				case TWO_WAY: break;
                        				case ATTACHED: {
                        					link.router2.status = RouterStatus.INIT;
                                            System.out.println("set "+received.srcIP+" STATE to INIT;");
                        					SOSPFPacket message = new SOSPFPacket(router.rd.getProcessIPAddress(), router.rd.getProcessPortNumber(), router.rd.getSimulatedIPAddress(), link.router2.simulatedIPAddress, (short) 0, router.rd.getSimulatedIPAddress(), link.router2.simulatedIPAddress);
                        					try {
                        						CommunicationLayer.client(message, link.router2.processIPAddress, link.router2.processPortNumber, link.router2.simulatedIPAddress, 1);
                        					} catch (IOException e) {
                        						// TODO Auto-generated catch block
                        						e.printStackTrace();
                        					}
                        					break;
                        				}
                        			}
                        		}
                        	}
                        	
                        	if (!found) {//we have never received a hello from this router before. find an open space to try and attach them
	                        	for (int i = 0; i < router.ports.length; i++ ) {
	                        		if (router.ports[i] == null) {
	                    				RouterDescription r2 = new RouterDescription(received.srcProcessIP, received.srcProcessPort, received.srcIP, RouterStatus.ATTACHED);
	                    				SOSPFPacket message = new SOSPFPacket(router.rd.getProcessIPAddress(), router.rd.getProcessPortNumber(), router.rd.getSimulatedIPAddress(), received.srcIP, (short) 3, router.rd.getSimulatedIPAddress(), r2.getSimulatedIPAddress());
	                    				CommunicationLayer.client(message, r2.processIPAddress, r2.processPortNumber, r2.simulatedIPAddress, 1);
                                        System.out.println("Auto accepted the attach request;");
	                    				router.ports[i] = new Link(router.rd, r2);
	                          	  		router.portIdx++;
	                          	  		break;
	                    			}
	                        	}
                        	} break;
                        case 1:
                            LSA oldLSA = router.lsd._store.get(received.srcIP);
                            LSA newLSA = received.lsaArray.lastElement();
                            boolean needUpdate = false;
                            if (oldLSA == null) {
                                LSA lsa = router.newLSA();
                                router.broadcastLSU(lsa);
                                needUpdate = true;
                            }
                            if (oldLSA != null && newLSA.lsaSeqNumber > oldLSA.lsaSeqNumber) {
                                needUpdate = true;
                            }
                            if (needUpdate) {
                                int index = getIdx(received.srcIP);
                                LinkDescription ld = getDescr(newLSA.links);
                                if (index != -1 && ld != null) {
                                    LSA curLSA = router.lsd._store.get(router.rd.simulatedIPAddress);
                                    curLSA.links = createLinks();
                                    router.lsd.add(router.rd.simulatedIPAddress, curLSA);
                                    router.broadcastLSU(curLSA);

                                }
                                router.lsd.add(received.srcIP, newLSA);

                                // forward the packet to all neighbors
                                for (int i = 0; i < router.ports.length; i++) {
                                    if (router.ports[i] != null && !router.ports[i].router2.simulatedIPAddress.equals(received.srcIP)) {
                                        // forward LSAUPDATE packet to its neighbors
                                        forward(received);
                                    }
                                }
                            }

                            break;
                        case 2:
                            System.out.println("Received Message 2");
                            break;
                        case 3: //used as a confirmation that an attach request was accepted
                			RouterDescription r2 = new RouterDescription(router.rd.getProcessIPAddress(), received.srcProcessPort, received.srcIP, RouterStatus.ATTACHED);
                			for (int i = 0; i < router.ports.length; i++) {
                				if (router.ports[i] == null) {
                        			router.ports[i] = new Link(router.rd, r2);
                              	  	router.portIdx++;
                              	  	break;
                				}
                			} break;
                        case 4: //used to indicate that the sender is disconnecting the mutual link
                        	for (int i = 0; i < router.ports.length; i++) {
                        		if (router.ports[i] != null && router.ports[i].router2.simulatedIPAddress.equals(received.srcIP)) {
                        			router.ports[i] = null; //remove link to src router
                        			router.portIdx--;
                        			//perform lsa update for this router
                                    router.broadcastLSU(router.newLSA());
                        			break;
                        		}
                        	} break;
                        default:
                            System.out.println("Unknown message received from router " + received.srcIP);
                            break;
                    }
                    System.out.print("\n>> ");
                } catch (EOFException e) { // Supposed to happen
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } 
            try {
                this.s.close();
                this.ois.close();
                this.oos.close();

            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        public int getIdx(String srcIP) {
            for (int i = 0; i < router.ports.length; i++) {
                if (router.ports[i] != null && router.ports[i].router2.simulatedIPAddress.equals(srcIP)) {
                    return i;
                }
            }
            // not found
            return -1;
        }
        public LinkDescription getDescr(LinkedList<LinkDescription> list) {
            for (LinkDescription ld : list) {
                if (ld.linkID.equals(router.rd.simulatedIPAddress)) {
                    return ld;
                }
            }
            return null;
        }
        public LinkedList<LinkDescription> createLinks() {
            LinkedList<LinkDescription> newLinks = new LinkedList<LinkDescription>();
            for (int i = 0; i < router.ports.length; i++) {
                if (router.ports[i] != null && router.ports[i].router2.status != null) { // valid connection
                    LinkDescription ld = new LinkDescription(router.ports[i].router2.simulatedIPAddress,
                            router.ports[i].router2.processPortNumber);
                    newLinks.add(ld);
                }
            }
            return newLinks;
        }
        @SuppressWarnings("resource")
        public void forward(SOSPFPacket message) throws IOException {
            for (int i = 0; i < router.ports.length; i++) {
                if (router.ports[i] != null) {
                    CommunicationLayer.client(message, router.ports[i].router2.processIPAddress, router.ports[i].router2.processPortNumber, router.ports[i].router2.simulatedIPAddress, 1);
                }
            }
        }
    }


    
    class Server implements Runnable{
        short port;
        Router router;

        public Server(short aPort, Router router) {
            this.port = aPort;
            this.router = router;
        }

        public void run() {
            Socket s = null;
            try {
                // server is continuously listening on port for client requests
                ServerSocket ss = new ServerSocket(this.port);
                while (true) {
                    s = ss.accept();
                    ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                    // assign new thread for this client
                    Thread t = new ClientHandler(s, ois, oos, router);
                    t.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
