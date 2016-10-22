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

import net.tanesha.tftpd.external.FirmwareVersion;
import net.tanesha.tftpd.external.PhoneConfiguration;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import junit.framework.TestCase;

public class JdbcExternalImplTest extends TestCase {

	DriverManagerDataSource ds;
	JdbcExternalImpl impl;
	
	protected void setUp() throws Exception {
		// create a datasource
		ds = new DriverManagerDataSource("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:test", "sa", "");
		
		Connection c = ds.getConnection();

		// create tables
		c.createStatement().execute("create table firmware_versions ( phone_id varchar(50), version_no varchar(50), firmware_path varchar(100), is_latest varchar(1) )");
		c.createStatement().execute("create table phone_firmwares ( mac_address varchar(15) primary key, phone_id varchar(50), version_no varchar(50) )");
		c.createStatement().execute("create table phone_settings ( mac_address varchar(15), provision_attribute varchar(10), provision_value varchar(100) )");
		c.close();
		
		impl = new JdbcExternalImpl();
		impl.setDataSource(ds);
		impl.setQueryByVersion("select phone_id, version_no, firmware_path from firmware_versions where phone_id = ? and version_no = ?");
		impl.setQueryLatestVersion("select phone_id, version_no, firmware_path from firmware_versions where phone_id = ? and is_latest = 'T'");
		impl.setQueryPhoneByMac("select phone_id, version_no from phone_firmwares where mac_address = ?");
		impl.setQuerySettingsByMac("select provision_attribute, provision_value from phone_settings where mac_address = ?");
	}
	
	protected void tearDown() throws Exception {
		Connection c = ds.getConnection();
		c.createStatement().execute("drop table firmware_versions");
		c.createStatement().execute("drop table phone_firmwares");
		c.createStatement().execute("drop table phone_settings");
		c.close();
	}
	
	public void testFindByMac() throws Exception {
		
		Connection c = ds.getConnection();
		c.createStatement().execute("insert into firmware_versions values ('BT100','1.0.8.33','/var/firmwares/BT100/1.0.8.33', 'F')");
		c.createStatement().execute("insert into phone_firmwares values ('123456789012', 'BT100','1.0.8.33')");
		c.createStatement().execute("insert into phone_settings values ('123456789012', 'P10','10.0.0.1')");
		c.createStatement().execute("insert into phone_settings values ('123456789012', 'P11','YES')");
		c.close();
		
		PhoneConfiguration cfg = impl.findByMac("123456789012");
		assertNotNull(cfg);
		assertEquals("1.0.8.33", cfg.getFirmwareVersion());
		assertEquals("BT100", cfg.getPhone());
		assertEquals(2, cfg.getProperties().size());
		assertTrue(cfg.getProperties().containsKey("P10"));
		assertTrue(cfg.getProperties().containsKey("P11"));
		assertEquals("10.0.0.1", cfg.getProperties().get("P10"));
		assertEquals("YES", cfg.getProperties().get("P11"));

		PhoneConfiguration cfg2 = impl.findByMac("123456789011");
		assertNull(cfg2);
		
	}

	public void testFindLatestVersion() throws Exception {
		
		Connection c = ds.getConnection();
		c.createStatement().execute("insert into firmware_versions values ('BT100','1.0.8.33','test1', 'F')");
		c.createStatement().execute("insert into firmware_versions values ('BT100','1.0.8.34','test2', 'T')");
		c.createStatement().execute("insert into firmware_versions values ('HT488','1.0.8.35','test3', 'T')");
		c.createStatement().execute("insert into firmware_versions values ('HT388','1.0.8.36','test4', 'F')");
		c.close();
		
		FirmwareVersion v1 = impl.findLatestVersion("HT286");
		assertNull(v1);
		
		FirmwareVersion v2 = impl.findLatestVersion("BT100");
		assertNotNull(v2);
		assertEquals("BT100", v2.getPhone());
		assertEquals("1.0.8.34", v2.getVersion());
		assertEquals("test2", v2.getFirmwarePath());
		
		FirmwareVersion v3 = impl.findLatestVersion("HT488");
		assertNotNull(v3);
		assertEquals("HT488", v3.getPhone());
		assertEquals("1.0.8.35", v3.getVersion());
		assertEquals("test3", v3.getFirmwarePath());
		
		FirmwareVersion v4 = impl.findLatestVersion("HT388");
		assertNull(v4);
		
	}

	public void testFindVersion() throws Exception {
		Connection c = ds.getConnection();
		c.createStatement().execute("insert into firmware_versions values ('BT100','1.0.8.33','test1', 'F')");
		c.createStatement().execute("insert into firmware_versions values ('BT100','1.0.8.34','test2', 'T')");
		c.createStatement().execute("insert into firmware_versions values ('HT488','1.0.8.35','test3', 'T')");
		c.createStatement().execute("insert into firmware_versions values ('HT388','1.0.8.36','test4', 'F')");
		c.close();
		
		FirmwareVersion v1 = impl.findVersion("BT100", "1.0.8.33");
		assertNotNull(v1);
		assertEquals("BT100", v1.getPhone());
		assertEquals("1.0.8.33", v1.getVersion());
		assertEquals("test1", v1.getFirmwarePath());

		FirmwareVersion v2 = impl.findVersion("BT100", "1.0.8.34");
		assertNotNull(v2);
		assertEquals("BT100", v2.getPhone());
		assertEquals("1.0.8.34", v2.getVersion());
		assertEquals("test2", v2.getFirmwarePath());

		FirmwareVersion v3 = impl.findVersion("BT100", "1.0.8.35");
		assertNull(v3);

		FirmwareVersion v4 = impl.findVersion("HT488", "1.0.8.33");
		assertNull(v4);

	}

}
