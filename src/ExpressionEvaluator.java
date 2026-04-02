import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

public class ExpressionEvaluator{

    Set<Character> operatorSet = new HashSet<>(Arrays.asList('+', '-', '/', '*', '('));

    public static double evaluate(String expr) throws EvaluationException{
        return 0.0;
    }

    public static String formatResult(double r){
        return " ";
    }

    public String convertToPostFix(String eq){
        StringBuilder infix = new StringBuilder();

        StringBuilder postFix = new StringBuilder();

        Stack<Character> stack = new Stack<>();

        infix.append(eq).append(')');


        stack.push('(');
        for(int i =0; i < infix.length() - 1; i++){

            Character currChar = eq.charAt(i);

            if(currChar.equals(' ')){
                continue;
            }

            if(operatorSet.contains(currChar)){

                stack.push(currChar);

            }else if(currChar.equals(')')){

                Character flag;

                while(!(flag = stack.pop()).equals('(')){
                    postFix.append(flag);
                }
            }else{

                postFix.append(currChar);

            }
        }



        char flag;
        while((flag = stack.pop()) != '('){
            postFix.append(flag);
        }

        return postFix.toString();
    }
}
