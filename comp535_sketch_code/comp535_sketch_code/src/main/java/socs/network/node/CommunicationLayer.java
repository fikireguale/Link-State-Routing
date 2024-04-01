
import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;
import java.util.Scanner;

public class CommunicationLayer extends Thread

{
    public CommunicationLayer(Link link) {
        link
    }

    public static void server() throws IOException
    {
        // server is continuously listening on port for client requests
        ServerSocket ss = new ServerSocket(port);
        while (true)
        {
            Socket s = null;
            try
            {
                s = ss.accept();
                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                Thread t = new ClientHandler(s, dis, dos);
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

            DataInputStream dis = new DataInputStream(s.getInputStream());
            //DataOutputStream dos = new DataOutputStream(s.getOutputStream());

            //FileOutputStream fos = new FileOutputStream("temp.txt");
            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());

            while (true)
            {
                oos.writeObject(message);
                if(tosend.equals("Exit"))
                {
                    s.close();
                    break;
                }
                String received = dis.readUTF();
                System.out.println(received);
            }
            scn.close();
            dis.close();
            dos.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    class ClientHandler extends Thread
    {
        final DataInputStream dis;
        final DataOutputStream dos;
        final Socket s;

        public ClientHandler(Socket s, DataInputStream dis, DataOutputStream dos)
        {
            this.s = s;
            this.dis = dis;
            this.dos = dos;
        }

        @Override
        public void run()
        {
            String received;
            String toreturn;
            while (true)
            {
                try {
                    received = dis.readUTF();
                    if(received.equals("Exit")) {
                        this.s.close();
                        break;
                    }
                    switch (received) {

                        case "Hello" :
                            toreturn = fordate.format(date);
                            dos.writeUTF(toreturn);
                            break;

                        default:
                            dos.writeUTF("Invalid input");
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try
            {
                this.dis.close();
                this.dos.close();

            }catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
