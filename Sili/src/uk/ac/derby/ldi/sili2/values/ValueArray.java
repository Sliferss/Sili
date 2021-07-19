package uk.ac.derby.ldi.sili2.values;

public class ValueArray extends ValueAbstract {

	private Value[] internalValue;
	
	public ValueArray(Value[] b) {
		internalValue = b;
	}
	
	public String getName() {
		return "array";
	}
	
	public Value[] getValues() {
		return internalValue;
	}

	public Value getIndex(int index) {
		return internalValue[index];
	}
	
	public void setIndex(int index, Value v) {
		internalValue[index] = v;
	}
	
	public void addIndex(int index, Value v) {
		internalValue[index] = internalValue[index].add(v);
	}
	
	public void subIndex(int index, Value v) {
		internalValue[index] = internalValue[index].subtract(v);
	}
	
	public void multIndex(int index, Value v) {
		internalValue[index] = internalValue[index].mult(v);
	}
	
	public void divIndex(int index, Value v) {
		internalValue[index] = internalValue[index].div(v);
	}
	
	public int compare(int index, Value v) {
		if (internalValue[index].doubleValue() == v.doubleValue())
			return 0;
		else if (internalValue[index].doubleValue() > v.doubleValue())
			return 1;
		else
			return -1;
	}
}
