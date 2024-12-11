import java.net.*;
import java.io.*;
import java.util.*;

public class DAS {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("specify port and number");
            return;
        }

        int port;
        int number;
        try {
            port = Integer.parseInt(args[0]);
            number = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.out.println("Port and number must be integers.");
            return;
        }

        try {
            DatagramSocket socket = new DatagramSocket(port);
            System.out.println("Master mode running on port " + port);
            runMaster(socket, number);
        } catch (SocketException e) {
            System.out.println("Slave mode running.");
            runSlave(port, number);
        }
    }

    private static void runMaster(DatagramSocket socket, int number) {
        List<Integer> numbers = new ArrayList<>();
        numbers.add(number);
        System.out.println("Master received initial number: " + number);
        byte[] buffer = new byte[1024];

        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                int receivedNumber = Integer.parseInt(received);
                System.out.println("Received: " + receivedNumber);

                if (receivedNumber == 0) {
                    int sum = 0;
                    for (int num : numbers) {
                        sum += num;
                    }
                    int average = sum / numbers.size();
                    System.out.println("Numbers received: " + numbers);
                    System.out.println("Broadcast average: " + average);
                    broadcast(socket, packet.getPort(), String.valueOf(average));
                } else if (receivedNumber == -1) {
                    System.out.println("Terminating");
                    broadcast(socket, packet.getPort(), "-1");
                    break;
                } else {
                    numbers.add(receivedNumber);
                }
            } catch (IOException | NumberFormatException e) {
                System.out.println("Error processing packet: " + e.getMessage());
            }
        }

        socket.close();
    }

    private static void runSlave(int port, int number) {
        try {
            DatagramSocket socket = new DatagramSocket();
            byte[] data = String.valueOf(number).getBytes();
            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

            socket.send(packet);
            System.out.println("Sent: " + number);
        } catch (IOException e) {
            System.out.println("Error in Slave mode: " + e.getMessage());
        }
    }

    private static void broadcast(DatagramSocket socket, int port, String message) {
        try {
            InetAddress localHost = InetAddress.getLocalHost();

            byte[] ipBytes = localHost.getAddress();
            byte[] subnetMask = {(byte) 255, (byte) 255, (byte) 255, 0};

            byte[] broadcastBytes = new byte[4];
            for (int i = 0; i < ipBytes.length; i++) {
                broadcastBytes[i] = (byte) (ipBytes[i] | ~subnetMask[i]);
            }

            InetAddress broadcastAddress = InetAddress.getByAddress(broadcastBytes);

            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, port);
            socket.setBroadcast(true);
            socket.send(packet);


        } catch (IOException e) {
            System.out.println("Error broadcasting message: " + e.getMessage());
        }
    }
}