package com.sf.jintn3270.tn3270;

import com.sf.jintn3270.telnet.Binary;
import com.sf.jintn3270.telnet.EndOfRecord;
import com.sf.jintn3270.telnet.Option;
import com.sf.jintn3270.telnet.TelnetClient;
import com.sf.jintn3270.telnet.TelnetConstants;

import java.util.ArrayList;
import java.util.EnumSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * TODO: Allow specifying a device name, properly handle REJECT during the CONNECT phase.
 */
public class Tn3270e extends Option implements TelnetConstants {
	public static final int MODE_3270 = 0;
	public static final int MODE_NVT = 5;

	EndOfRecord eor;
	Binary binary;
	
	String deviceType;
	String deviceName;
	
	private int dataMode;
	
	ArrayList<Function> requestedFunctions;
	
	public static final short TN3270E = 40;
	
	public enum Command {
		ASSOCIATE, 
		CONNECT, 
		DEVICE_TYPE, 
		FUNCTIONS, 
		IS, 
		REASON, 
		REJECT, 
		REQUEST, 	
		SEND
	};
	
	public enum Reason {
		CONN_PARTNER,
		DEVICE_IN_USE,
		INV_ASSOCIATE,
		INV_DEVICE_NAME,
		INV_DEVICE_TYPE,
		TYPE_NAME_ERROR,
		UNKNOWN_ERROR,
		UNSUPPORTED_REQ
	};
	
	public enum Function {
		BIND_IMAGE,
		DATA_STREAM_CTL,
		RESPONSES,
		SCS_CTL_CODES,
		SYSREQ
	};
	
	
	public Tn3270e(EndOfRecord eor, Binary b) {
		super();
		deviceType = "";
		deviceName = "";
		this.eor = eor;
		this.binary = b;
		
		requestedFunctions = new ArrayList<Function>();
		
		dataMode = -1;
	}
	
	public String getName() {
		return "Tn3270e";
	}
	
	public short getCode() {
		return TN3270E;
	}
	
	
	public void initiate(TelnetClient client) {
	}
	
	public int consumeIncoming(short[] incoming, TelnetClient client) {
		System.out.println("incoming length: " + incoming.length);
		// If the binary option is enabled, we've negotiated the stream
		// successfully, and need to look for 3270 frames.
		if (dataMode != -1) {
			// frame must consist of at least <5-byte header> IAC EOR
			if (incoming.length >= 7) {
				int dataEnd = 0;
				boolean found = false;
				for (; dataEnd < incoming.length - 1 && !found; dataEnd++) {
					if (incoming[dataEnd] == IAC &&
					    incoming[dataEnd + 1] == eor.EOR)
					{
						found = true;
					}
				}
				
				// If we find an EOR record, then we process the frame.
				if (found) {
					System.out.println("Found IAC, EOR at: " + dataEnd);
					
					// Parse the 5-byte header.
					short dataType = incoming[0];
					short requestFlag = incoming[1];
					short responseFlag = incoming[2];
					int sequence = incoming[3] << 8 | incoming[4];
					
					// Basic 3270e requires only supporting dataType in 3270 and NVT modes.
					
					if (dataType != dataMode) {
						// TODO: Switch data modes from 3270 to NVT or 
						// from NVT to 3270
					}
					System.out.println("Data Length: " + (incoming.length - 7));
					short[] dataFrame = new short[incoming.length - 7];
					System.arraycopy(incoming, 5, dataFrame, 0, dataFrame.length);
					
					if (dataType == MODE_3270) {
						// Send 3270 Binary option to be parsed
						binary.setEnabled(true, client);
						binary.consumeIncoming(dataFrame, client);
						binary.setEnabled(false, client);
					} else if (dataType == MODE_NVT) {
						// Render NVT data directly to the model.
						client.getTerminalModel().print(dataFrame);
					}
					
					System.out.println("Consumed 3270 data: " + (dataEnd + 1) + " bytes");
					return dataEnd + 1;
				}
			}
		}
		// Otherwise, there's not enough data to process this as a frame.
		return 0;
	}
	
