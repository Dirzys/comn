import java.io.*;
import java.net.*;

/**
 * Lukas Dirzys s1119520
 * 
 */

public class Sender1 {
	
	public static final int MAX_PAYLOAD = 1024;
	public static final int HEADER = 3;
	public static final int PACKET_SIZE = MAX_PAYLOAD + HEADER;
	public static final int END = 1; // Flag for the last packet
	public static final int NOT_END = 0; // Flag for other packets
	protected static int dataToSend = 0;
	protected static FileInputStream fileInputStream;
	
	public static void main(String [] args) throws IOException, InterruptedException {
		//Get host from the inputs
		InetAddress host = InetAddress.getByName(args[0]);
		//Get port from the inputs
		int port = Integer.parseInt(args[1]);
		//Load the file
		File file = new File(args[2]);
		//we need to send as much data as there are in the file
		dataToSend = (int) file.length();
		fileInputStream = new FileInputStream(file);
        
		//Create sender socket
		DatagramSocket sender = new DatagramSocket();
		
		int currentSeqNr = 1;
		//While we still have some data to be send
		//make new packet and send it
		while(dataToSend > 0) {
			byte[] buffer = makePacket(currentSeqNr);
			currentSeqNr += 1;
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length, host, port);
			sender.send(packet);
			//Sleep for a while to avoid packet loss
			Thread.sleep(5);
		}
		sender.close();
	}
	
	/**
	 * Given the sequence number make new packet
	 * to be send by knowing how much data is left
	 * to be send.
	 * Packet is made in the following way:
	 * - The first 2 bytes are the sequence number
	 * - The third byte is the flag corresponding to last/not last packet
	 * - Next MAX_PAYLOAD bytes corresponds to an actual data to be send 
	 * @param current sequence number
	 * @return byte array
	 * @throws IOException
	 */
	public static byte[] makePacket(int currentSeqNr) throws IOException {
		byte[] message;
		byte[] packet = new byte[PACKET_SIZE];
		//If there is more data to be send
		//than the maximum payload - add the
		//flag that this is not the last packet,
		//and flag that this is the last one otherwise.
		if (dataToSend >= MAX_PAYLOAD) {
			message = new byte[MAX_PAYLOAD];
			packet[2] = NOT_END;
		} else {
			message = new byte[dataToSend];
			packet[2] = END;
		}
		
		//Continue reading the stream of size
		//the same as message
		fileInputStream.read(message);
		System.arraycopy(message, 0, packet, 3, message.length);
		
		//Add the sequence number
		byte[] seqNr = intToByteArray(currentSeqNr);
		System.arraycopy(seqNr, 0, packet, 0, seqNr.length);
		
		//Reduce the amount of data need to be send
		dataToSend -= MAX_PAYLOAD; 
        
        return packet;
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
