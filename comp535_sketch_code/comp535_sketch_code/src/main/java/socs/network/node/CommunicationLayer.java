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

    public void server(short port) throws IOException
    {
        // server is continuously listening on port for client requests
        ServerSocket ss = new ServerSocket(port);
        while (true)
        {
            Socket s = null;
            try
            {
                s = ss.accept();

                ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
                // assign new thread for this client
                Thread t = new ClientHandler(s, ois, oos);
                t.start();
            }
            catch (Exception e){
                s.close();
                e.printStackTrace();
            }
        }
    }
    public static void client(SOSPFPacket message, String processIP, short processPort, String simulatedIP) throws IOException
    {
        try
        {
            InetAddress ip = InetAddress.getByName(processIP);
            Socket s = new Socket(ip, processPort);

            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());

            while (true)
            {

                oos.writeObject(message);


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
                        case 0 :
                            /*SOSPFPacket toreturn = new SOSPFPacket(me.getProcessIPAddress(),
                                    me.getProcessPortNumber(), me.getSimulatedIPAddress(), received.simulatedIP,
                                2, received.routerId, received.getSimulatedIPAddress());
                            oos.writeObject(toreturn);
                            */
                            System.out.println("Received Message 0");

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
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            try
            {
                this.ois.close();
                this.oos.close();

            }catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
