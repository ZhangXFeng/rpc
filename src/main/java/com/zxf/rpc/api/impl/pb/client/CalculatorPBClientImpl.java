package com.zxf.rpc.api.impl.pb.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import com.google.protobuf.ServiceException;
import com.zxf.rpc.api.Calculator;
import com.zxf.rpc.api.CalculatorPB;
import com.zxf.rpc.proto.CalculatorMsg.CalRequest;
import com.zxf.rpc.proto.CalculatorMsg.CalResponse;

public class CalculatorPBClientImpl implements Calculator {

	private CalculatorPB proxy;

	public CalculatorPBClientImpl() {

		proxy = (CalculatorPB) Proxy.newProxyInstance(
				CalculatorPB.class.getClassLoader(),
				new Class[] { CalculatorPB.class }, new Invoker());

	}

	public class Invoker implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			// TODO Auto-generated method stub
			Socket socket = new Socket("localhost", 8038);
			DataOutputStream out = new DataOutputStream(
					socket.getOutputStream());
			DataInputStream in = new DataInputStream(socket.getInputStream());

			byte[] bytes = ((CalRequest) args[1]).toByteArray();
			out.writeInt(bytes.length);
			out.write(bytes);
			out.flush();
			socket.shutdownOutput();
			// socket.close();
			int dataLen = in.readInt();
			byte[] data = new byte[dataLen];
			int count = in.read(data);
			CalResponse result = CalResponse.parseFrom(data);
			System.out.println(result);

			socket.close();
			return result;
		}

	}

	@Override
	public int add(int a, int b) {
		CalRequest.Builder builder = CalRequest.newBuilder();
		builder.setMethodName("add");
		builder.setNum1(a);
		builder.setNum2(b);
		CalRequest request = builder.build();
		CalResponse calResponse = null;
		try {
			calResponse = proxy.add(null, request);
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return calResponse.getResult();
	}

	@Override
	public int subtraction(int a, int b) {
		CalRequest.Builder builder = CalRequest.newBuilder();
		builder.setMethodName("Subtraction");
		builder.setNum1(a);
		builder.setNum2(b);
		CalRequest request = builder.build();
		CalResponse calResponse = null;
		try {
			calResponse = proxy.add(null, request);
		} catch (ServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return calResponse.getResult();
	}

}