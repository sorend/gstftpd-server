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

 $Id: PacketType.java 16 2007-05-25 06:59:11Z sorend $

 */
package net.tanesha.tftpd.core;

/**
 * Enumeration over the different Tftp packet types.
 * 
 * @author Soren Davidsen <soren Zz tanesha.net>
 */
public class PacketType {

	public static final PacketType RRQ = new PacketType(1, "RRQ");

	public static final PacketType WRQ = new PacketType(2, "WRQ");

	public static final PacketType DATA = new PacketType(3, "DATA");

	public static final PacketType ACK = new PacketType(4, "ACK");

	public static final PacketType ERROR = new PacketType(5, "ERROR");

	public static final PacketType TYPES[] = { RRQ, WRQ, DATA, ACK, ERROR };

	public static PacketType findType(int idx) {
		for (int i = 0; i < TYPES.length; i++)
			if (TYPES[i]._idx == idx)
				return TYPES[i];

		return null;
	}

	private int _idx;

	private String _name;

	private PacketType(int idx, String name) {
		_idx = idx;
		_name = name;
	}

	public String toString() {
		return _name;
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof PacketType))
			return false;

		return ((PacketType) o)._idx == this._idx;
	}

}
