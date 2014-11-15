package moa.streams.filters.privacy;


public class IndexValuePair implements Comparable<IndexValuePair> {
	
	public int index;
	
	public double value;
	
	/**
	 * Builds a new pair, with values:
	 * <ul>
	 *   <li>{@code index = 0}</li>
	 *   <li>{@code value = 0.0}</li>
	 * </ul>
	 */
	public IndexValuePair() {
		new IndexValuePair(0,0.0);
	}
	
	/**
	 * Builds a new pair with the given values.
	 * 
	 * @param index the index
	 * @param value the value
	 */
	public IndexValuePair(int index, double value){
		this.index = index;
		this.value = value;
	}
	
	/* TODO decide if this is important
	public boolean equals(Object o){
		if (o instanceof Pair)
			return true;
		else return false;
	}
	
	public String toString(){
		String st = "index: ";
		return st+index+" value: "+value;
	}*/

	@Override
	public int compareTo(IndexValuePair b) {
		if (this.value < b.value) {
			return 1;
		}
		else if (this.value == b.value)  {
			return 0;
		}
		else {
			return -1;
		}
	}

}
