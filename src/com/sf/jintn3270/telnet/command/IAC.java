package com.sf.jintn3270.telnet.command;

public class IAC extends TelnetCommand {
	TelnetCommand subCommand;
	
	public IAC(TelnetCommand tc) {
		super((byte)0xff);
		subCommand = tc;
	}
	
	public int getLength() {
		return tc.getLength() + 1;
	}
	
	protected void send(ChannelBuffer buf) {
		super.send(buf);
		subCommand.send(buf);
	}
}
