package moa.streams.filters.privacy.rankswapping;


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
		this(0,0.0);
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
