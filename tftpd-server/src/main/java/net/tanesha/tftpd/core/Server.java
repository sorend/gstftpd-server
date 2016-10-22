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

 $Id: Server.java 16 2007-05-25 06:59:11Z sorend $

 */
package net.tanesha.tftpd.core;

// java imports
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * This is what is left of the original fwftpd.java from Martin Kihlgren.
 * 
 */
public class Server implements InitializingBean, DisposableBean {

	public static final String VERSION = "$Id: Server.java 16 2007-05-25 06:59:11Z sorend $";
	
	private final Log LOG = LogFactory.getLog(Server.class);

	public final int FILE_NOT_FOUND = 1;

	public final int ACCESS_VIOLATION = 2;

	public final int maxFileNotFoundCache = 100;

	// holds the states of clients.
	protected Map<SocketAddress, ClientState> stateByRemote = new HashMap<SocketAddress, ClientState>();

	// the socket whhere we do all communications.
	private DatagramSocket serverSocket = null;

	private Thread serverHandlerThread = null;
	private Thread timeoutHandlerThread = null;
	private TimeoutMonitor timeoutHandler;

	private Vfs vfs;
	private int port;
	private String bindhost = null;

	public void setVfs(Vfs vfs) {
		this.vfs = vfs;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public void setBindhost(String bindhost) {
		this.bindhost = bindhost;
	}

	public void afterPropertiesSet() throws Exception {

		// initialize port.
		try {
			// bind to the socket
			InetSocketAddress saddr = new InetSocketAddress(bindhost, port);

			// create the socket to listen on.
			serverSocket = new DatagramSocket(saddr);
			
		} catch (SocketException e) {
			throw new RuntimeException("Startup failed: Couldn't create socket on port '" + port + "'", e);
		}

		// start retransmit timer.
		timeoutHandler = new TimeoutMonitor(this);
		timeoutHandlerThread = new Thread(timeoutHandler);
		timeoutHandlerThread.start();
		
		serverHandlerThread = new Thread(new ServerRunner());
		serverHandlerThread.start();
	}

	public void destroy() {
		// close serversocket
		serverSocket.close();
		// stop the timeout handler.
		timeoutHandler.stop();
	}

	private class ServerRunner implements Runnable {
		
		public void run() {
		
			byte[] buffer = new byte[1024];
	
			DatagramPacket packet = new DatagramPacket(buffer, 512);
	
			// loop forever.
			while (true) {
	
				try {
	
					boolean isOk = true;
	
					// receive a packet.
					try {
						serverSocket.receive(packet);
	
					} catch (IOException e) {
						if (e instanceof SocketException && e.getMessage() != null && e.getMessage().startsWith("Resource temporarily unavailable")) {
							LOG.warn("Got a 'Resource temporarily unavailable, but ignoring that", e);
							isOk = false;
						} else {
							LOG.error("Couldn't receive on my socket: " + e.getMessage(), e);
						}
					}
	
					if (isOk) {
						handleInput(packet.getSocketAddress(), packet.getData());
					}
	
					// clean dead clients.
					cleanUp();
	
				} catch (Throwable t) {
					if (t.getMessage() != null && t.getMessage().startsWith("Couldn't receive on my socket on port")) {
						LOG.error("Bad!", t);
					}
					if (stateByRemote.containsKey(packet.getSocketAddress())) {
						SocketAddress remote = packet.getSocketAddress();
						ClientState state = stateByRemote.get(remote);
						closedownState(state);
					}
					LOG.warn("Got a problem, but ignoring that and continuing.", t);
				}
			}
		}
	}

	private void cleanUp() {
		for (SocketAddress remote : stateByRemote.keySet()) {
			ClientState state = stateByRemote.get(remote);
			if (state.getCreatedAt() < System.currentTimeMillis() - (1000 * 60 * 10)) {
				LOG.info("Closing down old hung state: " + state);
				closedownState(state);
			}
		}
	}

	protected void closedownState(ClientState s) {
		LOG.info("closedownState: " + s);
		stateByRemote.remove(s.getRemote());
		s.close();
	}

	public void sendPacket(DatagramPacket packet) {
		try {
			serverSocket.send(packet);
		} catch (IOException e) {
			LOG.warn("Couldn't send DatagramPacket", e);
		}
	}

	private void handleInput(SocketAddress remote, byte[] data) {
		if (stateByRemote.containsKey(remote)) {
			handleEstablishedInput((ClientState) stateByRemote.get(remote), data);
		} else {
			stateByRemote.put(remote, new ClientState(remote));
			handleNewInput((ClientState) stateByRemote.get(remote), data);
		}
	}

	private void handleNewInput(ClientState state, byte[] data) {

		RequestPacket packet = RequestPacket.newPacket(data);

		// could not parse the packet
		if (packet == null)
			return;

		// put packet into state.
		state.updatePacket(packet);

		// state is now established
		state.updateState(ClientState.ESTABLISHED);

		PacketType type = state.getType();

		// String gsmac=(String)state.options.get("grandstream_ID");
		if (type == PacketType.RRQ) {

			// inputstream mapper
			InputStream sending = vfs.getInputStream(state);

			if (sending == null) {
				new ErrorPacket(state, FILE_NOT_FOUND).send(this);
				LOG.info("Sent ErrorPacket to '" + state.getRemote() + "' due to non-existing file");

				// remove state.
				closedownState(state);

				return;
			}

			// initialze the inputstream in the state.
			state.setInputStream(sending);

			// sned the first packet
			new DataPacket(state).send(this);

		} else {
			new ErrorPacket(state, ACCESS_VIOLATION).send(this);
			LOG.info("Sent ErrorPacket to '" + state.getRemote() + "' due to non-existing file");

			// remove state.
			closedownState(state);
		}
	}

	private void handleEstablishedInput(ClientState state, byte[] data) {
		if (state.getType() == PacketType.RRQ) {
			if (data[1] == (byte) 4) {
				handleAck(state, data);
			} else if (data[1] == (byte) 5) {
				handleError(state, data);
			}
		}
	}

	private void handleAck(ClientState state, byte[] data) {

		AckPacket ackPacket = new AckPacket(data);

		LOG.debug("Got ACK packet for block nr '" + ackPacket.blockNumber + "' from '" + state.getRemote() + "'");
		int sblockNumber = state.getBlockNumber();

		if (ackPacket.blockNumber < sblockNumber - 1) {
			LOG.debug("AckPacket with old block# received (wanted '" + sblockNumber + "' but got '" + ackPacket.blockNumber + "').. but this will be ignored so as not to induce " + "Sorcerer's Apprentice bug");
		} else if (ackPacket.blockNumber == sblockNumber - 1) {
			if (state.getLastData().getLastSentAt() < System.currentTimeMillis() - (1000 * 5)) {

				// resend the last data packet.
				state.getLastData().send(this);

				LOG.debug("AckPacket with last block# received (wanted '" + sblockNumber + "' but got '" + ackPacket.blockNumber + "'). Since timeout has been reached I will resend the last packet.");
			} else {
				LOG.debug("AckPacket with last block# received (wanted '" + sblockNumber + "' but got '" + ackPacket.blockNumber + "').. but this will be ignored so as not to induce " + "Sorcerer's Apprentice bug");
			}
		} else if (ackPacket.blockNumber > sblockNumber) {
			closedownState(state);

			LOG.warn("Oops, AckPacket with unknown block# received (wanted '" + sblockNumber + "' but got '" + ackPacket.blockNumber + "').. this is totally strange, and I have dropped this " + "connection for now");
		} else {
			if (state.getState().equals(ClientState.DONE)) {
				closedownState(state);

				LOG.debug("Got last ACK packet (nr '" + ackPacket.blockNumber + "' from '" + state.getRemote() + "' for '" + state.getPacket().getFilename() + "' - closed down this connection.");
			} else {
				new DataPacket(state).send(this);

				// check if we're done after sending this datapacket.
				if (state.getState().equals(ClientState.DONE)) {
					try {
						state.getInputStream().close();
					} catch (IOException e) {
						LOG.warn("Couldn't close my inputStream", e);
					}
					LOG.debug("Sent last DataPacket nr '" + sblockNumber + "' of '" + state.getPacket() + "'");
				}

			}
		}
	}

	private void handleError(ClientState state, byte[] data) {
		ErrorPacket packet = new ErrorPacket(data);

		LOG.warn("" + state.getRemote() + " said: '" + packet.message + "' closed down session (after " + state.getBlockNumber() + " packets).");

		// closedown the state.
		closedownState(state);
	}

	private String getDefaultErrorMessage(int type) {
		if (type == 0) {
			return "Undefined";
		} else if (type == 1) {
			return "File not found";
		} else if (type == 2) {
			return "Access violation";
		} else if (type == 3) {
			return "Disk full or allocation exceeded";
		} else if (type == 4) {
			return "Illegal TFTP operation";
		} else if (type == 5) {
			return "Unknown transfer ID";
		} else if (type == 6) {
			return "File already exists";
		} else if (type == 7) {
			return "No such user";
		} else {
			LOG.warn("Strange, errormessage of type '" + type + "' is not known...");
			return null;
		}
	}

	private class ErrorPacket extends OutPacket {
		public int type;

		public String message;

		public ErrorPacket(byte[] data) {
			this.type = (int) data[3];
			this.message = ByteUtil.getString(data, 4);
		}

		public ErrorPacket(ClientState state, int type) {
			this(state, type, getDefaultErrorMessage(type));
		}

		public ErrorPacket(ClientState state, int type, String message) {
			super(state);
			this.type = type;
			this.message = message;
			byte[] byteMessage = null;
			try {
				byteMessage = message.getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e) {
				LOG.error("Huh? WTF? lol@n00b - you don't support the encoding US-ASCII... " + "this shouldn't ever happen...");
			}
			data = new byte[5 + byteMessage.length];
			Arrays.fill(data, (byte) 0);
			data[1] = (byte) 5;
			data[3] = (byte) type;
			for (int i = 0; i < byteMessage.length; i++) {
				data[4 + i] = byteMessage[i];
			}
		}
	}

	private class AckPacket {
		int blockNumber;

		public AckPacket(byte[] data) {
			// System.out.println(new String(data));
			int data2;
			int data3;
			data2 = (int) data[2];
			data3 = (int) data[3];
			if (data2 < 0) {
				data2 = data2 + 256;
			}
			if (data3 < 0) {
				data3 = data3 + 256;
			}
			blockNumber = (data2 << 8) + data3;
		}
	}

}
