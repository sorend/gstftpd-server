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

 Copyright 2005 Catalis, implemented by Charles Duffy <cduffy@isgenesis.com>
 Portions copyright 2003 Martin Kihlgren <fwtftpd A T troja.ath.cx>
 Portions copyright 2004 Soren Davidsen <soren Zz tanesha.net>

 */
package net.tanesha.tftpd.vfs;

// java imports
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;

import net.tanesha.tftpd.core.ClientState;
import net.tanesha.tftpd.core.Vfs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * VFS server mapping tftp requests to HTTP requests.
 * 
 * @author Charles Duffy <ccd@isgenesis.com>
 */
public class HTTPProxy implements Vfs.Server {

	private final Log LOG = LogFactory.getLog(HTTPProxy.class);

	private Pattern pattern = null;
	private String target;
	
	public void setPattern(String pattern) {
		this.pattern = Pattern.compile(pattern);
	}
	public void setTarget(String target) {
		this.target = target;
	}

	public Pattern serves() {
		return pattern;
	}

	// Implements Vfs.Server
	public InputStream getInputStream(ClientState state) {
 
		String filename = state.getPacket().getFilename();
		String gsmac = (String) state.getPacket().getOptions().get("grandstream_ID");

		String urlString = encodeUrl(filename, gsmac);
		try {
			URL requestURL = new URL(urlString + filename);
			return requestURL.openStream();
		} catch (java.net.MalformedURLException e) {
			LOG.error("Invalid URL (" + urlString + filename + ")", e);
		} catch (java.io.IOException e) {
			LOG.error("Failure opening URL (" + urlString + filename + ")", e);
		}
		return null;
	}
	
	private String encodeUrl(String filename, String macAddress) {
		return target.replace("%f", filename).replace("%m", macAddress);
	}

}
