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

 $Id: Vfs.java 16 2007-05-25 06:59:11Z sorend $

 */
package net.tanesha.tftpd.vfs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.regex.Pattern;

import net.tanesha.tftpd.core.ClientState;
import net.tanesha.tftpd.core.Vfs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TftpRootServer implements Vfs.Server {

	// matcher for all files ;-)
	private static final Pattern TFTPROOT_PAT = Pattern.compile(".+");

	private Log LOG = LogFactory.getLog(TftpRootServer.class);

	private String rootpath;

	public void setRootpath(String rootpath) {
		this.rootpath = rootpath;
	}
	
	public Pattern serves() {
		return TFTPROOT_PAT;
	}

	// get inputstream from tftproot.
	public InputStream getInputStream(ClientState state) {

		String filename = state.getPacket().getFilename();

		try {

			File file = new File(rootpath, filename);
			String abs = file.getCanonicalPath();

			if (!abs.startsWith(new File(rootpath).getCanonicalPath())) {
				LOG.warn("Pathname hacking, tried to get: " + abs);
				return null;
			}

			if (!file.exists())
				return null;
			
			return new FileInputStream(file);

		} catch (Exception e) {
			LOG.info("Error finding file in tftproot: ", e);
			return null;
		}

	}

}
