package uk.ac.derby.ldi.sili2.interpreter;

import uk.ac.derby.ldi.sili2.parser.ast.*;
import uk.ac.derby.ldi.sili2.values.*;

public class Parser implements SiliVisitor {
	
	// Scope display handler
	private Display scope = new Display();
	
	// Get the ith child of a given node.
	private static SimpleNode getChild(SimpleNode node, int childIndex) {
		return (SimpleNode)node.jjtGetChild(childIndex);
	}
	
	// Get the token value of the ith child of a given node.
	private static String getTokenOfChild(SimpleNode node, int childIndex) {
		return getChild(node, childIndex).tokenValue;
	}
	
	// Execute a given child of the given node
	private Object doChild(SimpleNode node, int childIndex, Object data) {
		return node.jjtGetChild(childIndex).jjtAccept(this, data);
	}
	
	// Execute a given child of a given node, and return its value as a Value.
	// This is used by the expression evaluation nodes.
	Value doChild(SimpleNode node, int childIndex) {
		return (Value)doChild(node, childIndex, null);
	}
	
	// Execute all children of the given node
	Object doChildren(SimpleNode node, Object data) {
		return node.childrenAccept(this, data);
	}
	
	// Called if one of the following methods is missing...
	public Object visit(SimpleNode node, Object data) {
		System.out.println(node + ": acceptor not implemented in subclass?");
		return data;
	}
	
	// Execute a Sili program
	public Object visit(ASTCode node, Object data) {
		return doChildren(node, data);	
	}
	
	// Execute a statement
	public Object visit(ASTStatement node, Object data) {
		return doChildren(node, data);	
	}

	// Execute a block
	public Object visit(ASTBlock node, Object data) {
		return doChildren(node, data);	
	}

	// Function definition
	public Object visit(ASTFnDef node, Object data) {
		// Already defined?
		if (node.optimised != null)
			return data;
		// Child 0 - identifier (fn name)
		String fnname = getTokenOfChild(node, 0);
		if (scope.findFunctionInCurrentLevel(fnname) != null)
			throw new ExceptionSemantic("Function " + fnname + " already exists.");
		FunctionDefinition currentFunctionDefinition = new FunctionDefinition(fnname, scope.getLevel() + 1);
		// Child 1 - function definition parameter list
		doChild(node, 1, currentFunctionDefinition);
		// Add to available functions
		scope.addFunction(currentFunctionDefinition);
		// Child 2 - function body
		currentFunctionDefinition.setFunctionBody(getChild(node, 2));
		// Child 3 - optional return expression
		if (node.fnHasReturn)
			currentFunctionDefinition.setFunctionReturnExpression(getChild(node, 3));
		// Preserve this definition for future reference, and so we don't define
		// it every time this node is processed.
		node.optimised = currentFunctionDefinition;
		return data;
	}
	
	// Function definition parameter list
	public Object visit(ASTParmlist node, Object data) {
		FunctionDefinition currentDefinition = (FunctionDefinition)data;
		for (int i=0; i<node.jjtGetNumChildren(); i++)
			currentDefinition.defineParameter(getTokenOfChild(node, i));
		return data;
	}
	
	// Function body
	public Object visit(ASTFnBody node, Object data) {
		return doChildren(node, data);
	}
	
	// Function return expression
	public Object visit(ASTReturnExpression node, Object data) {
		return doChildren(node, data);
	}
	
