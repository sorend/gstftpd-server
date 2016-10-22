/*
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 Copyright 2003 Martin Kihlgren <fwtftpd A T troja.ath.cx>

 */
/*

 This is a fork of Martin Kihlgren's fwtftpd, little restructured and some extra
 functionality specific to Grandstream phones was added.

 Copyright 2004 Soren Davidsen <soren Zz tanesha.net>

 $Id: ClientState.java 16 2007-05-25 06:59:11Z sorend $

 */
package net.tanesha.tftpd.core;

// java imports
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClientState holds information of the current state of the client.
 * 
 */
public class ClientState {

	// client states strings
	public static final String NEW = "NEW";

	public static final String ESTABLISHED = "ESTABLISHED";

	public static final String DONE = "DONE";

	private static final Logger LOG = Logger.getLogger("net.tanesha.grandstream.tftpd.ClientState");

	// properties of the client state
	private String _state;

	private long _createdAt;

	private SocketAddress _remote;

	private InputStream _inputStream;

	private int _blockNumber = 0;

	public RequestPacket _lastPacket = null;

	public DataPacket _lastData = null;

	// Constructor
	public ClientState(SocketAddress remote) {
		_remote = remote;
		_createdAt = System.currentTimeMillis();
		_state = NEW;
	}

	// Accessor methods
	public DataPacket getLastData() {
		return _lastData;
	}

	public void setLastData(DataPacket dp) {
		_lastData = dp;
	}

	public SocketAddress getRemote() {
		return _remote;
	}

	// inputstream accessor methods
	public void setInputStream(InputStream in) {
		_blockNumber = 0;
		_inputStream = in;
	}

	public InputStream getInputStream() {
		return _inputStream;
	}

	// blocknumber accessor methods
	public int getBlockNumber() {
		return _blockNumber;
	}

	public void incBlockNumber() {
		_blockNumber++;
	}

	// state accessor methods
	public String getState() {
		return _state;
	}

	public void updateState(String state) {
		_state = state;
	}

	public void updatePacket(RequestPacket packet) {
		_lastPacket = packet;
	}

	public RequestPacket getPacket() {
		return _lastPacket;
	}

	public PacketType getType() {
		return _lastPacket.getType();
	}

	public long getCreatedAt() {
		return _createdAt;
	}

	public String toString() {
		return "<ClientState remote=" + _remote + ", state=" + _state + ", blockN=" + _blockNumber + ", " + _lastPacket + ">";
	}

	public void close() {
		if (_inputStream != null) {
			try {
				_inputStream.close();
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Couldn't close my inputStream", e);
			}
		}
		// stateByRemote.remove(this.remote);
	}
}
