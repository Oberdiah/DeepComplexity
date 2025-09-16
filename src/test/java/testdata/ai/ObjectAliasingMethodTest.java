package testdata.ai;

import com.github.oberdiah.deepcomplexity.RequiredScore;

public class ObjectAliasingMethodTest {
	// Testing basic aliasing with field modification
	@RequiredScore(1.0)
	public static short basicAliasing(short x) {
		AliasClass obj = new AliasClass(5);
		modifyBothRefs(obj, obj);
		return (short) obj.value;
	}
	
	private static void modifyBothRefs(AliasClass a, AliasClass b) {
		a.value = 10;
		b.value = 10;
	}
	
	// Testing aliasing with conditional modification
	@RequiredScore(1.0)
	public static short conditionalAliasing(short x) {
		AliasClass obj = new AliasClass(0);
		conditionalModify(obj, obj);
		return (short) obj.value;
	}
	
	private static void conditionalModify(AliasClass a, AliasClass b) {
		a.value = 10;
		if (b.value == 10) {
			a.value = 20;
		}
	}
	
	// Testing aliasing with method chaining
	public static short chainingAliasing(short x) {
		AliasClass obj = new AliasClass(5);
		chainedModify(obj, obj);
		return (short) obj.value;
	}
	
	private static void chainedModify(AliasClass a, AliasClass b) {
		a.increment();
		b.increment();
		a.add(8);
	}
	
	// Testing aliasing with return value dependency
	@RequiredScore(1.0)
	public static short returnAliasing(short x) {
		AliasClass obj = new AliasClass(0);
		return (short) modifyAndCheck(obj, obj);
	}
	
	private static int modifyAndCheck(AliasClass a, AliasClass b) {
		a.value = 5;
		return b.value == 5 ? 1 : 0;
	}
	
	// Testing aliasing with nested object references
	public static short nestedAliasing(short x) {
		Container c1 = new Container(new AliasClass(0));
		Container c2 = new Container(c1.inner);
		nestedModify(c1, c2);
		return (short) c1.inner.value;
	}
	
	private static void nestedModify(Container a, Container b) {
		a.inner.value = 21;
		b.inner.value = 42;
	}
	
	// Testing aliasing with array element modification
	public static short arrayAliasing(short x) {
		AliasClass[] arr = {new AliasClass(25)};
		arrayModify(arr[0], arr[0]);
		return (short) arr[0].value;
	}
	
	private static void arrayModify(AliasClass a, AliasClass b) {
		a.value = 50;
		b.value = 100;
	}
	
	// Testing aliasing with multiple field updates
	@RequiredScore(1.0)
	public static short multiFieldAliasing(short x) {
		MultiField obj = new MultiField(1, 2);
		multiFieldModify(obj, obj);
		return (short) obj.sum();
	}
	
	private static void multiFieldModify(MultiField a, MultiField b) {
		a.x = 3;
		b.y = 4;
	}
	
	// Testing aliasing with recursive method calls
	public static short recursiveAliasing(short x) {
		AliasClass obj = new AliasClass(1);
		recursiveModify(obj, obj, 3);
		return (short) obj.value;
	}
	
	private static void recursiveModify(AliasClass a, AliasClass b, int depth) {
		if (depth > 0) {
			a.value *= 2;
			recursiveModify(b, a, depth - 1);
		}
	}
	
	// Testing aliasing with object swapping attempt
	@RequiredScore(1.0)
	public static short swapAliasing(short x) {
		AliasClass obj = new AliasClass(0);
		attemptSwap(obj, obj);
		return (short) obj.value;
	}
	
	private static void attemptSwap(AliasClass a, AliasClass b) {
		a.value = 99;
		AliasClass temp = a;
		a = b;
		b = temp;
		a.value = 0;
	}
	
	// Testing aliasing with exception handling
	public static short exceptionAliasing(short x) {
		AliasClass obj = new AliasClass(0);
		try {
			exceptionModify(obj, obj);
		} catch (Exception e) {
			// Ignore
		}
		return (short) obj.value;
	}
	
	private static void exceptionModify(AliasClass a, AliasClass b) {
		a.value = 77;
		if (b.value == 77) {
			throw new RuntimeException();
		}
	}
	
	// Testing aliasing with static field interaction
	public static short staticAliasing(short x) {
		StaticField.reset();
		AliasClass obj = new AliasClass(0);
		staticModify(obj, obj);
		return (short) obj.value;
	}
	
	private static void staticModify(AliasClass a, AliasClass b) {
		StaticField.count = 55;
		a.value = StaticField.count;
		b.value = StaticField.count;
	}
	
