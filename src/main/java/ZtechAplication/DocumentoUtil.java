package ZtechAplication;

public class DocumentoUtil {


    public static String mascarar_doc(String cpf) {
        if (cpf == null || cpf.length() < 14) {
            return cpf; // 
        }
        

        return "***" + cpf.substring(3, 12) + "**";
    }
}