package com.zxf;

import java.util.Random;

import com.zxf.rpc.api.Calculator;
import com.zxf.rpc.api.impl.pb.client.CalculatorPBClientImpl;

public class TestClient {
	public static void main(String[] args) {
		final Calculator calculator = new CalculatorPBClientImpl();
		Random random = new Random(100);
		int num1 = random.nextInt(100);
		int num2 = random.nextInt(100);
		for (int i = 0; i < 5; i++) {

			// System.out.println(num1+"+"+num2+"="+calculator.add(num1, num2));
			calculator.add(num1, num2);
			num1 = random.nextInt(100);
			num2 = random.nextInt(100);
		}
	}
}