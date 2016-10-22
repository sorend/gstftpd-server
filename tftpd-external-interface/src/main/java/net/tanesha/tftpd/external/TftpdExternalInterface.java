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

public interface TftpdExternalInterface {

	/**
	 * Lookup a given version of a firmware.
	 * @param phone The phone type.
	 * @param version The version
	 * @return null if none
	 */
	public FirmwareVersion findVersion(String phone, String version);

	/**
	 * Lookup the latest version for a given phone type.
	 * @param phone
	 * @return null if none
	 */
	public FirmwareVersion findLatestVersion(String phone);

	/**
	 * Lookup configuration for a given mac address. This should load configuration
	 * and return the given configuration
	 * @param macAddress The MAC address
	 * @return null if not found
	 */
	public PhoneConfiguration findByMac(String macAddress);
}
