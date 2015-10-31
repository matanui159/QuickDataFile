

import java.io.IOException;

import com.redmintie.quickdata.QuickDataFile;

public class Test {
	public static void main(String[] args) throws IOException {
		QuickDataFile file = new QuickDataFile("test.qdt");
		file.clear();
		file.saveString("test", "Hello, World!");
		file.saveString("hi", "test");
		file.saveString("test", "Hi!");
		file.saveInt("number", 21);
		file.saveDouble("pi", Math.PI);
		file.close();
	}
}