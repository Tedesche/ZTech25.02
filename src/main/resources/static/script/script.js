function GETcep() {
	 var cep = document.getElementById('cep').value;
	 var url = "https://viacep.com.br/ws/"+cep+"/json/";
	
	 fetch(url)
	 .then(response => response.json())
	 .then(data => {
	 if(data.erro){
	 document.getElementById('resultado').textContent = "CEP não encontrado!";
	 }else{
	 document.getElementById('resultado').innerHTML =
	 "<b>Cidade:</b>" + data.cidade + "";
	 +"<b>Logradouro:</b>" + data.logradouro + "";
	 +"<b>Bairro:</b>" + data.bairro + "";
	 +"<b>Bairro:</b>" + data.bairro + "";
	 }
	
	 }).catch(error => alert(error))
}