import java.io.*;
import java.net.*;

/**
 * Lukas Dirzys s1119520
 * 
 */

public class Receiver1 {    
	
	public static final int MAX_PAYLOAD = 1024;
	public static final int HEADER = 3;
	public static final int PACKET_SIZE = MAX_PAYLOAD + HEADER;
	protected static FileOutputStream fileOutputStream;
	protected static DatagramSocket receiver;
	
	public static void main(String args[]) throws Exception    { 
		//Get port from the inputs
		int port = Integer.parseInt(args[0]);
		//Get filename where to save the file
        fileOutputStream = new FileOutputStream(args[1], true);
		//Create receiver socket
		receiver = new DatagramSocket(port);
		
		//Start receiving
		startReceiving(port);
		
		fileOutputStream.close();
		receiver.close(); 
	}
	
	/**
	 * Wait for a new packet to arrive while
	 * the last packet has not arrived yet.
	 * Deliver received data by writing it
	 * to the file.
	 * 
	 * @param port
	 * @throws IOException
	 */
	public static void startReceiving(int port) throws IOException {
		//Flag to know if the last packet arrived and we can exit
		boolean last = false;
		while (!last) {
			byte[] buffer = new byte[PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			//Wait for packet to arrive
			receiver.receive(packet);
			byte[] seqNr = new byte[2];
			System.arraycopy(buffer, 0, seqNr, 0, 2);
			System.out.println("Received " + byteArrayToInt(seqNr) + " packet");
			//If the 3rd byte in the buffer is 1, that means
			//we just got the last packet.
			if (buffer[2] == 1) {
				last = true;
			}
			//Write data to file
			fileOutputStream.write(buffer, HEADER, packet.getLength()-HEADER);
		}
	}
	
	/**
	 * Convert byte array of 2 bytes into integer
	 * @param data
	 * @return int
	 */
	public static int byteArrayToInt(byte[] data) {
		int high = data[1] >= 0 ? data[1] : 256 + data[1];
		int low = data[0] >= 0 ? data[0] : 256 + data[0];

		int res = low | (high << 8);
		
		return res;
	}
}