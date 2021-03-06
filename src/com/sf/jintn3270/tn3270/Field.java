package com.sf.jintn3270.tn3270;


/**
 * Defines a field in a Partition, in a 3270 terminal.
 * 
 * Keeps a reference to the TNFieldCharacter (attributes) of the field,
 * as well as tracking the start / end address of the field.
 */
public class Field {
	TNFieldCharacter c;
	
	int startAddress;
	int endAddress;
	boolean endSet;
	
	public Field(TNFieldCharacter c, int startAddress) {
		this.c = c;
		this.startAddress = startAddress;
		this.endAddress = startAddress;
		this.endSet = false;
	}
	
	public void setEnd(int endAddress) {
		this.endAddress = endAddress;
		endSet = true;
	}
	
	public boolean isEndSet(){ 
		return endSet;
	}
	
	public int getStart() {
		return startAddress;
	}
	
	public int getEnd() {
		return endAddress;
	}
	
	public int getLength() {
		return endAddress - startAddress;
	}
	
	public TNFieldCharacter getFieldCharacter() {
		return c;
	}
	
	public String toString() {
		return "Field[" + startAddress + "-" + endAddress + "] " + (endSet ? "set" : "unset") + " attribs: " + Integer.toBinaryString(c.getCode());
	}
}
