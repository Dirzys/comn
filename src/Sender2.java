import java.io.*;
import java.net.*;

/**
 * Lukas Dirzys s1119520
 * 
 * Class extends Sender1
 */

public class Sender2 extends Sender1 {
	
	public static final int ACK_SIZE = 2; 
	public static final int SMALL_TIMEOUT = 5; //Time to wait for ACK to arrive and check if still need to wait more
	public static final int FIRST_SEQ_NR = 1; 
	protected static InetAddress host;
	protected static int port;
	protected static int timeout;
	protected static DatagramSocket sender;
	protected static long time = 0;
	protected static int retransmissions = 0;
	protected static int lastPacket;
	
	public static void main(String [] args) throws IOException {
		//Get host from the inputs
		host = InetAddress.getByName(args[0]);
		//Get port from the inputs
		port = Integer.parseInt(args[1]);
		//Load the file
		File file = new File(args[2]);
		//We need to send as much data as there are in the file
		dataToSend = (int) file.length();
		//Find how many packets we will need to send
		int totalPackets = (int) Math.ceil(dataToSend / (MAX_PAYLOAD * 1.0)); 
		lastPacket = totalPackets + FIRST_SEQ_NR - 1;
		fileInputStream = new FileInputStream(file);
		//Get retry timeout from the input
		timeout = Integer.parseInt(args[3]);
		
		//Create sender socket
		sender = new DatagramSocket();
		sender.setSoTimeout(SMALL_TIMEOUT);
		
		//Start timer for finding average throughput
		long startTime = System.currentTimeMillis();
		
		//Send packets
		send(FIRST_SEQ_NR);
		
		//Find the average throughput
		long fullTime = System.currentTimeMillis() - startTime;
		sender.close();
		System.out.println("Number of total retransmissions: " + retransmissions);
		System.out.println("Average throughput " + (file.length()/1024.0) / (fullTime / 1000.0));
	}
	
	/**
	 * Send packets while there are some more data to
	 * be sent or ACK's to be received.
	 * @param seqNr - sequence number 
	 * @throws IOException
	 */
	public static void send(int seqNr) throws IOException {
		//Send the first packet
		DatagramPacket packet = sendPacket(seqNr);
		//Create ACK packet
		byte[] ack = new byte[ACK_SIZE];
		DatagramPacket getack = new DatagramPacket(ack, ack.length);
		//While we have packets to be send
		//or ACK's to be received
		while(true) {
			//First try to receive ACK during the next SMALL_TIMEOUT ms
			try {
				System.out.println("Waiting for ACK " + seqNr);
				sender.receive(getack);
			} catch (SocketTimeoutException e) {
				//If we haven't received anything
				//and we should no longer wait for
				//ACK to arrive (timeout) - resend the packet
				//and restart the timer
				if (!waitTimeout()) {
					System.out.println("Retrying to send packet " + seqNr);
					retransmissions += 1;
					sender.send(packet);
					startTimer();
				}
				//Continue waiting
				continue;
			}
			//If received ACK and it's the one
			//as expected - send next packet
			if (byteArrayToInt(ack) == seqNr) { 
				if (dataToSend > 0) {
					seqNr += 1;
					packet = sendPacket(seqNr);
				} else {
					//If there is no more data to be send and last acknowledgement
					//received - exit
					if (seqNr == lastPacket) {
						return;
					}
				}
			}
			continue;
		}
	}
	
	/**
	 * Create a packet with given sequence number seqNr,
	 * send it and start the timer
	 * @param sequence number
	 * @return DatagramPacket packet
	 * @throws IOException
	 */
	public static DatagramPacket sendPacket(int seqNr) throws IOException {
		//Make packet with sequence number seqNr
		byte[] data = makePacket(seqNr);
		//Send packet
		DatagramPacket packet = new DatagramPacket(data, data.length, host, port);
		System.out.println("Sending packet " + seqNr);
		sender.send(packet);
		//Start timer
		startTimer();
		return packet;
	}
	
	/**
	 * Start timer by saving the value
	 * of current time
	 */
	public static void startTimer() {
		time = System.currentTimeMillis();
	}
	
	/**
	 * Returns whether we should wait a bit more
	 * @return true if wait, false otherwise
	 */
	public static boolean waitTimeout() {
		return System.currentTimeMillis() < time + timeout;
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
