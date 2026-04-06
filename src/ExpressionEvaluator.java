import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * ExpressionEvaluator
 *
 * Parses and evaluates basic arithmetic expressions by converting infix to postfix notation
 * and then evaluating the postfix expression.
 *
 * Supports:
 *   - Operators: +, -, *, /, %
 *   - Parentheses for grouping
 *   - Integer and decimal operands
 *   - Unary negation (e.g. "-3+5")
 *
 * Throws EvaluationException for:
 *   - Division by zero
 *   - Mismatched parentheses
 *   - Invalid characters
 *   - Empty or null expression
 */
public class ExpressionEvaluator{
    private static final int DECIMAL_PLACES = 4; // for formatting results
    private static final Set<Character> operatorSet = new HashSet<>(Arrays.asList('+', '-', '/', '*', '%'));
    
    /**
     * Convert the input expression to postfix then evaluate it and return as double.
     */
    public static double evaluate(String expr) throws EvaluationException{
        if(expr == null || expr.trim().isEmpty()){
            throw new EvaluationException("Expression cannot be empty");
        }
        String postFix = convertToPostFix(expr.trim());
        if(postFix.trim().isEmpty()){
            throw new EvaluationException("Expression cannot be empty");  // catches cases with only parenthesis e.g., "(())"
        }
        return evaluatePostfix(postFix);
    }
    
    /**
     * Formats the result as a string.
     * If the result has no fractional part, return as plain integer string.
     * Else, round to `DECIMAL_PLACES` decimal places, then remove trailing zeros and decimal point if not needed (if
     * rounds down to integer).
     */
    public static String formatResult(double r){
        if(r == Math.floor(r)){
            return String.valueOf((long) r);
        }
        return String.format("%." + DECIMAL_PLACES + "f", r).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
    
    /**
     * Converts an infix expression to a space-delimited postfix string.
     * Doesn't support variables since this is used to evaluate numeric expressions.
     * Throws EvaluationException for invalid characters or mismatched parentheses.
     */
    public static String convertToPostFix(String expr) throws EvaluationException{
        StringBuilder postFix = new StringBuilder();
        Stack<Character> stack = new Stack<>();
        Character top, currChar;
        
        for(int i =0; i < expr.length(); i++){
            currChar = expr.charAt(i);
            
            if(currChar.equals(' ')){
                // skip spaces
                continue;
            }
            
            // skip spaces to find previous non-space character
            int j = i - 1;
            while(j >= 0 && expr.charAt(j) == ' ') j--;
            
            // check for unary minus: if at start, after '(', or after another operator
            if(currChar == '-' && (j < 0 || expr.charAt(j) == '(' || isOperator(expr.charAt(j)))){
                if(!postFix.isEmpty()){ // delimit with space unless this is the first token
                    postFix.append(' ');
                }
                postFix.append('0');
                stack.push('-');
            }else if(currChar.equals('(')){
                stack.push(currChar);
            }else if(currChar.equals(')')){
                // pop until corresponding left parenthesis
                boolean foundOpen = false;
                while(!stack.isEmpty()){
                    top = stack.pop();
                    if(top == '('){
                        foundOpen = true;
                        break;
                    }
                    postFix.append(' ').append(top);
                }
                if(!foundOpen) throw new EvaluationException("Mismatched parentheses");
            }else if(isOperator(currChar)){ // binary operator
                while(!stack.isEmpty()
                        && isOperator(stack.peek())
                        && precedence(stack.peek()) >= precedence(currChar)){
                    // pop higher or equal precedence operators and delimit with spaces
                    postFix.append(' ').append(stack.pop());
                }
                
                stack.push(currChar);
            }else if(Character.isDigit(currChar) || currChar == '.'){
                // delimit with space unless this is the first token
                if(!postFix.isEmpty()){
                    postFix.append(' ');
                }
                // append full number token including multiple digits or a single decimal point
                boolean seenDecimal = false;
                while(i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')){
                    if(expr.charAt(i) == '.'){
                        if(seenDecimal) throw new EvaluationException("Invalid number: multiple decimal points");
                        seenDecimal = true;
                    }
                    postFix.append(expr.charAt(i));
                    i++;
                }
                i--; // adjust for extra increment at end of inner loop
            }else{
                throw new EvaluationException("Invalid character '" + currChar + "' in expression");
            }
        }

        // pop remaining operators
        while(!stack.isEmpty()){
            top = stack.pop();
            if(top == '(') throw new EvaluationException("Mismatched parentheses");
            postFix.append(' ').append(top);
        }

        return postFix.toString();
    }
    
    /**
     * Evaluates a space-delimited postfix expression and returns the result as a double.
     * Throws EvaluationException for malformed expressions, invalid numbers, or division/modulus by zero.
     */
    private static double evaluatePostfix(String postFix) throws EvaluationException{
        Stack<Double> stack = new Stack<>();
        String[] tokens = postFix.split(" ");
        
        for(String token : tokens){
            if(token.isEmpty()){
                continue; // shouldn't happen but just in case
            }else if(isOperator(token.charAt(0)) && token.length() == 1){
                // binary operator: pop two operands and apply operator to them
                if(stack.size() < 2){
                    throw new EvaluationException("Invalid expression: missing operands");
                }
                double b = stack.pop();
                double a = stack.pop();
                stack.push(applyOperator(token.charAt(0), a, b));
            }else{
                // operand: parse as double to handle both integers and decimals
                try{
                    stack.push(Double.parseDouble(token));
                }catch(NumberFormatException e){
                    throw new EvaluationException("Invalid number: '" + token + "'");
                }
            }
        }
        
        // should end up with a single final value on stack
        if(stack.size() != 1){
            throw new EvaluationException("Invalid expression: too many operands");
        }
        return stack.pop();
    }
    
    /**
     * Applies a binary operator to two operands and returns the result.
     * Throws EvaluationException for division or modulus by zero, or unknown operators.
     */
    private static double applyOperator(char op, double a, double b) throws EvaluationException{
        switch(op){
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/':
                if(b == 0) throw new EvaluationException("Division by zero");
                return a / b;
            case '%':
                if(b == 0) throw new EvaluationException("Modulus by zero");
                return a % b;
            default:
                throw new EvaluationException("Unknown operator: " + op);
        }
    }
    
    private static boolean isOperator(char c){
        return operatorSet.contains(c);
    }
    
    private static int precedence(char op){
        if(op == '+' || op == '-') return 1;
        if(op == '*' || op == '/' || op == '%') return 2;
        return 0;
    }
}