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
package net.tanesha.tftpd.vfs;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

public class VersioningHelper implements InitializingBean {

	private Log LOG = LogFactory.getLog(VersioningHelper.class);
	
	private Properties properties;
	private Map<String, Pattern> matchers = new HashMap<String, Pattern>();
	
	public void setModelConfiguration(Properties properties) {
		this.properties = properties;
	}
	
	public void afterPropertiesSet() throws Exception {
		String[] models = StringUtils.commaDelimitedListToStringArray(properties.getProperty("models"));
		
		for (int i = 0; i < models.length; i++) {
			String model = models[i].trim();
			
			//String name = properties.getProperty(model + ".name");
			String name = model;
			String matcher = properties.getProperty(model + ".matcher");
			
			if (name == null || matcher == null)
				throw new Exception("Invalid configuration, " + model + " not configured properly.");
			
			matchers.put(name, Pattern.compile(matcher));
		}

		LOG.info("VersioningHelper configured: " + matchers);
	}
	
	public Set<String> getAllModels() {
		return matchers.keySet();
	}
	
	public String lookupModel(String modelString) {
		if (modelString == null)
			return null;
		
		for (String key : matchers.keySet()) {
			Pattern pattern = matchers.get(key);
			
			if (pattern.matcher(modelString).find())
				return key;
		}
		return null;
	}
}