	public int consumeIncomingSubcommand(short[] incoming, TelnetClient client) {
		int length = 0;
		for (int i = 0; i < incoming.length - 1; i++) {
			if (incoming[i] == IAC && incoming[i + 1] == SE) {
				length = i + 2;
			}
		}
		System.out.println("Subcommand is " + length + " bytes long.");
		if (length > 0) {
			System.out.println("Subcommand: " + resolveValue(incoming[3], Command.class) + " " + resolveValue(incoming[4], Command.class));
			if (incoming[3] == Command.SEND.ordinal() && 
			    incoming[4] == Command.DEVICE_TYPE.ordinal()) 
			{
				try {
					System.out.println("Sending DEVICE_TYPE REQUEST");
					out.write(new short[] {IAC, SB, getCode(), (short)Command.DEVICE_TYPE.ordinal(), (short)Command.REQUEST.ordinal()});
					out.write(((client.getTerminalModel().getModelName())[0]).getBytes("ASCII"));
					out.write(new short[] {IAC, SE});
				} catch (IOException ioe) {
					System.out.println("Failed to send Device Type response.");
				}
			} else if (incoming[3] == Command.DEVICE_TYPE.ordinal() &&
				      incoming[4] == Command.IS.ordinal())
			{
				// Device type is set.
				StringBuffer buf = new StringBuffer();
				int i = 5;
				for (; i < length - 2 && incoming[i] != Command.CONNECT.ordinal(); i++) {
					buf.append((char)incoming[i]);
				}
				deviceType = buf.toString();
				buf.delete(0, buf.length());
				
				if (incoming[i] == Command.CONNECT.ordinal()) {
					for (++i;i < length - 2; i++) {
						buf.append((char)incoming[i]);
					}
				}
				deviceName = buf.toString();
				
				System.out.println("Received type: " + deviceType + " name: " + deviceName);
				
				// Send a request with the list of functions we intend to support.
				// For now, we'll NOT send any, and just implement "Basic TN3270E".
				//requestedFunctions.add(Function.BIND_IMAGE);
				//requestedFunctions.add(Function.DATA_STREAM_CTL);
				//requestedFunctions.add(Function.RESPONSES);
				//requestedFunctions.add(Function.SCS_CTL_CODES);
				//requestedFunctions.add(Function.SYSREQ);
				requestFunctions();
			} else if (incoming[3] == Command.DEVICE_TYPE.ordinal() &&
			           incoming[4] == Command.REJECT.ordinal() &&
					 incoming[5] == Command.REASON.ordinal())
			{
				// Rejected device type. Reason = index 6 through length - 2.
			} else if (incoming[3] == Command.FUNCTIONS.ordinal() &&
			           incoming[4] == Command.REQUEST.ordinal())
			{
				// Our last request was likely rejected.
				// Build a list of the proposed functions from the other side.
				ArrayList<Function> proposed = new ArrayList<Function>();
				for (int i = 5; i < length - 2; i++) {
					proposed.add((Function)(resolveValue(incoming[i], Function.class)));
				}
				for (Function f : proposed) {
					System.out.println("  server proposed: " + f);
				}
				System.out.println("Setting proposed as my list");
				requestedFunctions = proposed;
				requestFunctions();
			} else if (incoming[3] == Command.FUNCTIONS.ordinal() &&
			           incoming[4] == Command.IS.ordinal())
			{
				ArrayList<Function> proposed = new ArrayList<Function>();
				for (int i = 5; i < length - 2; i++) {
					proposed.add((Function)(resolveValue(incoming[i], Function.class)));
				}
				int i = 0;
				boolean match = (proposed.size() == requestedFunctions.size());
				while (match && i < proposed.size()) {
					match = requestedFunctions.contains(proposed.get(i++));
				}
				if (match) {
					System.out.println("FUNCTIONS IS matched!");
					dataMode = MODE_3270;
				} else {
					System.out.println("FUNCTIONS IS did NOT match!");
					requestedFunctions = proposed;
					requestFunctions();
				}
			}
		}
		return length;
	}
	
	private void requestFunctions() {
		try {
			out.write(new short[] {IAC, SB, getCode(), (short)Command.FUNCTIONS.ordinal(), (short)Command.REQUEST.ordinal()});
			for (int i = 0; i < requestedFunctions.size(); i++) {
				out.write(requestedFunctions.get(i).ordinal());
			}
			out.write(new short[] {IAC, SE});
		} catch (IOException ioe) {
			System.err.println("Error sending function request");
		}
	}
	
	/**
	 * If we're enabled, defer to our parent.
	 * If we're not enabled, then we never have anything to write.
	 */
	public short[] outgoing(ByteArrayOutputStream queuedForSend, TelnetClient client) {
		// If we're not enabled, we never send.
		if (!isEnabled()) {
			return nill;
		}
		return super.outgoing(queuedForSend, client);
	}
	
	/**
	 * Resolves a byte to an Enumeration Value.
	 */
	private Enum resolveValue(short ordinal, Class<? extends Enum> type) {
		for (Object e : EnumSet.allOf(type)) {
			if (ordinal == ((Enum)e).ordinal()) {
				return (Enum)e;
			}
		}
		return null;
	}
	
	
	
}
