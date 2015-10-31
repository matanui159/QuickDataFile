package com.redmintie.quickdata;

import java.io.IOException;

public class Main {
	public static void main(String[] args) throws IOException {
		QuickDataFile file = new QuickDataFile("test.qdt");
		file.storeString("test", "Hello, World!");
		file.storeString("hi", "test");
		file.storeString("test", "Hi!");
		file.storeInt("number", 21);
		file.storeDouble("pi", Math.PI);
		file.close();
	}
}