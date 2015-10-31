import java.io.*;
import java.net.*;
import java.util.HashMap;

/**
 * Lukas Dirzys s1119520
 * 
 * Class extends Sender2
 */

public class Sender3 extends Sender2 {

	protected static int window = 0;
	//Data structure for keeping packets with their sequence number
	protected static HashMap<Integer, DatagramPacket> map = new HashMap<Integer, DatagramPacket>();
	
	public static void main(String [] args) throws IOException {
		//Get host from the inputs
		host = InetAddress.getByName(args[0]);
		//Get port from the inputs
		port = Integer.parseInt(args[1]);
		//Load the file
		File file = new File(args[2]);
		//we need to send as much data as there are in the file
		dataToSend = (int) file.length();
		//Find how many packets we will need to send
		int totalPackets = (int) Math.ceil(dataToSend / (MAX_PAYLOAD * 1.0)); 
		lastPacket = totalPackets + FIRST_SEQ_NR - 1;
		fileInputStream = new FileInputStream(file);
		//Get retry timeout from the input
		timeout = Integer.parseInt(args[3]);
		//Get window size 
		window = Integer.parseInt(args[4]);
		//Create sender socket
		sender = new DatagramSocket();
		sender.setSoTimeout(SMALL_TIMEOUT);
		
		//Start timer for finding average throughput
		long startTime = System.currentTimeMillis();
		
		//Send the first packet
		send(FIRST_SEQ_NR, FIRST_SEQ_NR);
		
		//Find the average throughput
		long fullTime = System.currentTimeMillis() - startTime;
		sender.close();
		System.out.println("Number of total retransmissions: " + retransmissions);
		System.out.println("Average throughput " + (file.length()/1024.0) / (fullTime / 1000.0));
	}
	
	/**
	 * Send packets while there are some more data to
	 * be sent or ACK's to be received.
	 * @param base - the sequence number of the oldest
	 * 			unacknowledged packet
	 * @param nextSeqNr - sequence number of the next 
	 * 			packet to be send
	 * @throws IOException
	 */
	public static void send(int base, int nextSeqNr) throws IOException {
		//Send the first packet
		nextSeqNr = sendPacket(base, nextSeqNr);
		//Create ACK packet
		byte[] ack = new byte[ACK_SIZE];
		DatagramPacket getack = new DatagramPacket(ack, ack.length);
		//While we have packets to be send
		//or ACK's to be received
		while (true) {
			//First try to receive ACK during the next SMALL_TIMEOUT ms
			try {
				sender.receive(getack);
				//If received an ACK change the base
				base = byteArrayToInt(ack) + 1;
				System.out.println("Received ACK " + (base-1));
				//Restart the timer if base is not the same as
				//the next sequence number
				if (base != nextSeqNr) {
					startTimer();
				}
			} catch (SocketTimeoutException e) {
				//If we haven't received anything
				//and we should no longer wait for
				//ACK to arrive (timeout) - re-send all packets
				//within base and next sequence number
				//and restart the timer
				if (!waitTimeout()) {
					System.out.println("Retrying to send packets " + base + " - " + (nextSeqNr-1));
					startTimer();
					for (int i = base; i < nextSeqNr; i++) {
						retransmissions += 1;
						sender.send(findPacket(i));
					}
				}
			}
			//If there is more data to be send - send it,
			//otherwise if last acknowledgement already received - exit
			if (dataToSend > 0) {
				//Send packet
				nextSeqNr = sendPacket(base, nextSeqNr);
			} else {
				if (base == lastPacket + 1) {
					return;
				} 
			}
		}
	}
	
	/**
	 * Create a packet with given next sequence number nextSeqNr and
	 * base, send it and start the timer
	 * @param base - the sequence number of the oldest
	 * 			unacknowledged packet
	 * @param next sequence number
	 * @return int nextSeqNr
	 * @throws IOException
	 */
	public static int sendPacket(int base, int nextSeqNr) throws IOException {
		//If packet we want to send is within our window - send it
		//and increase next sequence number by 1, so that next time
		//we send next packet
		if (nextSeqNr < base + window) {
			DatagramPacket packet = findPacket(nextSeqNr);
			System.out.println("Sending packet " + nextSeqNr);
			sender.send(packet);
			if (base == nextSeqNr) {
				startTimer();
			}
			nextSeqNr += 1;
		}
		return nextSeqNr;
	}
	
	/**
	 * Find packet with given sequence number by searching for it
	 * in the hash map. If packet is not yet in map - create it
	 * and put it into the map.
	 * @param sequence number
	 * @return DatagramPacket packet
	 * @throws IOException
	 */
	public static DatagramPacket findPacket(int seqNr) throws IOException {
		if (map.containsKey(seqNr)) {
			return map.get(seqNr);
		} else {
			byte[] data = makePacket(seqNr);
			DatagramPacket packet = new DatagramPacket(data, data.length, host, port);
			map.put(seqNr, packet);
			return packet;
		}
	}
}
