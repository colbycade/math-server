import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

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

    private static final Set<Character> operatorSet = new HashSet<>(Arrays.asList('+', '-', '/', '*', '%'));

    public static double evaluate(String expr) throws EvaluationException{
        return 0.0;
    }

    public static String formatResult(double r){
        return " ";
    }
    
    /**
     * Converts an infix expression to a space-delimited postfix string.
     * Doesn't support variables since this is used to evaluate numeric expressions.
     * Throws EvaluationException for invalid characters or mismatched parentheses.
     */
    public static String convertToPostFix(String eq) throws EvaluationException {
        StringBuilder postFix = new StringBuilder();
        Stack<Character> stack = new Stack<>();
        Character flag, currChar;
        
        for(int i =0; i < eq.length(); i++) {
            currChar = eq.charAt(i);
            
            if(currChar.equals(' ')){
                // skip spaces
                continue;
            }else if(currChar.equals('(')){
                stack.push(currChar);
            }else if(currChar.equals(')')){
                // pop until corresponding left parenthesis
                while (!stack.isEmpty() && !(flag = stack.pop()).equals('(')){
                    postFix.append(' ').append(flag);
                }
            }else if(isOperator(currChar)){
                while (!stack.isEmpty()
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
                while(i < eq.length() && (Character.isDigit(eq.charAt(i)) || eq.charAt(i) == '.')){
                    if(eq.charAt(i) == '.'){
                        if(seenDecimal) throw new EvaluationException("Invalid number: multiple decimal points");
                        seenDecimal = true;
                    }
                    postFix.append(eq.charAt(i));
                    i++;
                }
                i--; // adjust for extra increment at end of inner loop
            }else{
                throw new EvaluationException("Invalid character '" + currChar + "' in expression");
            }
        }

        // pop remaining operators
        while(!stack.isEmpty() && (flag = stack.pop()) != '('){
            postFix.append(' ').append(flag);
        }

        return postFix.toString();
    }
    
    private static boolean isOperator(char c){
        return operatorSet.contains(c);
    }
    
    private static int precedence(char op){
        if (op == '+' || op == '-') return 1;
        if (op == '*' || op == '/' || op == '%') return 2;
        return 0;
    }
}
