package testdata.ai;

public class MutableParameterSideEffectTest {
	// Testing method with side effect on array parameter
	public static short arraySideEffect(short x) {
		int[] arr = {0};
		modifyArray(arr);
		return (short) arr[0];
	}
	
	private static void modifyArray(int[] arr) {
		arr[0] = 10;
	}
	
	// Testing method with side effect on object field
	public static short objectFieldSideEffect(short x) {
		Counter c = new Counter(3);
		incrementCounter(c);
		return (short) c.value;
	}
	
	private static void incrementCounter(Counter c) {
		c.value += 4;
	}
	
	private static class Counter {
		int value;
		
		Counter(int value) {
			this.value = value;
		}
	}
	
	// Testing multiple side effects on same object
	public static short multipleSideEffects(short x) {
		Holder h = new Holder(5);
		addFive(h);
		addFive(h);
		return (short) h.data;
	}
	
	private static void addFive(Holder h) {
		h.data += 5;
	}
	
	private static class Holder {
		int data;
		
		Holder(int data) {
			this.data = data;
		}
	}
	
	// Testing side effect through aliased parameters
	public static short aliasedParameterSideEffect(short x) {
		Box box = new Box(10);
		doubleValue(box, box);
		return (short) box.val;
	}
	
	private static void doubleValue(Box a, Box b) {
		a.val = b.val * 2;
	}
	
	private static class Box {
		int val;
		
		Box(int val) {
			this.val = val;
		}
	}
	
	// Testing conditional side effect
	public static short conditionalSideEffect(short x) {
		Value v = new Value(2);
		conditionalModify(v, true);
		return (short) v.num;
	}
	
	private static void conditionalModify(Value v, boolean condition) {
		if (condition) {
			v.num = 8;
		}
	}
	
	private static class Value {
		int num;
		
		Value(int num) {
			this.num = num;
		}
	}
	
	// Testing side effect in nested method calls
	public static short nestedSideEffect(short x) {
		Item item = new Item(4);
		outerMethod(item);
		return (short) item.amount;
	}
	
	private static void outerMethod(Item item) {
		innerMethod(item);
	}
	
	private static void innerMethod(Item item) {
		item.amount = 12;
	}
	
	private static class Item {
		int amount;
		
		Item(int amount) {
			this.amount = amount;
		}
	}
	
	// Testing side effect with return value ignored
	public static short ignoredReturnSideEffect(short x) {
		Data data = new Data(5);
		modifyAndReturn(data);
		return (short) data.field;
	}
	
	private static int modifyAndReturn(Data data) {
		data.field = 25;
		return data.field;
	}
	
	private static class Data {
		int field;
		
		Data(int field) {
			this.field = field;
		}
	}
	
	// Testing side effect on array element
	public static short arrayElementSideEffect(short x) {
		int[] nums = {1, 2, 3};
		setFirstElement(nums);
		return (short) nums[0];
	}
	
	private static void setFirstElement(int[] arr) {
		arr[0] = 99;
	}
	
	// Testing side effect through multiple object references
	public static short multipleReferenceSideEffect(short x) {
		Node node = new Node(6);
		Node alias = node;
		modifyNode(alias);
		return (short) node.id;
	}
	
	private static void modifyNode(Node n) {
		n.id = 30;
	}
	
	private static class Node {
		int id;
		
		Node(int id) {
			this.id = id;
		}
	}
	
	// Testing side effect with exception path
	public static short exceptionPathSideEffect(short x) {
		Store store = new Store(7);
		try {
			riskyModify(store);
		} catch (RuntimeException e) {
			// ignore
		}
		return (short) store.stock;
	}
	
	private static void riskyModify(Store store) {
		store.stock = 42;
	}
	
	private static class Store {
		int stock;
		
		Store(int stock) {
			this.stock = stock;
		}
	}
	
	// Testing side effect in recursive method
	public static short recursiveSideEffect(short x) {
		Accumulator acc = new Accumulator(2);
		recursiveAdd(acc, 2);
		return (short) acc.total;
	}
	
	private static void recursiveAdd(Accumulator acc, int times) {
		if (times > 0) {
			acc.total += 3;
			recursiveAdd(acc, times - 1);
		}
	}
	
