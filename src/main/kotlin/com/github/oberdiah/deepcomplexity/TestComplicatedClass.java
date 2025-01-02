package com.github.oberdiah.deepcomplexity;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

// Things happen in two stages.
// First, all methods build their own internal input -> output trees.
// Next, we traverse all built trees and apply the trees as we go to build
// all the inputs final expressions.

public class TestComplicatedClass {
	// Expected final expression tree:
	// countingUp = {
	//     0, {
	//         doFoo = { += 1 }
	//     }
	// }
	private static int countingUp = 0;
	
	private int playGround = 0;
	
	private final List<Integer> myList = new ArrayList<>();
	
	/**
	 * Start out:
	 * <p>
	 * Square brackets mean an unknown.
	 * <p>
	 * vars: {
	 * VAR[incomingData]
	 * }
	 * <p>
	 * Line 2:
	 * <p>
	 * vars: {
	 * incomingData = VAR[incomingData],
	 * bar = VAR[playGround]
	 * countingUp = VAR[countingUp] + 1
	 * }
	 * <p>
	 * Line 4:
	 * <p>
	 * vars: {
	 * incomingData = VAR[incomingData],
	 * bar = VAR[playGround],
	 * countingUp = VAR[countingUp] + 1
	 * playGround = (VAR[countingUp] + 1) * VAR[incomingData]
	 * }
	 */
	public boolean doFoo(int incomingData) {
		int foo = 0;
		int bar = playGround;
		countingUp += 1;
		
		if (countingUp > 2) { // countingUp % 2 == 0
			playGround = countingUp * incomingData;
			countingUp = -countingUp;
			bar += countingUp;
			foo += 3;
		} else if (playGround < 3) {
			foo += 4;
		} else {
			bar += 5;
		}
		
		return countingUp + bar * 2 + foo > 5;
	}
	
	public List<Integer> whatIsNext() {
		if (doFoo(0)) {
			myList.add(countingUp);
		}
		return myList;
	}
	
	public static int complicatedFn() {
		TestComplicatedClass complexClass = new TestComplicatedClass();
		
		for (int i = 0; i < 10; i++) {
			complexClass.whatIsNext();
		}
		
		List<Integer> nextList = complexClass.whatIsNext();
		
		Integer.getInteger("foo");
		
		for (int foo : nextList) {
			complexClass.doFoo(foo);
		}
		
		return complexClass.playGround;
	}
}
