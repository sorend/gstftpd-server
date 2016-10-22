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

 Copyright 2004 Soren Davidsen <soren IN tanesha.net>

 $Id: OutPacket.java 16 2007-05-25 06:59:11Z sorend $

 */
package net.tanesha.tftpd.core;

// java imports
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Representation of an outgoing tftp packet.
 *
 */
public abstract class OutPacket {

	private static final Log LOG = LogFactory.getLog(OutPacket.class);

	protected byte[] data;

	private SocketAddress remote;

	public OutPacket() {
	}

	public OutPacket(ClientState state) {
		remote = state.getRemote();
	}

	public void send(Server s) {
		DatagramPacket packet = createPacket(data, remote);
		s.sendPacket(packet);
	}

	// create a datagram packet from a bytearray and a remote address.
	private static DatagramPacket createPacket(byte[] data, SocketAddress remote) {
		DatagramPacket packet = new DatagramPacket(data, data.length, remote);
		return packet;
	}

}
