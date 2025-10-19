package testdata.ai;

import com.github.oberdiah.deepcomplexity.RequiredScore;

import com.github.oberdiah.deepcomplexity.ExpectedExpressionSize;

public class ClassMemberModificationTest {
	// Testing field modification through method calls
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short fieldModification(short x) {
		TestClass obj = new TestClass(0);
		obj.setValue(10);
		return (short) obj.getValue();
	}
	
	// Testing field modification through direct access
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short directFieldAccess(short x) {
		TestClass obj = new TestClass(0);
		obj.value = 15;
		return (short) obj.value;
	}
	
	// Testing reference aliasing with field modification
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short referenceAliasing(short x) {
		TestClass obj1 = new TestClass(5);
		TestClass obj2 = obj1;
		obj2.value = 7;
		return (short) obj1.value;
	}
	
	// Testing method parameter modification by reference
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short parameterModification(short x) {
		TestClass obj = new TestClass(5);
		modifyObject(obj);
		return (short) obj.value;
	}
	
	// Testing multiple references to same object
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short multipleReferences(short x) {
		TestClass obj = new TestClass(8);
		TestClass ref1 = obj;
		TestClass ref2 = obj;
		ref1.value = 12;
		return (short) ref2.value;
	}
	
	// Testing field modification in constructor
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short constructorField(short x) {
		TestClass obj = new TestClass(25);
		return (short) obj.value;
	}
	
	// Testing chained method calls
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short chainedMethods(short x) {
		TestClass obj = new TestClass(0);
		return (short) obj.setValue(30).getValue();
	}
	
	// Testing static field modification
	public static short staticField(short x) {
		TestClass.staticValue = 40;
		return (short) TestClass.staticValue;
	}
	
	// Testing static method field access
	public static short staticMethod(short x) {
		TestClass.setStaticValue(50);
		return (short) TestClass.getStaticValue();
	}
	
	// Testing field modification through getter/setter
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short getterSetter(short x) {
		TestClass obj = new TestClass(0);
		obj.setValue(60);
		return (short) obj.getValue();
	}
	
	// Testing object field reassignment
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short fieldReassignment(short x) {
		TestClass obj = new TestClass(35);
		obj = new TestClass(70);
		return (short) obj.value;
	}
	
	// Testing nested object field modification
	public static short nestedObject(short x) {
		NestedClass outer = new NestedClass();
		outer.inner.value = 80;
		return (short) outer.inner.value;
	}
	
	// Testing method returning modified object
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short methodReturn(short x) {
		TestClass obj = new TestClass(45);
		TestClass result = modifyAndReturn(obj);
		return (short) result.value;
	}
	
	// Testing field modification in conditional
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short conditionalModification(short x) {
		TestClass obj = new TestClass(0);
		if (true) {
			obj.value = 100;
		}
		return (short) obj.value;
	}
	
	// Testing field modification in loop
	public static short loopModification(short x) {
		TestClass obj = new TestClass(0);
		for (int i = 0; i < 5; i++) {
			obj.value++;
		}
		return (short) obj.value;
	}
	
	// Testing array of objects field modification
	public static short arrayObject(short x) {
		TestClass[] arr = new TestClass[]{new TestClass(0)};
		arr[0].value = 110;
		return (short) arr[0].value;
	}
	
	// Testing method with multiple object parameters
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short multipleParameters(short x) {
		TestClass obj1 = new TestClass(10);
		TestClass obj2 = new TestClass(5);
		swapValues(obj1, obj2);
		return (short) obj1.value;
	}
	
	// Testing field modification through this reference
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short thisReference(short x) {
		TestClass obj = new TestClass(0);
		obj.setValueUsingThis(120);
		return (short) obj.value;
	}
	
	// Testing final field initialization
	@RequiredScore(1.0)
	@ExpectedExpressionSize(2)
	public static short finalField(short x) {
		FinalFieldClass obj = new FinalFieldClass(130);
		return (short) obj.finalValue;
	}
	
	// Testing field modification after method call
	@RequiredScore(1.0)
	@ExpectedExpressionSize(4)
	public static short postMethodModification(short x) {
		TestClass obj = new TestClass(2);
		incrementValue(obj);
		return (short) obj.value;
	}
	
	private static void modifyObject(TestClass obj) {
		obj.value = 20;
	}
	
	private static TestClass modifyAndReturn(TestClass obj) {
		obj.value = 90;
		return obj;
	}
	
	private static void swapValues(TestClass obj1, TestClass obj2) {
		int temp = obj1.value;
		obj1.value = obj2.value;
		obj2.value = temp;
	}
	
	private static void incrementValue(TestClass obj) {
		obj.value++;
	}
	
	private static class TestClass {
		public int value;
		public static int staticValue;
		
		public TestClass(int value) {
			this.value = value;
		}
		
		public TestClass setValue(int value) {
			this.value = value;
			return this;
		}
		
		public int getValue() {
			return this.value;
		}
		
		public void setValueUsingThis(int value) {
			this.value = value;
		}
		
		public static void setStaticValue(int value) {
			staticValue = value;
		}
		
		public static int getStaticValue() {
			return staticValue;
		}
	}
	
	private static class NestedClass {
		public TestClass inner = new TestClass(0);
	}
	
	private static class FinalFieldClass {
		public final int finalValue;
		
		public FinalFieldClass(int value) {
			this.finalValue = value;
		}
	}
}