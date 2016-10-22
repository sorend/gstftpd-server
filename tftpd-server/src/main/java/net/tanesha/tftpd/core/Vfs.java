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
package net.tanesha.tftpd.core;

// java imports
import java.io.InputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Representation of the somehow virtual filesystem in the tftpserver.
 * 
 * @author Soren Davidsen <soren Zz tanesha.net>
 */
public class Vfs {

	private static final String PROP_VFS_INUSE = "vfs.inuse";
	
	private final Log LOG = LogFactory.getLog(Vfs.class);

	// the mappers in the vfs
	private List<Vfs.Server> filesystems;

	public void setFilesystems(List<Vfs.Server> filesystems) {
		this.filesystems = filesystems;
	}
	
	// Accessor to get an inpustream.
	public InputStream getInputStream(ClientState state) {

		// try each mapping util we get an inputstream.
		for (Vfs.Server server : filesystems) {

			Pattern serverPattern = server.serves();

			if (serverPattern == null)
				continue;

			Matcher m = serverPattern.matcher(state.getPacket().getFilename());

			if (!m.find())
				continue;

			// ask the vfs.server for an inputstream
			InputStream in = server.getInputStream(state);

			// we got one, return it.
			if (in != null)
				return in;
		}

		// got nothing, return unknown filename :-S
		return null;
	}

	/**
	 * Definition of a server in the vfs.
	 * 
	 */
	public static interface Server {

		public InputStream getInputStream(ClientState state);

		public Pattern serves();
	}

}
