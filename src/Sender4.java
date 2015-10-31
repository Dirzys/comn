import java.io.*;
import java.net.*;
import java.util.HashMap;

/**
 * Lukas Dirzys s1119520
 * 
 * Class extends Sender3
 */

public class Sender4 extends Sender3 {
	
	protected static long[] time;
	
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
		//Initialise timeouts array
		time = new long[(int) Math.ceil(dataToSend/(MAX_PAYLOAD*1.0))+1];
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
				//If received acknowledgement,
				//mark packet with the received ACK
				//as received by stopping the timer for
				//this packet
				int intAck = byteArrayToInt(ack);
				System.out.println("Received ACK " + intAck);
				stopTimer(intAck);
				//If received packet is the smallest
				//unacknowledged packet, move window to
				//next unACKed sequence number
				if (base == intAck) {
					base = moveBaseTo(base + 1, nextSeqNr);
				}
			} catch (SocketTimeoutException e) {
				//If we haven't received anything
				//go through all packets between
				//the base and the one with the current
				//sequence number and for each of them
				//check if we should wait anymore for their
				//ACK's to arrive. If not - restart the timer
				//for the ACK and re-send the packet with 
				//corresponding sequence number
				for (int i = base; i < nextSeqNr; i++) {
					if (!waitTimeout(i)) {
						System.out.println("Retrying to send packet " + i);
						startTimer(i);
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
			//Start the timer for the packet with the sequence number nextSeqNr
			startTimer(nextSeqNr);
			nextSeqNr += 1;
		}
		return nextSeqNr;
	}
	
	/**
	 * Given base and next sequence number, find much to
	 * increase the base, so that base is oldest
	 * unacknowledged sequence number
	 * @param base
	 * @param nextSeqNr
	 * @return new base
	 */
	public static int moveBaseTo(int base, int nextSeqNr) {
		int currentBase = base;
		for (int i = base; i < nextSeqNr; i++) {
			if (time[i] == 0) {
				currentBase += 1;
			} else {
				break;
			}
		}
		return currentBase;
	}
	
	/**
	 * Begin the timer for packet
	 * with sequence number seqNr
	 * @param sequence number
	 */
	public static void startTimer(int seqNr) {
		time[seqNr] = System.currentTimeMillis();
	}
	
	/**
	 * Returns whether we should wait a bit more
	 * @param ms
	 * @return true if needs to wait, false otherwise
	 */
	public static boolean waitTimeout(int seqNr) {
		return System.currentTimeMillis() < time[seqNr] + timeout;
	}
	
	/**
	 * Stop timer for some packet with
	 * sequence number seqNr
	 * @param seqNr
	 */
	public static void stopTimer(int seqNr) {
		time[seqNr] = 0;
	}
}
