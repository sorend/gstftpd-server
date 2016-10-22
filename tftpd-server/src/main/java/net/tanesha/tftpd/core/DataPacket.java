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

 $Id: DataPacket.java 16 2007-05-25 06:59:11Z sorend $

 */
package net.tanesha.tftpd.core;

// java imports
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Representation of an outgoing tftp data packet.
 * 
 */
public class DataPacket extends OutPacket {
	private ClientState state;

	private long lastSentAt;

	private int _retransCount = 0;

	private static final Logger LOG = Logger.getLogger("net.tanesha.grandstream.tftpd.DataPacket");

	public DataPacket(ClientState state) {
		super(state);
		this.state = state;

		InputStream in = state.getInputStream();

		try {
			if (in.available() >= 512) {
				data = new byte[516];
			} else {
				data = new byte[4 + in.available()];
				state.updateState(ClientState.DONE);
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Couldn't look at my InputStream", e);
		}
		Arrays.fill(data, (byte) 0);
		data[1] = (byte) 3;

		// increase the blocknumber
		state.incBlockNumber();
		int blockNumber = state.getBlockNumber();

		data[2] = (byte) (blockNumber >> 8);
		data[3] = (byte) blockNumber;
		ByteUtil.fillBytes(in, data, 4, data.length - 4);
	}

	public void send(Server s) {
		super.send(s);
		state.setLastData(this);
		this.lastSentAt = System.currentTimeMillis();
	}

	public long getLastSentAt() {
		return lastSentAt;
	}

	// retranscount accessor methods.
	public int getRetransCount() {
		return _retransCount;
	}

	public void incRetransCount() {
		_retransCount++;
	}

	public String toString() {
		return "<DataPacket " + lastSentAt + ">";
	}

}
