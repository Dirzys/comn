import java.io.*;
import java.net.*;
import java.util.HashMap;

/**
 * Lukas Dirzys s1119520
 * 
 * Class extends Receiver3
 */

public class Receiver4 extends Receiver3 {    
	
	public static int window;
	//Data structure for keeping data with their sequence number
	protected static HashMap<Integer, byte[]> map = new HashMap<Integer, byte[]>();
	
	public static void main(String args[]) throws Exception    { 
		//Get port from the inputs
		int port = Integer.parseInt(args[0]);
		//Get filename where to save the file
        fileOutputStream = new FileOutputStream(args[1], true);
        //Get window size 
      	window = Integer.parseInt(args[2]);
		//Create receiver socket
		receiver = new DatagramSocket(port);
		
		//Start receiving
		receivePacket(FIRST_SEQ_NR);
		
		fileOutputStream.close();
		receiver.close(); 
	}
	
	/**
	 * Receive packets starting from the one with the given
	 * sequence number base and continue receiving until
	 * the last packet is received.
	 * @param expected first sequence number (initial base)
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static void receivePacket(int base) throws IOException, InterruptedException {
		while (true) {
			//Receive packet from sender
			byte[] buffer = new byte[PACKET_SIZE];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length); //Wait for packet to arrive
			receiver.receive(packet);

			//Extract received sequence number
			byte[] receivedSeqNr = extractSeqNr(buffer);
			int intReceivedSeqNr = byteArrayToInt(receivedSeqNr);

			if ((base - window <= intReceivedSeqNr) && (intReceivedSeqNr <= base + window - 1)) {
				if (base <= intReceivedSeqNr) {
					//Received in-order packet
					if (intReceivedSeqNr == base) {
						//Deliver packet
						deliverData(buffer);
						System.out.println("Received " + intReceivedSeqNr + " packet as expected in order");
						//Deliver buffered also
						base += 1;
						while (true) {
							if (map.containsKey(base)) {
								deliverData(map.get(base));
								System.out.println("Delivering packet " + base);
								base += 1;
							} else {
								break;
							}
						}
					} else { //Received out-order packet
						//Keep it in the buffer
						if (!map.containsKey(intReceivedSeqNr)) {
							map.put(intReceivedSeqNr, buffer);
						}
						System.out.println("Received " + intReceivedSeqNr + " packet out order, expected " + base);
					}
				} else {
					System.out.println("Received " + intReceivedSeqNr + " packet and it was already acknowledged before");
				}
				
				//Send acknowledgement back
				DatagramPacket sendACK = new DatagramPacket(receivedSeqNr, receivedSeqNr.length, packet.getAddress(), packet.getPort());
				receiver.send(sendACK);
				//Check if last packet arrived
				if (isLast(sendACK, base)) {
					return;
				}
			} else {
				System.out.println("Received " + intReceivedSeqNr + " packet, but it is not within the window");
			}
			//Continue receiving packets
		}
	}
	
	/**
	 * Function to test if the packet given
	 * is the last one
	 * @param packet
	 * @return true if last, false otherwise
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public static boolean isLast(DatagramPacket packet, int base) throws IOException, InterruptedException {
		if (last) {
			//Send last n ACK where n = window size
			//for 3 times and sleep for 10ms after each send
			//to make sure that none of them will be lost.
			//This is done to avoid cases when receiver exists
			//but some ACK sent to sender was lost and sender
			//never ends his job just, because it tries to reach
			//receiver again.
			for (int i = base - window; i < base; i++) {
				byte[] byteI =  intToByteArray(i);
				DatagramPacket sendACK = new DatagramPacket(byteI, byteI.length, packet.getAddress(), packet.getPort());
				for (int j = 0; j < 3; j++) {
					receiver.send(sendACK);
					Thread.sleep(10);
				}
			}
			return true;
		} else {
			return false;
		}
	}
}