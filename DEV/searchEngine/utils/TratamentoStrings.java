package searchEngine.utils;

public class TratamentoStrings {
    
    public static String urlTratamento(int port, String endPoint){
        return "rmi://localhost:" + port + "/" + endPoint;
    }

}
