package com.sf.jintn3270;

/**
 * Abstract base class for all Characters in a TerminalModel's buffer.
 * 
 * By default, Characters have a code (8-bit value) and a displayable char.
 * The display character is a Java unicode (ascii) char.
 *
 * TerminalModels use a CharacterFactory (see DefaultCharacterFactory) to 
 * provide TerminalCharacter instances to the TerminalModel.
 */
public abstract class TerminalCharacter {
	protected char display;
	protected short code;
	
	protected TerminalCharacter(short code) {
		this(code, (char)code);
	}
	
	protected TerminalCharacter(short code, char display) {
		this.code = code;
		this.display = display;
	}
	
	public char getDisplay() {
		return display;
	}
	
	public short getCode() {
		return code;
	}
	
	public boolean equals(Object obj) {
		TerminalCharacter that = (TerminalCharacter)obj;
		return this.display == that.display && this.code == that.code;
	}
}
