package com.zxf.rpc.api.impl.pb.service;

import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import com.zxf.rpc.api.Calculator;
import com.zxf.rpc.api.CalculatorPB;
import com.zxf.rpc.proto.CalculatorMsg.CalRequest;
import com.zxf.rpc.proto.CalculatorMsg.CalResponse;

public class CalculatorPBServiceImpl implements CalculatorPB {
	private Calculator real;

	public CalculatorPBServiceImpl(Calculator calculator) {
		this.real = calculator;
	}

	@Override
	public CalResponse add(RpcController controller, CalRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		CalResponse.Builder builder = CalResponse.newBuilder();
		int num1 = request.getNum1();
		int num2 = request.getNum2();
		int result = real.add(num1, num2);
		builder.setResult(result);

		return builder.build();
	}

	@Override
	public CalResponse subtraction(RpcController controller, CalRequest request)
			throws ServiceException {
		// TODO Auto-generated method stub
		CalResponse.Builder builder = CalResponse.newBuilder();
		int num1 = request.getNum1();
		int num2 = request.getNum2();
		int result = real.subtraction(num1, num2);
		builder.setResult(result);

		return builder.build();
	}

}