package testdata.ai;

import com.oberdiah.deepcomplexity.ExpectedExpressionSize;
import com.oberdiah.deepcomplexity.RequiredScore;

public class EarlyReturnMethodCallInteractionTest {
	// Testing early return with no method calls
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short earlyReturnNoCalls(short x) {
		if (x > 0) {
			return 1;
		}
		methodWithSideEffect();
		return 0;
	}
	
	private static void methodWithSideEffect() {
		// This should not be reached when x > 0
	}
	
	// Testing early return bypassing method call with side effects
	@RequiredScore(1.0)
	@ExpectedExpressionSize(11)
	public static short earlyReturnBypassCall(short x) {
		int result = 5;
		if (x < 0) {
			return (short) result;
		}
		result = addFive(result);
		return (short) result;
	}
	
	private static int addFive(int val) {
		return val + 5;
	}
	
	// Testing multiple early returns with different method calls
	@RequiredScore(1.0)
	@ExpectedExpressionSize(16)
	public static short multipleEarlyReturns(short x) {
		if (x == 0) {
			return (short) getValue(1);
		}
		if (x > 0) {
			return (short) getValue(2);
		}
		return (short) getValue(3);
	}
	
	private static int getValue(int x) {
		return x;
	}
	
	// Testing early return in loop with method calls
	public static short earlyReturnInLoop(short x) {
		for (int i = 0; i < 3; i++) {
			if (shouldReturn(i)) {
				return 1;
			}
		}
		return 0;
	}
	
	private static boolean shouldReturn(int i) {
		return i == 1;
	}
	
	// Testing early return with recursive method call
	@RequiredScore(1.0)
	@ExpectedExpressionSize(27)
	public static short earlyReturnRecursive(short x) {
		if (x <= 0) {
			return 0;
		}
		if (x == 1) {
			return 3;
		}
		return recursiveMethod((short) (x - 1));
	}
	
	private static short recursiveMethod(short n) {
		if (n <= 0) return 0;
		return 3;
	}
	
	// Testing early return with method that modifies global state
	public static short earlyReturnGlobalState(short x) {
		counter = 1;
		if (x == 0) {
			return (short) counter;
		}
		incrementCounter();
		return (short) counter;
	}
	
	private static int counter;
	
	private static void incrementCounter() {
		counter++;
	}
	
	// Testing early return with exception-throwing method
	public static short earlyReturnException(short x) {
		if (x > 100) {
			return 42;
		}
		try {
			throwException();
			return 1;
		} catch (Exception e) {
			return 0;
		}
	}
	
	private static void throwException() throws Exception {
		throw new Exception();
	}
	
	// Testing early return with method call in condition
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short earlyReturnMethodInCondition(short x) {
		if (isPositive(x)) {
			return 100;
		}
		return 50;
	}
	
	private static boolean isPositive(short x) {
		return x > 0;
	}
	
	// Testing early return with chained method calls
	public static short earlyReturnChainedCalls(short x) {
		if (x < 0) {
			return (short) chain().getValue().doubleValue();
		}
		return (short) chain().getValue().doubleValue();
	}
	
	private static ChainClass chain() {
		return new ChainClass();
	}
	
	private static class ChainClass {
		public ChainClass getValue() {
			return this;
		}
		
		public int doubleValue() {
			return 30;
		}
	}
	
	// Testing early return with static method calls
	public static short earlyReturnStaticCall(short x) {
		if (x == 0) {
			return (short) StaticHelper.getValue();
		}
		return (short) StaticHelper.getDoubleValue();
	}
	
	private static class StaticHelper {
		public static int getValue() {
			return 7;
		}
		
		public static int getDoubleValue() {
			return 14;
		}
	}
	
	// Testing early return with method modifying parameter object
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short earlyReturnModifyObject(short x) {
		TestObject obj = new TestObject(1);
		if (x < 0) {
			return (short) obj.value;
		}
		modifyObject(obj);
		return (short) obj.value;
	}
	
	private static void modifyObject(TestObject obj) {
		obj.value = 2;
	}
	
	private static class TestObject {
		int value;
		
		TestObject(int value) {
			this.value = value;
		}
	}
	
	// Testing early return with method call returning different types
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short earlyReturnDifferentTypes(short x) {
		if (x > 0) {
			return (short) (getBoolean() ? 1 : 0);
		}
		return (short) getInteger();
	}
	
	private static boolean getBoolean() {
		return true;
	}
	
	private static int getInteger() {
		return 0;
	}
	
	// Testing early return with method call in finally block
	public static short earlyReturnFinally(short x) {
		try {
			if (x > 0) {
				return 5;
			}
			return 0;
		} finally {
			setValue(10);
		}
	}
	
	private static int value;
	
	private static void setValue(int v) {
		value = v;
	}
	
	// Testing early return with method call that has early return
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short earlyReturnNested(short x) {
		if (x == 0) {
			return (short) methodWithEarlyReturn(1);
		}
		return (short) methodWithEarlyReturn(2);
	}
	
	private static int methodWithEarlyReturn(int x) {
		if (x == 1) return 3;
		return 6;
	}
	
	// Testing early return with constructor call
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short earlyReturnConstructor(short x) {
		if (x < 0) {
			return (short) new ValueClass(5).getValue();
		}
		return (short) new ValueClass(10).getValue();
	}
	
	private static class ValueClass {
		private int value;
		
		public ValueClass(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	}
	
	// Testing early return with method call inside ternary
	@RequiredScore(1.0)
	@ExpectedExpressionSize(9)
	public static short earlyReturnTernary(short x) {
		if (x == 0) {
			return 1;
		}
		return (short) (x > 0 ? getPositiveValue() : getNegativeValue());
	}
	
	private static int getPositiveValue() {
		return 2;
	}
	
	private static int getNegativeValue() {
		return 2;
	}
	
	// Testing early return with void method call affecting return value
	public static short earlyReturnVoidMethod(short x) {
		state = 0;
		if (x > 0) {
			return (short) state;
		}
		changeState();
		return (short) state;
	}
	
	private static int state;
	
	private static void changeState() {
		state = 1;
	}
	
	// Testing early return with method call in switch statement
	public static short earlyReturnSwitch(short x) {
		switch (x) {
			case 0:
				return 1;
			case 1:
				return (short) getSwitchValue();
			default:
				return (short) getSwitchValue();
		}
	}
	
	private static int getSwitchValue() {
		return 2;
	}
	
	// Testing early return with overloaded method calls
	@RequiredScore(1.0)
	@ExpectedExpressionSize(13)
	public static short earlyReturnOverload(short x) {
		if (x == 0) {
			return (short) overloadedMethod(5);
		}
		return (short) overloadedMethod(5, 5);
	}
	
	private static int overloadedMethod(int a) {
		return a;
	}
	
	private static int overloadedMethod(int a, int b) {
		return a + b;
	}
	
	// Testing early return with method call using this reference
	public static short earlyReturnThis(short x) {
		InstanceClass instance = new InstanceClass();
		if (x > 0) {
			return (short) instance.getValueEarly();
		}
		return (short) instance.getValueLate();
	}
	
	private static class InstanceClass {
		public int getValueEarly() {
			return 3;
		}
		
		public int getValueLate() {
			return 6;
		}
	}
}