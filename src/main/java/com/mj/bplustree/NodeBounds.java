package com.mj.bplustree;

import java.util.List;

public class NodeBounds {

	public List low ;
	public List high ;
	public int blockPointer;
	
	public NodeBounds(List low, List high, int b) {
		
		this.low = low ;
		this.high = high ;
		blockPointer = b ;
	}
	

}
