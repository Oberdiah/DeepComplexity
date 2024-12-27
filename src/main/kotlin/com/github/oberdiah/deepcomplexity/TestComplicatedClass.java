package com.github.oberdiah.deepcomplexity;

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
	 * Inputs: inpCountingUp, inpIncomingData.
	 * Outputs: outCountingUp, outPlayGround, $retVal.
	 * <p>
	 * outCountingUp = {
	 * ((inpCountingUp + 1) % 2 == 0) ? -(inpCountingUp + 1) : (inpCountingUp + 1)
	 * }
	 * outPlayGround = {
	 * ((inpCountingUp + 1) % 2 == 0) ? (inpCountingUp + 1) * inpIncomingData : (inpPlayGround)
	 * }
	 * $retVal = {
	 * outCountingUp > 5
	 * }
	 */
	public boolean doFoo(
			int incomingData
	) {
		countingUp++;
		
		if (countingUp % 2 == 0) {
			playGround = countingUp * incomingData;
			countingUp = -countingUp;
		}
		
		return countingUp > 5;
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
		
		for (int foo : nextList) {
			complexClass.doFoo(foo);
		}
		
		return complexClass.playGround;
	}
}