	private static class Accumulator {
		int total;
		
		Accumulator(int total) {
			this.total = total;
		}
	}
	
	// Testing side effect with method chaining pattern
	public static short methodChainingSideEffect(short x) {
		Builder builder = new Builder(10);
		process(builder);
		return (short) builder.result;
	}
	
	private static void process(Builder b) {
		b.multiply(5);
	}
	
	private static class Builder {
		int result;
		
		Builder(int result) {
			this.result = result;
		}
		
		void multiply(int factor) {
			this.result *= factor;
		}
	}
	
	// Testing side effect on shared mutable state
	public static short sharedStateSideEffect(short x) {
		Shared shared = new Shared(11);
		modifyShared1(shared);
		modifyShared2(shared);
		return (short) shared.value;
	}
	
	private static void modifyShared1(Shared s) {
		s.value = 22;
	}
	
	private static void modifyShared2(Shared s) {
		s.value = 77;
	}
	
	private static class Shared {
		int value;
		
		Shared(int value) {
			this.value = value;
		}
	}
	
	// Testing side effect with loop modification
	public static short loopSideEffect(short x) {
		Counter counter = new Counter(3);
		loopModify(counter);
		return (short) counter.value;
	}
	
	private static void loopModify(Counter c) {
		for (int i = 0; i < 3; i++) {
			c.value += 4;
		}
	}
	
	// Testing side effect with static method call
	public static short staticMethodSideEffect(short x) {
		Container container = new Container(8);
		StaticModifier.change(container);
		return (short) container.content;
	}
	
	private static class StaticModifier {
		static void change(Container c) {
			c.content = 88;
		}
	}
	
	private static class Container {
		int content;
		
		Container(int content) {
			this.content = content;
		}
	}
	
	// Testing side effect with parameter reassignment not affecting original
	public static short parameterReassignment(short x) {
		Reference ref = new Reference(9);
		tryReassign(ref);
		return (short) ref.data;
	}
	
	private static void tryReassign(Reference r) {
		r = new Reference(100);
	}
	
	private static class Reference {
		int data;
		
		Reference(int data) {
			this.data = data;
		}
	}
	
	// Testing side effect through getter method modification
	public static short getterSideEffect(short x) {
		LazyValue lazy = new LazyValue(13);
		lazy.getValue();
		return (short) lazy.cachedValue;
	}
	
	private static class LazyValue {
		int cachedValue;
		
		LazyValue(int initial) {
			this.cachedValue = initial;
		}
		
		int getValue() {
			this.cachedValue = 33;
			return cachedValue;
		}
	}
	
	// Testing complex side effect with multiple parameters
	public static short complexSideEffect(short x) {
		Pair pair = new Pair(14, 15);
		swapAndSum(pair);
		return (short) pair.first;
	}
	
	private static void swapAndSum(Pair p) {
		int temp = p.first;
		p.first = p.second + 45;
		p.second = temp;
	}
	
	private static class Pair {
		int first, second;
		
		Pair(int first, int second) {
			this.first = first;
			this.second = second;
		}
	}
	
	// Testing side effect with early return
	public static short earlyReturnSideEffect(short x) {
		Mutable mut = new Mutable(16);
		earlyReturnModify(mut, false);
		return (short) mut.val;
	}
	
	private static void earlyReturnModify(Mutable m, boolean flag) {
		if (!flag) {
			return;
		}
		m.val = 200;
	}
	
	private static class Mutable {
		int val;
		
		Mutable(int val) {
			this.val = val;
		}
	}
	
	// Testing side effect with interface implementation
	public static short interfaceSideEffect(short x) {
		ModifiableImpl impl = new ModifiableImpl(17);
		processModifiable(impl);
		return (short) impl.state;
	}
	
	private static void processModifiable(Modifiable m) {
		m.modify();
	}
	
	private interface Modifiable {
		void modify();
	}
	
	private static class ModifiableImpl implements Modifiable {
		int state;
		
		ModifiableImpl(int state) {
			this.state = state;
		}
		
		public void modify() {
			this.state = 75;
		}
	}
}