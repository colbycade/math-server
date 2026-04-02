public class EvaluationException extends Exception{


    EvaluationException(){
        super("Invalid Expr");
    }

    EvaluationException(String message){
        super(message);
    }
}
