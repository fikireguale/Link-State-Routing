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
        try
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
                System.out.println("Sent "+message.toString()+" to "+simulatedIP);

                if(message.sospfType == (short) 3)
                {
                    s.close();
                    break;
                }

            }
            ois.close();
            oos.close();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    class ClientHandler extends Thread
    {
        final ObjectInputStream ois;
        final ObjectOutputStream oos;
        final Socket s;

        public ClientHandler(Socket s, ObjectInputStream ois, ObjectOutputStream oos)
        {
            this.s = s;
            this.ois = ois;
            this.oos = oos;
        }

        @Override
        public void run()
        {
            while (true)
            {

                try {
                    SOSPFPacket received = null;
                    received = (SOSPFPacket) ois.readObject();

                    if(received.equals("Exit")) {
                        this.s.close();
                        break;
                    }
                    switch (received.sospfType) {
                        case 0:
                            /*SOSPFPacket toreturn = new SOSPFPacket(me.getProcessIPAddress(),
                                    me.getProcessPortNumber(), me.getSimulatedIPAddress(), received.simulatedIP,
                                2, received.routerId, received.getSimulatedIPAddress());
                            oos.writeObject(toreturn);
                            */
                            System.out.print("\n Received "+received.toString()+"\n>> ");
                            break;
                        case 1:
                            System.out.println("Received Message 1");
                            break;
                        case 2:
                            System.out.println("Received Message 2");
                            break;
                        default:
                            //oos.writeUTF("Invalid input");
                            break;
                    }
                } catch (EOFException e) { // Supposed to happen
                    break;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            try
            {
                this.s.close();
                this.ois.close();
                this.oos.close();

            }catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
    class Server implements Runnable{
        short port;

        public Server(short aPort) {
            this.port = aPort;
        }

        public void run()
        {
            Socket s = null;
            try {
                // server is continuously listening on port for client requests
                ServerSocket ss = new ServerSocket(this.port);
                while (true) {
                    s = ss.accept();
                    ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                    // assign new thread for this client
                    Thread t = new ClientHandler(s, ois, oos);
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
