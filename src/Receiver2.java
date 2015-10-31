import java.io.*;
import java.net.*;

/**
 * Lukas Dirzys s1119520
 * 
 * Class extends Receiver1
 */

public class Receiver2 extends Receiver1 {    
	
	public static final int FIRST_SEQ_NR = Sender2.FIRST_SEQ_NR;
	public static final int ACK_SIZE = Sender2.ACK_SIZE;
	protected static boolean last = false;
	
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
	 * sequence number seqNr and continue receiving until
	 * the last packet is received.
	 * @param sequence number
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static void receivePacket(int seqNr) throws IOException, InterruptedException {
		while (true) {
			//Receive packet from sender
			byte[] buffer = new byte[PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length); //Wait for packet to arrive
			receiver.receive(packet);
			
			//Extract received sequence number
			byte[] receivedSeqNr = extractSeqNr(buffer);
			
			//If received packet is the one as expected - extract and deliver the data
			if (byteArrayToInt(receivedSeqNr) == seqNr) {
				seqNr += 1;
				deliverData(buffer);
				System.out.println("Received " + byteArrayToInt(receivedSeqNr) + " packet as expected");
			} else {
				System.out.println("Received " + byteArrayToInt(receivedSeqNr) + " packet, but expected " + seqNr);
			}
			//Send acknowledgement back
			DatagramPacket sendACK = new DatagramPacket(receivedSeqNr, receivedSeqNr.length, packet.getAddress(), packet.getPort());
			receiver.send(sendACK);
			//Check if last packet arrived
			if (isLast(sendACK)) {
				return;
			}
			//Continue receiving packets
		}
	}
	
	/**
	 * Given packet, extract the sequence number
	 * @param buffer
	 * @param int sequence number
	 * @return sequence number in form byte[]
	 */
	public static byte[] extractSeqNr(byte[] buffer) {
		byte[] receivedSeqNr = new byte[2];
		System.arraycopy(buffer, 0, receivedSeqNr, 0, 2);
		return receivedSeqNr;
	}
	
	/**
	 * Check if that is the last packet
	 * and write data to the file.
	 * @param buffer
	 * @param packet
	 * @throws IOException
	 */
	public static void deliverData(byte[] buffer) throws IOException, InterruptedException {
		if (buffer[2] == 1) {
			last = true;
		}
		//Write data to file
		fileOutputStream.write(buffer, HEADER, buffer.length - HEADER);
	}
	
	/**
	 * Function to test if the packet given
	 * is the last one
	 * @param packet
	 * @return true if last, false otherwise
	 * @throws IOException
	 */
	public static boolean isLast(DatagramPacket packet) throws IOException {
		if (last) {
			//Send the last ACK for 5 times
			//to make sure that it will not be lost
			for (int i = 0; i < 5; i++) {
				receiver.send(packet);
			}
			return true;
		} else {
			return false;
		}
	}
}