import java.io.*;
import java.net.*;

/**
 * Lukas Dirzys s1119520
 * 
 * Class extends Receiver2
 */

public class Receiver3 extends Receiver2 {    
	
	public static void main(String args[]) throws Exception    { 
		//Get port from the inputs
		int port = Integer.parseInt(args[0]);
		//Get filename where to save the file
        fileOutputStream = new FileOutputStream(args[1], true);
		//Create receiver socket
		receiver = new DatagramSocket(port);
		
		//Start receiving
		receivePacket(FIRST_SEQ_NR);
		
		fileOutputStream.close();
		receiver.close(); 
	}
	
	/**
	 * Receive packets starting from the one with the given
	 * sequence number expectedSeqNr and continue receiving until
	 * the last packet is received.
	 * @param expected sequence number
	 * @throws IOException
	 */
	public static void receivePacket(int expectedSeqNr) throws IOException, InterruptedException {
		//Create initial acknowledgement packet
		byte[] initialSeqNr = intToByteArray(expectedSeqNr-1);
		DatagramPacket currentPacket = new DatagramPacket(initialSeqNr, initialSeqNr.length);
		
		while (true) {
			//Receive packet from sender
			byte[] buffer = new byte[PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length); //Wait for packet to arrive
			receiver.receive(packet);

			//Set address and port for the initial ACK packet
			if (expectedSeqNr == FIRST_SEQ_NR) {
				currentPacket.setAddress(packet.getAddress());
				currentPacket.setPort(packet.getPort());
			}

			//Extract received sequence number
			byte[] receivedSeqNr = extractSeqNr(buffer);

			//If received packet is the one as expected - extract and deliver the data
			if (byteArrayToInt(receivedSeqNr) == expectedSeqNr) {
				deliverData(buffer);
				currentPacket = new DatagramPacket(receivedSeqNr, receivedSeqNr.length, packet.getAddress(), packet.getPort());
				expectedSeqNr += 1;
				System.out.println("Received " + byteArrayToInt(receivedSeqNr) + " packet as expected");
			} else {
				System.out.println("Received " + byteArrayToInt(receivedSeqNr) + " packet, but expected " + expectedSeqNr);
			}
			//Send acknowledgement back
			receiver.send(currentPacket);
			//Check if last packet arrived
			if (isLast(currentPacket)) {
				return;
			}
			//Continue receiving packets
		}
	}
	
	/**
	 * Convert integer to byte array of 2 bytes
	 * @param a
	 * @return byte[]
	 */
	public static byte[] intToByteArray(int a) {
		byte[] data = new byte[2]; 

		data[0] = (byte)(a & 0xFF);
		data[1] = (byte)((a >> 8) & 0xFF);
		
		return data;
	}
}