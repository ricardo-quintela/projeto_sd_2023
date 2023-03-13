package utils;

public class TratamentoStrings {
    
    public String urlTratamento(int port, String endPoint){
        return "rmi://localhost:" + port + "/" + endPoint;
    }

}
