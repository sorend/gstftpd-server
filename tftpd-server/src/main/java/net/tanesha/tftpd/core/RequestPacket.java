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

 $Id: RequestPacket.java 16 2007-05-25 06:59:11Z sorend $

 */
package net.tanesha.tftpd.core;

// java imports
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Representation of a tftp request packet.
 * 
 * This was RQPacket in fwtftpd
 */
public class RequestPacket {

	private static final Logger LOG = Logger.getLogger("net.tanesha.grandstream.tftpd.RequestPacket");

	public static final String NETASCII = "NETASCII";

	public static final String OCTET = "OCTET";

	public static final String MAIL = "MAIL";

	// factory for the packet
	public static RequestPacket newPacket(byte[] data) {

		if (data == null)
			return null;

		int itype = new Integer(data[1]).intValue();

		PacketType type = PacketType.findType(itype);

		if (type == null)
			return null;

		String filename = ByteUtil.getString(data, 2);

		if (filename == null)
			return null;

		int offsetet = 0;

		offsetet = ByteUtil.getFirstZero(data, 2);

		String mode = ByteUtil.getString(data, offsetet).toUpperCase();

		if (!mode.equals(NETASCII) && !mode.equals(OCTET) && !mode.equals(MAIL)) {
			LOG.info("mode '" + mode + "' is not known...");
		}

		HashMap options = new HashMap();
		byte[] delimiterArray = new byte[1];
		delimiterArray[0] = (byte) 0;
		String s = new String(data);
		String[] ss = s.split(new String(delimiterArray), -2);
		for (int i = 3; i < ss.length; i = i + 2) {
			if (ss[i].equals("")) {
				i = ss.length;
			} else {
				options.put(ss[i], ss[i + 1]);
			}
		}

		return new RequestPacket(type, filename, mode, options);
	}

	// properties for request
	private PacketType _type;

	private String _filename;

	private String _mode;

	private HashMap _options;

	public String toString() {
		return ("<RequestPacket" + " type='" + _type + "'" + " mode='" + _mode + "'" + " filename='" + _filename + "'>");
	}

	public RequestPacket(PacketType type, String filename, String mode, HashMap options) {
		_type = type;
		_filename = filename;
		_mode = mode;
		_options = options;
	}

	public HashMap getOptions() {
		return _options;
	}

	public PacketType getType() {
		return _type;
	}

	public String getFilename() {
		return _filename;
	}
}
