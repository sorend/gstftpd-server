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

 $Id: ProvisionServer.java 105 2007-06-08 17:59:18Z sorend $

 */
package net.tanesha.tftpd.vfs;

// java imports
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Pattern;

import net.tanesha.tftpd.core.ClientState;
import net.tanesha.tftpd.core.Vfs;
import net.tanesha.tftpd.external.PhoneConfiguration;
import net.tanesha.tftpd.external.TftpdExternalInterface;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the Vfs server that creates a provisioning inputstream. To use it you
 * have to define the <code>PROVISION_SQL</code> in the config file.
 * 
 * Thanks to my brother Jens Davidsen for providing me with the info on how to
 * build the correct inputstream for Grandstream to accept the firmware.
 * 
 * @author Soren Davidsen <soren Zz tanesha.net>
 */
public class ProvisionServer implements Vfs.Server {

	// gnkey property
	public static final String GNKEY = "0b82";

	// the pattern that matches a grandstream cfg<MAC> file request.
	public static final Pattern CFG_FILE = Pattern.compile("^cfg[0-9a-f]{12}$");

	private final Log LOG = LogFactory.getLog(ProvisionServer.class);

	private TftpdExternalInterface externalInterface;
	
	public void setTftpdExternalInterface(TftpdExternalInterface externalInterface) {
		this.externalInterface = externalInterface;
	}

	public Pattern serves() {
		return CFG_FILE;
	}

	// Implements Vfs.Server
	public InputStream getInputStream(ClientState state) {

		String modelRaw = (String) state.getPacket().getOptions().get("grandstream_MODEL");
		
		String filename = state.getPacket().getFilename();

		String gsmac = filename.substring(3);

		LOG.info("Serving configuration for client=" + gsmac + ", model=" + modelRaw);

		PhoneConfiguration config = externalInterface.findByMac(gsmac);

		if (config == null) {
			return null;
		}

		Map<String, String> prop = config.getProperties(); 

		// got no properties.
		if (prop == null || prop.size() <= 0)
			return null;

		// generate the config and create input stream over it.
		ByteArrayInputStream bin = new ByteArrayInputStream(createConfig(prop, gsmac));

		// return ;)
		return bin;
	}

	/*
	 *  // return the default configuration. private Properties
	 * defaultConfiguration() {
	 * 
	 * Properties prop = new Properties();
	 * 
	 * try { InputStream din = new FileInputStream(DEFAULTS_INI);
	 * prop.load(din); din.close(); return prop; } catch (IOException ie) {
	 * System.out.println("Error loading the defaults file: "+DEFAULTS_INI); }
	 * 
	 * return prop; }
	 * 
	 */

	/**
	 * Create a config byte-array.
	 * 
	 * @param prop
	 *            The array of properties.
	 * @param mac
	 *            the macaddress, has to be like aaaaaaaaaaaa where a in
	 *            [0-9a-f]
	 */
	public byte[] createConfig(Map<String, String> prop, String mac) {

		if (mac.length() != 12)
			return null;

		// Properties defaults = defaultConfiguration();

		// put gnkey
		// defaults.put("gnkey", GNKEY);
		prop.put("gnkey", GNKEY);

		StringBuilder buf = new StringBuilder();

		for (String key : prop.keySet()) {

			String value = prop.get(key);

			if (value == null)// || "".equals(value))
				continue;

			if (buf.length() == 0)
				buf.append(key).append("=").append(value);
			else
				buf.append("&").append(key).append("=").append(value);
		}

		// make the buffer length even.
		if ((buf.length() % 2) != 0)
			buf.append((char) 0);

		// build body.
		byte body[] = buf.toString().getBytes();

		// sample mac: 00:02:B3:33:7D:6A (list03 ;).
		byte header[] = new byte[] { 0, 0, (byte) (((16 + buf.length()) / 2) >> 8), (byte) ((16 + buf.length()) / 2), 0, 0, Integer.valueOf(mac.substring(0, 2), 16).byteValue(), Integer.valueOf(mac.substring(2, 4), 16).byteValue(),
				Integer.valueOf(mac.substring(4, 6), 16).byteValue(), Integer.valueOf(mac.substring(6, 8), 16).byteValue(), Integer.valueOf(mac.substring(8, 10), 16).byteValue(), Integer.valueOf(mac.substring(10, 12), 16).byteValue(), 13, 10, 13, 10 };

		// make complete bytearray
		byte all[] = new byte[header.length + buf.length()];
		System.arraycopy(header, 0, all, 0, header.length);
		System.arraycopy(body, 0, all, header.length, body.length);

		// calculate CRC.
		int k = 0;
		for (int i = 0; i < all.length / 2; i++) {
			k += (all[i * 2] << 8) & 0xff00;
			k += all[i * 2 + 1] & 0xff;
			k &= 0xffff;
		}

		k = 0x10000 - k;
		all[4] = (byte) (k >> 8);
		all[5] = (byte) k;

		return all;
	}

	/**
	 * Test class
	 * 
	 * public static void main(String [] args) throws Exception {
	 * 
	 * if (args.length < 2) { System.out.println("syntax:
	 * GranstreamConfiguration <settings-file> <macaddr>"); return; }
	 * 
	 * Properties prop = new Properties();
	 * 
	 * InputStream in = new FileInputStream(args[0]);
	 * 
	 * prop.load(in);
	 * 
	 * in.close();
	 * 
	 * byte temp[] = instance().createConfig(prop, args[1]);
	 * 
	 * System.out.write(temp, 0, temp.length); }
	 */

}
