package socs.network.node;

import socs.network.util.Configuration;
import socs.network.message.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;
import java.net.InetAddress;
//import java.lang.Threads;


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
        Thread server = new Thread(this.comm.new Server(rd.processPortNumber));
        server.start();
    } catch (Exception e) {
        e.printStackTrace();
    }
    rd.status = RouterStatus.INIT;

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

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort,
                             String simulatedIP) {
      this.portIdx++;
      if (this.portIdx < ports.length) {
          RouterDescription r2 = new RouterDescription(processIP, processPort, simulatedIP, RouterStatus.INIT);
          Link link = new Link(this.rd, r2);
          ports[portIdx] = link;

          SOSPFPacket message = new SOSPFPacket(rd.getProcessIPAddress(), rd.getProcessPortNumber(), rd.getSimulatedIPAddress(), simulatedIP, (short) 0, rd.getSimulatedIPAddress(), r2.getSimulatedIPAddress());
          try {
              this.comm.client(message, processIP, processPort, simulatedIP, 1);
          } catch (Exception e) {
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

  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP) {

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {

  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3] );
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3]);
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else {
          //invalid command
          break;
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
