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
package net.tanesha.tftpd.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import net.tanesha.tftpd.external.FirmwareVersion;
import net.tanesha.tftpd.external.PhoneConfiguration;
import net.tanesha.tftpd.external.TftpdExternalInterface;

public class JdbcExternalImpl implements TftpdExternalInterface {

	private String querySettingsByMac;
	private String queryPhoneByMac;
	
	private String queryByVersion;
	private String queryLatestVersion;
	
	private DataSource datasource;
	
	/**
	 * This is a datasource, can be provided by Spring
	 * @param datasource
	 */
	public void setDataSource(DataSource datasource) {
		this.datasource = datasource;
	}
	
	/**
	 * This query defines how to look up a firmware version based on a phone identifier (eg. BT100_HT286_HT486) and a version number (eg. 1.0.8.33).
	 * The query will get two input parameters (in this order): phone-identifier and version-number
	 * The query must result in rows which contains three columns: phone-identifier, version-number, firmware-path
	 * One example could be:
	 * <code><pre>
	 * select phone_id, version_no, firmware_path from firmware_versions where phone_id = ? and version_no = ?
	 * </pre></code>
	 * @param queryByVersion The SQL query
	 */
	public void setQueryByVersion(String queryByVersion) {
		this.queryByVersion = queryByVersion;
	}

	/**
	 * This query defines how to look up latest firmware version based on a phone identifier (eg. BT100_HT286_HT486).
	 * The query will get one input parameter: phone-identifier
	 * The query must result in rows which contains three columns: phone-identifier, version-number, firmware-path
	 * One example could be:
	 * <code><pre>
	 * select phone_id, version_no, firmware_path from firmware_versions where phone_id = ? and is_latest = 'T'
	 * </pre></code>
	 * @param queryLatestVersion The SQL query
	 */
	public void setQueryLatestVersion(String queryLatestVersion) {
		this.queryLatestVersion = queryLatestVersion;
	}

	/**
	 * This query defines how to look up phone firmware version based on the phone's MAC address.
	 * The query will get one input parameter: mac-address
	 * The query must result in rows which contains two columns: phone-identifier and version-number
	 * One example could be:
	 * <code><pre>
	 * select phone_id, version_no from phone_firmwares where mac_address = ?
	 * </pre></code>
	 * @param queryPhoneByMac The SQL query
	 */
	public void setQueryPhoneByMac(String queryPhoneByMac) {
		this.queryPhoneByMac = queryPhoneByMac;
	}

	/**
	 * This query defines how to look up phone provisioning settings based on the phone's MAC address.
	 * The query will get one input parameter: mac-address
	 * The query must result in rows which contains two columns: setting-name, setting-value
	 * One example could be:
	 * <code><pre>
	 * select provision_attribute, provision_value from phone_settings where mac_address = ?
	 * </pre></code>
	 * @param queryPhoneByMac The SQL query
	 */
	public void setQuerySettingsByMac(String querySettingsByMac) {
		this.querySettingsByMac = querySettingsByMac;
	}
	
	public PhoneConfiguration findByMac(String macAddress) {

		RowMapper<NameValue> settingsMapper = new RowMapper<NameValue>() {
			public NameValue mapRow(ResultSet arg0) throws SQLException {
				return new NameValue(arg0.getString(1), arg0.getString(2));
			}
		};

		List<NameValue> settings = query(querySettingsByMac, settingsMapper, macAddress);

		RowMapper<PhoneConfiguration> phoneConfigMapper = new RowMapper<PhoneConfiguration>() {
			public PhoneConfiguration mapRow(ResultSet arg0) throws SQLException {
				PhoneConfiguration config = new PhoneConfiguration();
				config.setPhone(arg0.getString(1));
				config.setFirmwareVersion(arg0.getString(2));
				return config;
			}
		};

		List<PhoneConfiguration> configs = query(queryPhoneByMac, phoneConfigMapper, macAddress);

		if (configs == null || configs.size() < 1)
			return null;
		
		PhoneConfiguration config = configs.get(0);
		config.setProperties(mapProperties(settings));
		
		return config;
	}
	
	private Map<String, String> mapProperties(List<NameValue> settings) {
		if (settings == null)
			return null;
		
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (NameValue nv : settings)
			map.put(nv.getName(), nv.getValue());
		return map;
	}
	
	public FirmwareVersion findLatestVersion(final String phone) {
		
		RowMapper<FirmwareVersion> mapper = new RowMapper<FirmwareVersion>() {
			public FirmwareVersion mapRow(ResultSet arg0) throws SQLException {
				FirmwareVersion version = new FirmwareVersion();
				version.setPhone(arg0.getString(1));
				version.setVersion(arg0.getString(2));
				version.setFirmwarePath(arg0.getString(3));
				return version;
			}
		};
		
		List<FirmwareVersion> version = query(queryLatestVersion, mapper, phone);
		
		if (version == null || version.size() < 1)
			return null;
		else
			return version.get(0);
	}

	public FirmwareVersion findVersion(final String phone, final String version) {
		
		RowMapper<FirmwareVersion> mapper = new RowMapper<FirmwareVersion>() {
			public FirmwareVersion mapRow(ResultSet arg0) throws SQLException {
				FirmwareVersion version = new FirmwareVersion();
				version.setPhone(arg0.getString(1));
				version.setVersion(arg0.getString(2));
				version.setFirmwarePath(arg0.getString(3));
				return version;
			}
		};
		
		List<FirmwareVersion> fv = query(queryByVersion, mapper, phone, version);
		
		if (fv == null || fv.size() < 1)
			return null;
		else
			return fv.get(0);
	}

	private <T> List<T> query(String query, RowMapper<T> mapper, String... params) {
		
		Connection c = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			c = datasource.getConnection();

			st = c.prepareStatement(query);
			
			for (int i = 0; i < params.length; i++) {
				st.setString(i + 1, params[i]);
			}
			
			rs = st.executeQuery();
			
			List<T> list = new LinkedList<T>();
			while (rs.next()) {
				T obj = mapper.mapRow(rs);
				if (obj != null)
					list.add(obj);
			}
			
			return list;
		}
		catch (SQLException e) {
			throw new RuntimeException("Error executing query: " + e.getMessage(), e);
		}
		finally {
			try {
				if (rs != null)
					rs.close();
				if (st != null)
					st.close();
				if (c != null)
					c.close();
			}
			catch (SQLException e) {
				throw new RuntimeException("Error closing db query: " + e.getMessage(), e);
			}
		}
	}
	
	public interface RowMapper<T> {
		public T mapRow(ResultSet row) throws SQLException;
	}
	
	
	private class NameValue {
		private String name;
		private String value;
		public NameValue(String name, String value) {
			this.name =name;
			this.value = value;
		}
		public String getName() {
			return name;
		}
		public String getValue() {
			return value;
		}
	}
}
