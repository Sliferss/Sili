package uk.ac.derby.ldi.sili2.values;

public class ValueInteger extends ValueAbstract {

	private long internalValue;
	
	public ValueInteger(long b) {
		internalValue = b;
	}
	
	public String getName() {
		return "integer";
	}
	
	/** Convert this to a primitive long. */
	public long longValue() {
		return internalValue;
	}
	
	/** Convert this to a primitive double. */
	public double doubleValue() {
		return (double)internalValue;
	}
	
	/** Convert this to a primitive String. */
	public String stringValue() {
		return "" + internalValue;
	}

	public int compare(Value v) {
		if (internalValue == v.longValue())
			return 0;
		else if (internalValue > v.longValue())
			return 1;
		else
			return -1;
	}
	
	public Value add(Value v) {
		return new ValueInteger(internalValue + v.longValue());
	}

	public Value subtract(Value v) {
		return new ValueInteger(internalValue - v.longValue());
	}

	public Value mult(Value v) {
		return new ValueInteger(internalValue * v.longValue());
	}

	public Value div(Value v) {
		return new ValueInteger(internalValue / v.longValue());
	}
	
	public Value pow(Value v) {
		int i = (int)Math.pow(internalValue, v.longValue());
		return new ValueInteger(i);
	}
	
	public Value percent(Value v) {
		int i = (int)(internalValue * v.longValue()) / 100;
		return new ValueInteger(i);
	}
	
	public Value cos() {
		double i = Math.cos(Math.toRadians(internalValue));
		return new ValueInteger((int)i);
	}
	
	public Value sin() {
		double i = Math.sin(Math.toRadians(internalValue));
		return new ValueInteger((int)i);
	}
	
	public Value tan() {
		double i = Math.tan(Math.toRadians(internalValue));
		return new ValueInteger((int)i);
	}
	
	public Value icos() {
		double i = Math.toDegrees(Math.acos(internalValue));
		return new ValueInteger((int)i);
	}
	
	public Value isin() {
		double i = Math.toDegrees(Math.asin(internalValue));
		return new ValueInteger((int)i);
	}
	
	public Value itan() {
		double i = Math.toDegrees(Math.atan(internalValue));
		return new ValueInteger((int)i);
	}

	public Value unary_plus() {
		return new ValueInteger(internalValue);
	}

	public Value unary_minus() {
		return new ValueInteger(-internalValue);
	}
	
	public String toString() {
		return "" + internalValue;
	}
}
