package com.zxf.service;

import java.io.IOException;

import com.google.protobuf.BlockingService;
import com.zxf.rpc.api.Calculator;
import com.zxf.rpc.api.impl.pb.service.CalculatorPBServiceImpl;
import com.zxf.rpc.server.Server;

public class CalculatorService implements Calculator {

	private Server server;

	public void start() throws IOException {

		CalculatorPBServiceImpl calculatorPBServiceImpl = new CalculatorPBServiceImpl(
				this);
		BlockingService service = com.zxf.rpc.proto.Calculator.CalculatorService
				.newReflectiveBlockingService(calculatorPBServiceImpl);

		server = new Server(CalculatorPBServiceImpl.class.getInterfaces()[0],
				service, 8038);
		server.start();

	}

	@Override
	public int add(int a, int b) {
		return a + b;
	}

	@Override
	public int subtraction(int a, int b) {
		return a - b;
	}

	public static void main(String[] args) throws IOException {
		CalculatorService service = new CalculatorService();
		service.start();
	}
}