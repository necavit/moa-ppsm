package moa.streams.filters;


public class Pair implements Comparable<Pair>{
	public int index;
	public double value;
	public Pair(){
		new Pair(0,0);
	}
	
	public Pair(int index, double value){
		this.index = index;
		this.value = value;
	}
/*		
	public int compare(Pair a, Pair b){
		if (a.value < b.value)
			return 1;
		else if(a.value == b.value)
			return 0;
		else return -1;
	}
*/		
	public boolean equals(Object o){
		if (o instanceof Pair)
			return true;
		else return false;
	}
	
	public String toString(){
		String st = "index: ";
		return st+index+" value: "+value;
	}

	@Override
	public int compareTo(Pair b) {
		if (value < b.value)
			return 1;
		else if(value == b.value)
			return 0;
		else return -1;
	}

}
