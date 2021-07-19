package uk.ac.derby.ldi.sili2.values;

public class ValueRational extends ValueAbstract {

	private double internalValue;
	
	public ValueRational(double b) {
		internalValue = b;
	}
	
	public String getName() {
		return "rational";
	}
	
	/** Convert this to a primitive double. */
	public double doubleValue() {
		return (double)internalValue;
	}
	
	/** Convert this to a primitive long. */
	public long longValue() {
		return (int)internalValue;
	}
	
	
	/** Convert this to a primitive String. */
	public String stringValue() {
		return "" + internalValue;
	}

	public int compare(Value v) {
		if (internalValue == v.doubleValue())
			return 0;
		else if (internalValue > v.doubleValue())
			return 1;
		else
			return -1;
	}
	
	public Value add(Value v) {
		return new ValueRational(internalValue + v.doubleValue());
	}

	public Value subtract(Value v) {
		return new ValueRational(internalValue - v.doubleValue());
	}

	public Value mult(Value v) {
		return new ValueRational(internalValue * v.doubleValue());
	}

	public Value div(Value v) {
		return new ValueRational(internalValue / v.doubleValue());
	}
	
	public Value pow(Value v) {
		return new ValueRational(Math.pow(internalValue, v.doubleValue()));
	}
	
	public Value percent(Value v) {
		return new ValueRational((internalValue * v.doubleValue()) / 100);
	}
	
	public Value cos() {
		double i = Math.cos(Math.toRadians(internalValue));
		return new ValueRational(i);
	}
	
	public Value sin() {
		double i = Math.sin(Math.toRadians(internalValue));
		return new ValueRational(i);
	}
	
	public Value tan() {
		double i = Math.tan(Math.toRadians(internalValue));
		return new ValueRational(i);
	}
	
	public Value icos() {
		double i = Math.toDegrees(Math.acos(internalValue));
		return new ValueRational(i);
	}
	
	public Value isin() {
		double i = Math.toDegrees(Math.asin(internalValue));
		return new ValueRational(i);
	}
	
	public Value itan() {
		double i = Math.toDegrees(Math.atan(internalValue));
		return new ValueRational(i);
	}

	public Value unary_plus() {
		return new ValueRational(internalValue);
	}

	public Value unary_minus() {
		return new ValueRational(-internalValue);
	}
	
	public String toString() {
		return "" + internalValue;
	}
}
