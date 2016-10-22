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

 $Id: ByteUtil.java 16 2007-05-25 06:59:11Z sorend $

 */
package net.tanesha.tftpd.core;

// java imports
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class contains some of the byte-mangling parts of parsing the tftp data
 * packets.
 * 
 */
public class ByteUtil {

	private static final Log LOG = LogFactory.getLog(ByteUtil.class);

	private ByteUtil() {
		// prevent instantiation.
	}

	/**
	 * @param data
	 *            The byte[] to examine
	 * @param offset
	 *            The first byte to examine
	 * @return The index of the first zero-byte in the data after the offset or
	 *         -1 if no zero-byte was found
	 */
	public static int getFirstZero(byte[] data, int offset) {
		for (offset++; offset < data.length; offset++) {
			if (data[offset] == (byte) 0) {
				return offset;
			}
		}
		return -1;
	}

	/**
	 * @param data
	 *            The byte[] to examine
	 * @param offset
	 *            The first byte to examine
	 * @param length
	 *            The number of bytes to examine
	 * @return A new byte[] containing length nr of bytes out of data from
	 *         offset
	 */
	public static byte[] subArray(byte[] data, int offset, int length) {
		if (length == -1 || length > data.length - offset) {
			length = data.length - offset;
		}
		byte[] returnValue = new byte[length];
		for (int i = 0; i < length; i++) {
			returnValue[i] = data[i + offset];
		}
		return returnValue;
	}

	/**
	 * @param data
	 *            The byte[] to examine
	 * @param offset
	 *            The first byte to examine
	 * @return The string made up of the bytes from offset to the first
	 *         zero-byte found after the offset \ or the rest of the data if no
	 *         zero was found or the empty String if only zeroes were found.
	 */
	public static String getString(byte[] data, int offset) {
		byte[] subArray = subArray(data, offset, -1);
		byte[] delimiterArray = new byte[1];
		delimiterArray[0] = (byte) 0;
		StringTokenizer stringTokenizer = new StringTokenizer(new String(subArray), new String(delimiterArray));
		if (stringTokenizer.hasMoreTokens()) {
			return stringTokenizer.nextToken();
		} else {
			return "";
		}
	}

	/**
	 * @param in
	 *            The InputStream to read from
	 * @param data
	 *            The byte[] to write to
	 * @param offset
	 *            The position in data to start writing at
	 * @param length
	 *            The number of bytes to copy
	 */
	public static void fillBytes(InputStream in, byte[] data, int offset, int length) {
		int readBytes = 0;
		while (readBytes < length) {
			try {
				readBytes = readBytes + in.read(data, offset + readBytes, length - readBytes);
			} catch (IOException e) {
				LOG.error("Couldn't read from my InputStream", e);
			}
		}
	}

}
