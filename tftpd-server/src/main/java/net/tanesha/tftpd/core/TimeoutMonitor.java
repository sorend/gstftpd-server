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

 $Id: TimeoutHandlerTask.java 16 2007-05-25 06:59:11Z sorend $

 */
package net.tanesha.tftpd.core;

// java imports
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This handler is a thread which handles timeout retransmissions of packets if
 * client doesnt send ACK in time.
 * 
 * FIXME: look at synchronization.
 * 
 * @author Soren Davidsen <soren Zz tanesha.net>
 */
public class TimeoutMonitor implements Runnable {

	// allow 3 sec before retransmit.
	public static final int TIMEOUT_RETRANS = (3 * 1000);

	// allow max of 5 retransmissions
	public static final int MAX_RETRANSMISSIONS = 5;

	private final Log LOG = LogFactory.getLog(TimeoutMonitor.class);

	private Server server;

	private boolean running = true;
	
	public TimeoutMonitor(Server s) {
		server = s;
	}

	public void stop() {
		running = false;
	}
	
	// Implements Runnable
	public void run() {

		LOG.info("Retransmit server is running ..");

		while (running) {

			try {

				// sleep 1 sec.
				Thread.sleep(1000);

				long timeout = System.currentTimeMillis() - TIMEOUT_RETRANS;

				// check if some packets needs retransmit.
				for (Iterator i = server.stateByRemote.entrySet().iterator(); i.hasNext();) {
					ClientState state = (ClientState) ((Map.Entry) i.next()).getValue();

					DataPacket pack = state.getLastData();

					if (pack == null)
						continue;

					// retransmit.
					if (pack.getLastSentAt() < timeout) {
						LOG.debug("Retransmitting " + state + ", age=" + (timeout - pack.getLastSentAt() + TIMEOUT_RETRANS) + ", count=" + pack.getRetransCount());

						pack.send(server);

						pack.incRetransCount();

						// check retransmission counts.
						if (pack.getRetransCount() > MAX_RETRANSMISSIONS)
							server.closedownState(state);

					}

				}

			} catch (Throwable t) {
				// concurrent modification exception might be here sometimes,
				// but we can just ignore it,
				// and try again a little later :-)
				LOG.warn("Synchronization fixup :-S", t);
			}
		}

	}

}