	// Function call
	public Object visit(ASTCall node, Object data) {
		FunctionDefinition fndef;
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			String fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
			if (fndef == null)
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			// Save it for next time
			node.optimised = fndef;
		} else
			fndef = (FunctionDefinition)node.optimised;
		FunctionInvocation newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		scope.execute(newInvocation, this);
		return data;
	}
	
	// Function invocation in an expression
	public Object visit(ASTFnInvoke node, Object data) {
		FunctionDefinition fndef;
		if (node.optimised == null) { 
			// Child 0 - identifier (fn name)
			String fnname = getTokenOfChild(node, 0);
			fndef = scope.findFunction(fnname);
			if (fndef == null)
				throw new ExceptionSemantic("Function " + fnname + " is undefined.");
			if (!fndef.hasReturn())
				throw new ExceptionSemantic("Function " + fnname + " is being invoked in an expression but does not have a return value.");
			// Save it for next time
			node.optimised = fndef;
		} else
			fndef = (FunctionDefinition)node.optimised;
		FunctionInvocation newInvocation = new FunctionInvocation(fndef);
		// Child 1 - arglist
		doChild(node, 1, newInvocation);
		// Execute
		return scope.execute(newInvocation, this);
	}
	
	// Function invocation argument list.
	public Object visit(ASTArgList node, Object data) {
		FunctionInvocation newInvocation = (FunctionInvocation)data;
		for (int i=0; i<node.jjtGetNumChildren(); i++)
			newInvocation.setArgument(doChild(node, i));
		newInvocation.checkArgumentCount();
		return data;
	}
	
	// Execute an IF 
	public Object visit(ASTIfStatement node, Object data) {
		// evaluate boolean expression
		Value hopefullyValueBoolean = doChild(node, 0);
		if (!(hopefullyValueBoolean instanceof ValueBoolean))
			throw new ExceptionSemantic("The test expression of an if statement must be boolean.");
		if (((ValueBoolean)hopefullyValueBoolean).booleanValue())
			doChild(node, 1);							// if(true), therefore do 'if' statement
		else if (node.ifHasElse)						// does it have an else statement?
			doChild(node, 2);							// if(false), therefore do 'else' statement
		return data;
	}
	
	// Execute a FOR loop
	public Object visit(ASTFor node, Object data) {
		// loop initialisation
		doChild(node, 0);
		while (true) {
			// evaluate loop test
			Value hopefullyValueBoolean = doChild(node, 1);
			if (!(hopefullyValueBoolean instanceof ValueBoolean))
				throw new ExceptionSemantic("The test expression of a for loop must be boolean.");
			if (!((ValueBoolean)hopefullyValueBoolean).booleanValue())
				break;
			// do loop statement
			doChild(node, 3);
			// assign loop increment
			doChild(node, 2);
		}
		return data;
	}
	
	//duadratic equation
	public Object visit(ASTQuad node, Object data)
	{
		double a = doChild(node, 0).doubleValue();
		double b = doChild(node, 1).doubleValue();
		double c = doChild(node, 2).doubleValue();
		double positive = (-b + Math.pow((Math.pow(b, 2) -4 * a *c), 0.5)) / (2 * a);
		double negative = (-b - Math.pow((Math.pow(b, 2) -4 * a *c), 0.5)) / (2 * a);
		System.out.println("Quadratic of +X = " + positive + " Quadratic of -X = " + negative);
		return data;
	}
	
	//line equation
	public Object visit(ASTLine node, Object data)
	{
		double x1 = doChild(node, 0).doubleValue();
		double y1 = doChild(node, 1).doubleValue();
		double x2 = doChild(node, 2).doubleValue();
		double y2 = doChild(node, 3).doubleValue();
		double grad = (y1 - y2) / (x1 -x2);
		System.out.println("Gradient = " + grad);
		double intercept = y1 - (grad * x1);
		System.out.println("Line equation y = " + grad + "x + " + intercept);
		return data;
	}
	
	//speed equation
	public Object visit(ASTSpeed node, Object data)
	{
		double distance = doChild(node, 0).doubleValue();
		double time = doChild(node, 1).doubleValue();
		System.out.println("Speed = " + (distance / time) + "units/s");
		return data;
	}
	
	//time equation
	public Object visit(ASTTime node, Object data)
	{
		double distance = doChild(node, 0).doubleValue();
		double speed = doChild(node, 1).doubleValue();
		System.out.println("Time = " + (distance / speed) + " seconds");
		return data;
	}
	
	//distance equation
	public Object visit(ASTDistance node, Object data)
	{
		double speed = doChild(node, 0).doubleValue();
		double time = doChild(node, 1).doubleValue();
		System.out.println("Distance = " + (speed * time) + "units");
		return data;
	}
		
	
	// While loop
	public Object visit(ASTWhile node, Object data) {
		while (true) {
			//evaluate the loop
			Value hopefullyValueBoolean = doChild(node, 0);
			if (!((ValueBoolean)hopefullyValueBoolean).booleanValue())
				break;
			doChild(node, 1);
		}
		return data;
	}
	
	// Loop x number of times
	public Object visit(ASTLoopX node, Object data) {
		double count = 0;
		while (count != doChild(node, 0).doubleValue()) {
			doChild(node, 1); //do statement
			count = count + 1;
		}
		return data;
	}
	
	// Find Y from line equation
	public Object visit(ASTPointY node, Object data) {
		double m = doChild(node, 0).doubleValue();
		double x = doChild(node, 1).doubleValue();
		double intercept = doChild(node, 2).doubleValue();
		double y = (m * x) + intercept;
		System.out.println("Y = " + y);
		return y;
	}
	
	// Find X from line equation
	public Object visit(ASTPointX node, Object data) {
		double m = doChild(node, 0).doubleValue();
		double y = doChild(node, 1).doubleValue();
		double intercept = doChild(node, 2).doubleValue();
		double x = (y - intercept) / m;
		System.out.println("X = " + x);
		return x;
	}
		
	
	// Process an identifier
	// This doesn't do anything, but needs to be here because we need an ASTIdentifier node.
	public Object visit(ASTIdentifier node, Object data) {
		return data;
	}
	
	// Execute the TYPE statement
	public Object visit(ASTType node, Object data) {
		System.out.println(doChild(node, 0));
		return data;
	}
	
	// Execute the TYPEREPEAT statement
	public Object visit(ASTTypeRepeat node, Object data) {
		long count = 0;
		while(count != doChild(node, 1).longValue())
		{
			System.out.println(doChild(node, 0));
			doChild(node, 2);
			count++;
		}
		return data;
	}
	
	// Dereference a variable or parameter, and return its value.
	public Object visit(ASTDereference node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = node.tokenValue;
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		return reference.getValue();
	}
	
	// Function definition parameter list
	public Object visit(ASTArrayList node, Object data) {
		Value[] values = new Value[node.jjtGetNumChildren()];
		for (int i = 0; i < node.jjtGetNumChildren(); i++) {
			values[i] = doChild(node, i);			
		}
		
		ValueArray arr = new ValueArray(values);
		node.optimised = arr;
		return node.optimised;
	}
	
	public Object visit(ASTArrayIndex node, Object data) {
		Value index = doChild(node, 1);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		ValueArray arr = (ValueArray)reference.getValue();
		Value item = arr.getIndex((int)index.longValue());
		
		return item;
	}
	
	
	// Execute an assignment statement.
	public Object visit(ASTAssignment node, Object data) {
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				reference = scope.defineVariable(name);
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		reference.setValue(doChild(node, 1));
		return data;
	}
	
	// Execute an assignment increment statement.
		public Object visit(ASTAssignmentIncrement node, Object data) {
			Display.Reference reference;
			if (node.optimised == null) {
				String name = getTokenOfChild(node, 0);
				reference = scope.findReference(name);
				if (reference == null)
					reference = scope.defineVariable(name);
				node.optimised = reference;
			} else
				reference = (Display.Reference)node.optimised;
			reference.setValue(reference.getValue().add(doChild(node, 1)));
			return data;
		}
		
	// Execute an assignment mul statement.
		public Object visit(ASTAssignmentMul node, Object data) {
			Display.Reference reference;
			if (node.optimised == null) {
				String name = getTokenOfChild(node, 0);
				reference = scope.findReference(name);
				if (reference == null)
					reference = scope.defineVariable(name);
				node.optimised = reference;
			} else
				reference = (Display.Reference)node.optimised;
			reference.setValue(reference.getValue().mult(doChild(node, 1)));
			return data;
		}
				
	// Execute an assignment div statement.
		public Object visit(ASTAssignmentDiv node, Object data) {
			Display.Reference reference;
			if (node.optimised == null) {
				String name = getTokenOfChild(node, 0);
				reference = scope.findReference(name);
				if (reference == null)
					reference = scope.defineVariable(name);
				node.optimised = reference;
			} else
				reference = (Display.Reference)node.optimised;
			reference.setValue(reference.getValue().div(doChild(node, 1)));
			return data;
		}
		
	// Execute an assignment increment statement solo.
		public Object visit(ASTAssignmentIncrementSolo node, Object data) {
			Display.Reference reference;
			if (node.optimised == null) {
				String name = getTokenOfChild(node, 0);
				reference = scope.findReference(name);
				if (reference == null)
					reference = scope.defineVariable(name);
				node.optimised = reference;
			} else
				reference = (Display.Reference)node.optimised;
			long v = doChild(node, 1).longValue();
			v = v + 1;
			ValueInteger v2 = new ValueInteger(v);
			reference.setValue(v2);
			return data;
		}
	
	// Execute an assignment decrement statement.
		public Object visit(ASTAssignmentDecrement node, Object data) {
			Display.Reference reference;
			if (node.optimised == null) {
				String name = getTokenOfChild(node, 0);
				reference = scope.findReference(name);
				if (reference == null)
					reference = scope.defineVariable(name);
				node.optimised = reference;
			} else
				reference = (Display.Reference)node.optimised;
			reference.setValue(reference.getValue().subtract(doChild(node, 1)));
			return data;
		}
		
		// Execute an assignment increment statement solo.
		public Object visit(ASTAssignmentDecrementSolo node, Object data) {
			Display.Reference reference;
			if (node.optimised == null) {
				String name = getTokenOfChild(node, 0);
				reference = scope.findReference(name);
				if (reference == null)
					reference = scope.defineVariable(name);
				node.optimised = reference;
			} else
				reference = (Display.Reference)node.optimised;
			long v = doChild(node, 1).longValue();
			v = v - 1;
			ValueInteger v2 = new ValueInteger(v);
			reference.setValue(v2);
			return data;
		}
				
	// Execute array assignment statement.
	public Object visit(ASTArrayAssignment node, Object data) {
		Value index = doChild(node, 1);
		Value val = doChild(node, 2);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		
		ValueArray arr = (ValueArray)reference.getValue();		
		arr.setIndex((int)index.longValue(), val);
		
		return data;
	}
	
	//array increment
	public Object visit(ASTArrayAssignmentIncrement node, Object data) {
		Value index = doChild(node, 1);
		Value val = doChild(node, 2);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		
		ValueArray arr = (ValueArray)reference.getValue();		
		arr.addIndex((int)index.longValue(), val);
		return data;
	}
	
	//array decrement
	public Object visit(ASTArrayAssignmentDecrement node, Object data) {
		Value index = doChild(node, 1);
		Value val = doChild(node, 2);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		
		ValueArray arr = (ValueArray)reference.getValue();		
		arr.subIndex((int)index.longValue(), val);
		return data;
	}
	
	//array increment multi
	public Object visit(ASTArrayAssignmentMul node, Object data) {
		Value index = doChild(node, 1);
		Value val = doChild(node, 2);
		
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		
		ValueArray arr = (ValueArray)reference.getValue();		
		arr.multIndex((int)index.longValue(), val);
		return data;
	}
	
	//array increment div
	public Object visit(ASTArrayAssignmentDiv node, Object data) {
		Value index = doChild(node, 1);
		Value val = doChild(node, 2);
			
		Display.Reference reference;
		if (node.optimised == null) {
			String name = getTokenOfChild(node, 0);
			reference = scope.findReference(name);
			if (reference == null)
				throw new ExceptionSemantic("Variable or parameter " + name + " is undefined.");
			node.optimised = reference;
		} else
			reference = (Display.Reference)node.optimised;
		
		ValueArray arr = (ValueArray)reference.getValue();		
		arr.divIndex((int)index.longValue(), val);
		return data;
	}
	
	// OR
	public Object visit(ASTOr node, Object data) {
		return doChild(node, 0).or(doChild(node, 1));
	}

	// AND
	public Object visit(ASTAnd node, Object data) {
		return doChild(node, 0).and(doChild(node, 1));
	}

	// ==
	public Object visit(ASTCompEqual node, Object data) {
		return doChild(node, 0).eq(doChild(node, 1));
	}	

	// !=
	public Object visit(ASTCompNequal node, Object data) {
		return doChild(node, 0).neq(doChild(node, 1));
	}

	// >=
	public Object visit(ASTCompGTE node, Object data) {
		return doChild(node, 0).gte(doChild(node, 1));
	}

	// <=
	public Object visit(ASTCompLTE node, Object data) {
		return doChild(node, 0).lte(doChild(node, 1));
	}

	// >
	public Object visit(ASTCompGT node, Object data) {
		return doChild(node, 0).gt(doChild(node, 1));
	}

	// <
	public Object visit(ASTCompLT node, Object data) {
		return doChild(node, 0).lt(doChild(node, 1));
	}

	// +
	public Object visit(ASTAdd node, Object data) {
		return doChild(node, 0).add(doChild(node, 1));
	}

	// -
	public Object visit(ASTSubtract node, Object data) {
		return doChild(node, 0).subtract(doChild(node, 1));
	}

	// *
	public Object visit(ASTTimes node, Object data) {
		return doChild(node, 0).mult(doChild(node, 1));
	}

	// /
	public Object visit(ASTDivide node, Object data) {
		return doChild(node, 0).div(doChild(node, 1));
	}
	
	// Power
	public Object visit(ASTPow node, Object data) {
		return doChild(node, 0).pow(doChild(node, 1));
	}
	
	// Percent
	public Object visit(ASTPercent node, Object data) {
		return doChild(node, 0).percent(doChild(node, 1));
	}
	
	// Cos 
	public Object visit(ASTCos node, Object data) {
		//return always a rational so bybass here
		double v = doChild(node, 0).doubleValue();
		ValueRational v2 = new ValueRational(v);
		return v2.cos();
	}
	
	//Sin
	public Object visit(ASTSin node, Object data) {
		double v = doChild(node, 0).doubleValue();
		ValueRational v2 = new ValueRational(v);
		return v2.sin();
	}
	
	//Tan
	public Object visit(ASTTan node, Object data) {
		double v = doChild(node, 0).doubleValue();
		ValueRational v2 = new ValueRational(v);
		return v2.tan();
	}
	
	// InverseCos 
	public Object visit(ASTICos node, Object data) {
		//return always a rational so bybass here
		double v = doChild(node, 0).doubleValue();
		ValueRational v2 = new ValueRational(v);
		return v2.icos();
	}
		
	//InverseSin
	public Object visit(ASTISin node, Object data) {
		double v = doChild(node, 0).doubleValue();
		ValueRational v2 = new ValueRational(v);
		return v2.isin();
	}
		
	//InverseTan
	public Object visit(ASTITan node, Object data) {
		double v = doChild(node, 0).doubleValue();
		ValueRational v2 = new ValueRational(v);
		return v2.itan();
	}
	
	// NOT
	public Object visit(ASTUnaryNot node, Object data) {		
		return doChild(node, 0).not();
	}

	// + (unary)
	public Object visit(ASTUnaryPlus node, Object data) {
		return doChild(node, 0).unary_plus();
	}

	// - (unary)
	public Object visit(ASTUnaryMinus node, Object data) {
		return doChild(node, 0).unary_minus();
	}

	// Return string literal
	public Object visit(ASTCharacter node, Object data) {
		if (node.optimised == null)
			node.optimised = ValueString.stripDelimited(node.tokenValue);
		return node.optimised;
	}

	// Return integer literal
	public Object visit(ASTInteger node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueInteger(Long.parseLong(node.tokenValue));
		return node.optimised;
	}

	// Return floating point literal
	public Object visit(ASTRational node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueRational(Double.parseDouble(node.tokenValue));
		return node.optimised;
	}

	// Return true literal
	public Object visit(ASTTrue node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueBoolean(true);
		return node.optimised;
	}

	// Return false literal
	public Object visit(ASTFalse node, Object data) {
		if (node.optimised == null)
			node.optimised = new ValueBoolean(false);
		return node.optimised;
	}

}
