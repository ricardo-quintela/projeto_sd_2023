package utils;

public class tratamentoStrings {
    
    public String urlTratamento(int port, String endPoint){
        return "rmi://localhost:" + port + "/" + endPoint;
    }

}
