import java.net.*;
import java.io.*;
import java.util.*;

public class DAS {
    private static final int BUFFER_SIZE = 1024;
    private static List<Integer> receivedNumbers = new ArrayList<>();
    private static int masterNumber;

    public static void main(String[] args) {
        System.out.println("Arguments received: " + Arrays.toString(args));

        if (args.length != 2) {
            System.out.println("Usage: java DAS <port> <number>");
            return;
        }

        int port;
        int number;
        try {
            port = Integer.parseInt(args[0]);
            number = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port or number. Both should be integers.");
            return;
        }

        try {
            // Try to open the port (Master Mode)
            DatagramSocket socket = new DatagramSocket(port);
            System.out.println("Running in Master mode on port " + port);
            runAsMaster(socket, number, port);
        } catch (SocketException e) {
            // If the port is already in use, run as Slave
            System.out.println("Running in Slave mode.");
            runAsSlave(port, number);
        }

    }

    private static void runAsMaster(DatagramSocket socket, int number, int port) {
        masterNumber = number;
        receivedNumbers.add(number); // Store the initial number

        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String receivedData = new String(packet.getData(), 0, packet.getLength());
                int receivedNumber;
                try {
                    receivedNumber = Integer.parseInt(receivedData);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid data received: " + receivedData);
                    continue;
                }

                System.out.println("Received: " + receivedNumber);

                if (receivedNumber == 0) {
                    // Calculate and broadcast average
                    int average = calculateAverage();
                    System.out.println("Average: " + average);
                    broadcastMessage(socket, port, String.valueOf(average));
                } else if (receivedNumber == -1) {
                    // Broadcast -1 and terminate
                    broadcastMessage(socket, port, "-1");
                    System.out.println("Shutting down Master.");
                    socket.close();
                    break;
                } else {
                    // Store the number
                    receivedNumbers.add(receivedNumber);
                }
            }
        } catch (IOException e) {
            System.out.println("Error in Master mode: " + e.getMessage());
        }
    }

    private static void runAsSlave(int port, int number) {
        try (DatagramSocket socket = new DatagramSocket()) {
            String message = String.valueOf(number);
            byte[] data = message.getBytes();

            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);

            socket.send(packet);
            System.out.println("Sent: " + message);
        } catch (IOException e) {
            System.out.println("Error in Slave mode: " + e.getMessage());
        }
    }

    private static int calculateAverage() {
        int sum = 0;
        int count = 0;
        for (int num : receivedNumbers) {
            if (num != 0) {
                sum += num;
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }

    private static void broadcastMessage(DatagramSocket socket, int port, String message) {
        try {
            byte[] data = message.getBytes();
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(data, data.length, broadcastAddress, port);

            socket.setBroadcast(true);
            socket.send(packet);
            System.out.println("Broadcasted: " + message);
        } catch (IOException e) {
            System.out.println("Error broadcasting message: " + e.getMessage());
        }
    }
}
