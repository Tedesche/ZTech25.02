package ZtechAplication;

public class DocumentoUtil {

    // Mantive o nome mascarar_doc para ficar idêntico à documentação da imagem, 
    // mas em Java o ideal seria renomear para mascararDoc.
    public static String mascarar_doc(String cpf) {
        if (cpf == null || cpf.length() < 14) {
            return cpf; // Retorna como está se for nulo ou mal formatado
        }
        
        // Substitui os 3 primeiros dígitos e os 2 últimos por asteriscos
        return "***" + cpf.substring(3, 12) + "**";
    }
}