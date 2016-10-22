/*
 * Gratissip Tftpd Server
 * Copyright (C) 2007  Soren Davidsen
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.tanesha.tftpd.external;

import java.io.Serializable;

public class FirmwareVersion implements Serializable {
	
	private static final long serialVersionUID = -6595325310200287132L;
	
	public String version;
	public String phone;
	public String firmwarePath;
	
	public void setVersion(String version) {
		this.version = version;
	}
	public String getVersion() {
		return version;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getPhone() {
		return phone;
	}
	public void setFirmwarePath(String firmwarePath) {
		this.firmwarePath = firmwarePath;
	}
	public String getFirmwarePath() {
		return firmwarePath;
	}
}
