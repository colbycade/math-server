public class ExpressionEvaluatorTest {
    
    static int passed = 0, failed = 0;
    
    public static void main(String[] args) {
        // Basic arithmetic
        test("2+2",           4);
        test("10-3",          7);
        test("3*4",           12);
        test("10/4",          2.5);
        test("10%3",          1);
        
        // Modulus basic
        test("10%3",         1);
        test("-10%3",       -1);
        test("10%-3",        1);
        test("10%10",        0);
        
        // Precedence
        test("2+3*4",         14);
        test("(2+3)*4",       20);
        test("5+4*(14%3)-8/2",9);
        test("6*2%5%3",       2);
        
        // Unary minus
        test("-3+5",          2);
        test("(-3+5)*2",      4);
        test("-3+-5",         -8);
        test("--3+2",         5);
        
        // Decimals
        test("1.5+1.5",       3);
        test("1.5+2",         3.5);
        test("1.5*2.0",       3);
        test(".5*2.",         1);
        
        // Errors
        testError("10/0",     "Division by zero");
        testError("10%0",     "Modulus by zero");
        testError("(2+3",     "Mismatched");
        testError("2+3)",     "Mismatched");
        testError("2++3",     "Invalid");
        testError("2 3",      "Invalid");
        testError("",         "empty");
        testError("()",       "empty");
        
        // More edge cases
        test("  3  *  4  ", 12);
        test("42",          42);
        test("((((2+3))))", 5);
        testError("1.1.1",    "Invalid");
        
        System.out.printf("%nResults: %d passed, %d failed%n", passed, failed);
    }
    
    static void test(String expr, double expected) {
        try {
            double result = ExpressionEvaluator.evaluate(expr);
            if (Math.abs(result - expected) < 1e-9) {
                System.out.println("PASS: " + expr + " = " + result);
                passed++;
            } else {
                System.out.println("FAIL: " + expr + " expected " + expected + " got " + result);
                failed++;
            }
        } catch (EvaluationException e) {
            System.out.println("FAIL: " + expr + " threw unexpected: " + e.getMessage());
            failed++;
        }
    }
    
    static void testError(String expr, String expectedMsgFragment) {
        try {
            ExpressionEvaluator.evaluate(expr);
            System.out.println("FAIL: " + expr + " expected exception but got none");
            failed++;
        } catch (EvaluationException e) {
            if (e.getMessage().toLowerCase().contains(expectedMsgFragment.toLowerCase())) {
                System.out.println("PASS (error): " + expr + " → " + e.getMessage());
                passed++;
            } else {
                System.out.println("FAIL: " + expr + " expected msg containing '"
                        + expectedMsgFragment + "' but got: " + e.getMessage());
                failed++;
            }
        }
    }
}