	// Testing aliasing with method overloading
	@RequiredScore(1.0)
	public static short overloadAliasing(short x) {
		AliasClass obj = new AliasClass(10);
		overloadedModify(obj, obj, 20);
		return (short) obj.value;
	}
	
	private static void overloadedModify(AliasClass a, AliasClass b) {
		a.value = 5;
	}
	
	private static void overloadedModify(AliasClass a, AliasClass b, int extra) {
		a.value = extra;
		b.value = 30;
	}
	
	// Testing aliasing with interface implementation
	public static short interfaceAliasing(short x) {
		ModifiableInt obj = new AliasClass(0);
		interfaceModify(obj, obj);
		return (short) obj.getValue();
	}
	
	private static void interfaceModify(ModifiableInt a, ModifiableInt b) {
		a.setValue(100);
		if (b.getValue() == 100) {
			a.setValue(150);
		}
	}
	
	// Testing aliasing with synchronized methods
	@RequiredScore(1.0)
	public static short synchronizedAliasing(short x) {
		AliasClass obj = new AliasClass(44);
		synchronizedModify(obj, obj);
		return (short) obj.value;
	}
	
	private static synchronized void synchronizedModify(AliasClass a, AliasClass b) {
		int temp = a.value;
		a.value = temp + b.value;
	}
	
	// Testing aliasing with generic type parameters
	public static short genericAliasing(short x) {
		AliasClass obj = new AliasClass(100);
		genericModify(obj, obj);
		return (short) obj.value;
	}
	
	private static <T extends AliasClass> void genericModify(T a, T b) {
		a.value = 150;
		b.value = 200;
	}
	
	// Testing aliasing with collection operations
	public static short collectionAliasing(short x) {
		AliasClass obj = new AliasClass(1);
		java.util.List<AliasClass> list = new java.util.ArrayList<>();
		list.add(obj);
		collectionModify(obj, list.get(0));
		return (short) obj.value;
	}
	
	private static void collectionModify(AliasClass a, AliasClass b) {
		a.increment();
		b.increment();
	}
	
	// Testing aliasing with instanceof checks
	public static short instanceofAliasing(short x) {
		Object obj = new AliasClass(0);
		instanceofModify(obj, obj);
		return (short) ((AliasClass) obj).value;
	}
	
	private static void instanceofModify(Object a, Object b) {
		if (a instanceof AliasClass && b instanceof AliasClass) {
			((AliasClass) a).value = 333;
		}
	}
	
	// Testing aliasing with final parameter modification
	@RequiredScore(1.0)
	public static short finalAliasing(short x) {
		AliasClass obj = new AliasClass(0);
		finalModify(obj, obj);
		return (short) obj.value;
	}
	
	private static void finalModify(final AliasClass a, final AliasClass b) {
		a.value = 555;
		if (b.value == 555) {
			a.value = 777;
		}
	}
	
	// Testing aliasing with method reference behavior
	public static short methodRefAliasing(short x) {
		AliasClass obj = new AliasClass(0);
		methodRefModify(obj, obj);
		return (short) obj.value;
	}
	
	private static void methodRefModify(AliasClass a, AliasClass b) {
		Runnable r = () -> {
			a.value = 999;
			if (b.value == 999) {
				a.value = 999;
			}
		};
		r.run();
	}
	
	// Testing aliasing with constructor chaining
	@RequiredScore(1.0)
	public static short constructorAliasing(short x) {
		ChainedClass obj = new ChainedClass(0);
		constructorModify(obj.inner, obj.inner);
		return (short) obj.inner.value;
	}
	
	private static void constructorModify(AliasClass a, AliasClass b) {
		a.value = 111;
		if (b.value == 111) {
			a.value = 111;
		}
	}
	
	private static class AliasClass implements ModifiableInt {
		public int value;
		
		public AliasClass(int value) {
			this.value = value;
		}
		
		public void increment() {
			value++;
		}
		
		public void add(int x) {
			value += x;
		}
		
		@Override
		public int getValue() {
			return value;
		}
		
		@Override
		public void setValue(int value) {
			this.value = value;
		}
	}
	
	private static class Container {
		public AliasClass inner;
		
		public Container(AliasClass inner) {
			this.inner = inner;
		}
	}
	
	private static class MultiField {
		public int x, y;
		
		public MultiField(int x, int y) {
			this.x = x;
			this.y = y;
		}
		
		public int sum() {
			return x + y;
		}
	}
	
	private static class StaticField {
		public static int count = 0;
		
		public static void reset() {
			count = 0;
		}
	}
	
	private interface ModifiableInt {
		int getValue();
		
		void setValue(int value);
	}
	
	private static class ChainedClass {
		public AliasClass inner;
		
		public ChainedClass(int value) {
			this.inner = new AliasClass(value);
		}
	}
}