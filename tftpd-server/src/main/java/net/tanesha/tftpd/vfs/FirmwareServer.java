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

 $Id: FirmwareServer.java 105 2007-06-08 17:59:18Z sorend $

 */
package net.tanesha.tftpd.vfs;

// java imports
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import net.tanesha.tftpd.core.ClientState;
import net.tanesha.tftpd.core.Vfs;
import net.tanesha.tftpd.external.FirmwareVersion;
import net.tanesha.tftpd.external.PhoneConfiguration;
import net.tanesha.tftpd.external.TftpdExternalInterface;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the server which takes care of mapping a grandstream mac into a
 * specific firmware version by lookingit up in the db. It requires the
 * <code>FIRMWARE_PATH</code>, <code>FIRMWARE_SQL</code> and
 * <code>FIRMWARE_DEFAULT</code> in the config file.
 * 
 * @author Soren Davidsen <soren Zz tanesha.net>
 */
public class FirmwareServer implements Vfs.Server {

	public static final Pattern VFS = Pattern.compile("\\.bin$");

	public static final String PROP_PATH = "firmwareServer.path";
	
	private final Log LOG = LogFactory.getLog(FirmwareServer.class);

	private TftpdExternalInterface externalInterface;
	private VersioningHelper versioningHelper;
	
	public void setTftpdExternalInterface(TftpdExternalInterface externalInterface) {
		this.externalInterface = externalInterface;
	}
	public void setVersioningHelper(VersioningHelper versioningHelper) {
		this.versioningHelper = versioningHelper;
	}
	
	public Pattern serves() {
		return VFS;
	}

	public InputStream getInputStream(ClientState state) {

		LOG.info("Options are: " + state.getPacket().getOptions());
		
		// find grandstream id from the packet.
		String gsmac = (String) state.getPacket().getOptions().get("grandstream_ID");
		String modelRaw = (String) state.getPacket().getOptions().get("grandstream_MODEL");

		// find the default version for this model.
		String model = versioningHelper.lookupModel(modelRaw);

		// no model found.
		if (model == null)
			LOG.warn("!!!! Unknown model found: " + modelRaw + " !!!!");
		else
			LOG.info("Detected model: " + model + ", from=" + modelRaw);
		
		String filename = state.getPacket().getFilename();

		// no gsmac set.
		if (gsmac == null || filename == null)
			return null;

		// try to lookup by mac.
		PhoneConfiguration config = externalInterface.findByMac(gsmac);
		FirmwareVersion version = null;
		
		if (config == null && model != null) {
			version = externalInterface.findLatestVersion(model); 
		}
		else {
			if (model != null && !model.equals(config.getPhone())) {
				LOG.warn("Configured phone is not the same as probed, provisioning futile :-) '"+model+"' vs '"+config.getPhone()+"'");
			}
			version = externalInterface.findVersion(config.getPhone(), config.getFirmwareVersion());
		}

		// no version found.
		if (version == null)
			return null;
		
		// try to serve the wanted firmware file.
		return findFirmwareFile(version, filename);
	}

	private InputStream findFirmwareFile(FirmwareVersion version, String file) {

		File path = new File(version.getFirmwarePath(), file);

		try {
			if (!path.getCanonicalPath().startsWith(new File(version.getFirmwarePath()).getCanonicalPath())) {
				LOG.error("Attempted hacking, requested: " + path);
				return null;
			}
			
			// not found.
			if (!path.exists()) {
				return null;
			}

			LOG.info("Sending firmware file: " + path.getAbsolutePath());

			InputStream in = new FileInputStream(path);

			return in;
		}
		catch (IOException e) {
			LOG.fatal("Error opening file: " + path.getAbsolutePath(), e);
			return null;
		}
	}

}
