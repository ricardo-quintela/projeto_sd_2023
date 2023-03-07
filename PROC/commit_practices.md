# Práticas de commit
### As mensagens de commit devem:

* Ser escritas em **português**
* Completar sucintamente a idea "Se aplicado, este commit irá..." usando um verbo **imperativo ou **infinitivo**
* Ser curtas (50 caracteres)
* Conter o número do *issue* a qual pertencem (se aplicável) **no corpo do commit**

* Começar com "[código]", se não relacionado com desenvolvimento
	- [PROC] - alterações aos processos
	- [ARCH] - ficheiros do desenho da arquitetura
	- [DES] - ficheiros do design gráfico
	- [PM] - gestão do projeto
	- [QA] - desenvolvimento de funcionalidades de Quality Assurance
	- [REQ] - trabalho sobre os requisitos

* Se relacionado com desenvolvimento, começar com um dos seguintes tipos de commit:
	- wip:      Work in progress
	- docs:     Mudanças de documentação
	- feat:     Uma nova funcionalidade
	- fix:      Arranjou-se um bug
	- perf:     Uma mudança no código que aumenta a performance
	- refactor: Uma mudança no código que nem arranja um bug nem adiciona uma funcionalidade
	- test:     Adicionar testes ou corrigir testes existentes

* Utilizar o corpo do commit para detalhar as alterações (caso necessário)
* Referir apenas alterações da mesma área (cada commit deve ser limitado a uma área de alterações)

---

Exemplo 1:
fix: bug na lista de numeros  

--- 

Exemplo 2:
feat: funçao de compressao

---

