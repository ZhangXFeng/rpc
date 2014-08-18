package com.zxf.rpc.server;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.zxf.rpc.proto.Calculator.CalculatorService;
import com.zxf.rpc.proto.CalculatorMsg.CalRequest;
import com.zxf.rpc.proto.CalculatorMsg.CalResponse;
import com.google.protobuf.*;
import com.google.protobuf.Descriptors.MethodDescriptor;

public class Server {
	private Class<?> protocol;
	private BlockingService impl;
	private int port;
	private ServerSocket ss;
	private boolean running = true;
	private Map<Client, Connection> map = Collections
			.synchronizedMap(new HashMap<Client, Connection>());
	private Listener listener;
	private Responder responder;
	private ServerSocket serverSocket;
	private BlockingQueue<Call> calls = new LinkedBlockingQueue<Call>();

	public Server(Class<?> protocol, BlockingService protocolImpl, int port)
			throws IOException {
		this.protocol = protocol;
		this.impl = protocolImpl;
		this.port = port;
		InetSocketAddress address = new InetSocketAddress("localhost", port);
		ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(address);
		serverSocket = serverSocketChannel.socket();
		serverSocketChannel.configureBlocking(false);
		listener = new Listener(serverSocketChannel, 1);
		responder = new Responder();

	}

	public void start() {
		listener.start();
		responder.start();
	}

	public byte[] processOneRpc(byte[] data) throws Exception {
		CalRequest request = CalRequest.parseFrom(data);
		String methodName = request.getMethodName();
		MethodDescriptor methodDescriptor = impl.getDescriptorForType()
				.findMethodByName(methodName);
		Message response = impl.callBlockingMethod(methodDescriptor, null,
				request);
		return response.toByteArray();
	}

	private class Listener extends Thread {

		private ServerSocketChannel serverSocketChannel = null;
		private Random random = new Random();
		private Reader[] readers;
		private int currentReader;
		private Selector listenerSelector;

		public Listener(ServerSocketChannel channel, int numReader)
				throws IOException {
			serverSocketChannel = channel;
			listenerSelector = Selector.open();
			serverSocketChannel.register(listenerSelector,
					SelectionKey.OP_ACCEPT);
			readers = new Reader[numReader];
			for (int i = 0; i < numReader; i++) {
				readers[i] = new Reader();
				readers[i].start();
			}
		}

		@Override
		public void run() {
			while (running) {
				try {
					listenerSelector.select();
					Iterator<SelectionKey> iter = listenerSelector
							.selectedKeys().iterator();
					while (iter.hasNext()) {
						SelectionKey selectionKey = (SelectionKey) iter.next();
						iter.remove();
						if (selectionKey.isValid()) {

							if (selectionKey.isAcceptable()) {
								doAccept(selectionKey);
							}
						}
						selectionKey = null;
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		private void doAccept(SelectionKey key) throws IOException {
			SocketChannel channel = ((ServerSocketChannel) key.channel())
					.accept();
			channel.configureBlocking(false);

			Reader reader = getReader();
			reader.startAdd();
			SelectionKey key2 = reader.registerChannel(channel);
			Connection connection = new Connection(channel);
			key2.attach(connection);
			reader.finishAdd();

		}

		private synchronized Reader getReader() {
			return readers[(currentReader++) % readers.length];
		}

		private class Reader extends Thread {
			private Selector readSelector;
			private boolean adding;

			public Reader() throws IOException {
				readSelector = Selector.open();
			}

			@Override
			public void run() {
				while (running) {
					try {
						readSelector.select();
						synchronized (this) {
							while (adding) {
								this.wait(1000);
							}
						}

						Iterator<SelectionKey> iterator = readSelector
								.selectedKeys().iterator();
						while (iterator.hasNext()) {
							SelectionKey selectionKey = (SelectionKey) iterator
									.next();
							iterator.remove();
							if (selectionKey.isValid()) {

								if (selectionKey.isReadable()) {
									doRead(selectionKey);
								}
							}
							selectionKey = null;
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

			private void doRead(SelectionKey key) throws IOException {
				Connection connection = (Connection) key.attachment();
				int count = connection.readAndProcess();
				if (-1 == count) {
					key.cancel();
					System.out.println("===============================");
				}

			}

			public void startAdd() {
				adding = true;
				readSelector.wakeup();
			}

			public synchronized void finishAdd() {
				adding = false;
				this.notify();
			}

			public SelectionKey registerChannel(SocketChannel channel)
					throws IOException {
				return channel.register(readSelector, SelectionKey.OP_READ);
			}
		}
	}

	private class Call {
		private Connection connection;
		byte[] messageData;
		long time;
		Message message;

		public Call(Connection connection, Message message, long time) {
			super();
			this.connection = connection;
			this.message = message;
			this.time = time;
		}

		public byte[] getMessageData() {
			return messageData;
		}

		public Message getMessage() {
			return message;
		}

		public long getTime() {
			return time;
		}

	}

	private class Client {
		Socket clientSocket;
		InetAddress host;
		int port;
		private Connection connection;

		public Client(Socket socket) {
			this.clientSocket = socket;
			this.host = socket.getInetAddress();
			this.port = socket.getPort();

		}

		public InetAddress getHost() {
			return host;
		}

		public int getPort() {
			return port;
		}

		public Socket getClientSocket() {
			return clientSocket;
		}

		public void setConnection(Connection connection) {
			this.connection = connection;
		}

		public Connection getConnection() {
			return connection;
		}

		

	}

	private class Connection {

		private ByteBuffer length = ByteBuffer.allocate(4);
		private ByteBuffer data;
		private SocketChannel channel;

		public Connection(SocketChannel channel) {
			this.channel = channel;
		}

		int readAndProcess() throws IOException {
			while (true) {
				int count = -1;
				try {
					if (length.hasRemaining()) {
						count = channel.read(length);
						if (count < 0 || length.hasRemaining()) {
							return count;
						}
					}
					if (null == data) {
						data = ByteBuffer.allocate(length.getInt(0));
					}

					if (data.hasRemaining()) {
						count = channel.read(data);
					}
				} catch (IOException e) {
					return -1;
				}

				if (!data.hasRemaining()) {
					length.clear();
					CalRequest calRequest = CalRequest.parseFrom(data.array());
					data = null;
					Call call = new Call(this, calRequest,
							System.currentTimeMillis());
					try {
						calls.put(call);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}

				return count;
			}

		}
	}

	private class Handler extends Thread {
	}
	private class Responder extends Thread {
		@Override
		public void run() {

			while (running) {
				try {
					Call call = calls.take();
					System.out.println(call.getMessage());
					CalRequest request = (CalRequest) call.getMessage();
					MethodDescriptor methodDescriptor = CalculatorService
							.getDescriptor().findMethodByName(
									request.getMethodName());
					CalResponse response = (CalResponse) impl
							.callBlockingMethod(methodDescriptor, null, request);
					byte[] data = response.toByteArray();
					ByteBuffer length = ByteBuffer.allocate(4);
					ByteBuffer databuf = ByteBuffer.allocate(data.length);
					length.putInt(data.length);
					length.flip();
					databuf.put(data);
					databuf.flip();
					SocketChannel channel = call.connection.channel;
					channel.write(length);
					channel.write(databuf);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ServiceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}