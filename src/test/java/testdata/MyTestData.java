package testdata;

public class MyTestData {
	public static void test1(int incoming) {
		int outgoing = 0;
		if (incoming < 3) {
			outgoing = 1;
		} else {
			outgoing = 2;
		}
	}
}
