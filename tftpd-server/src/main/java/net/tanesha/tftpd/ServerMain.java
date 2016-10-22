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
package net.tanesha.tftpd;

import net.tanesha.tftpd.core.Server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class ServerMain {

	public static void main(String[] args) throws Exception {
		
		if (args.length != 1) {
			System.out.println(ServerMain.class.getName() + " <config-file>");
			System.exit(100);
		}
		
		ApplicationContext context = new FileSystemXmlApplicationContext(args[0]);
		
		Server server = (Server) context.getBean("tftpdServer");
		
		System.out.println("Tftpd server running ... ");
		
		while (true) {
			Thread.sleep(5000);
			Thread.yield();
		}
	}
}
