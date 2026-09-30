# Lean Inception: Três em Um

> Decisões de produto no formato Lean Inception (Paulo Caroli):
> visão → escopo → personas → jornadas → funcionalidades → sequenciamento → MVP.

---

## 1. Visão do produto

**Para** quem estuda ou avalia orientação a objetos (estudantes, professores e recrutadores técnicos)
**cujo** problema é que diagramas UML costumam ficar no papel, desatualizados e sem nada que mostre o modelo funcionando,
**o Três em Um** é um iPhone simulado no navegador
**que** transforma os três papéis do aparelho (reprodutor musical, telefone e navegador) em interfaces Java de verdade, com o domínio rodando no servidor e cada chamada de método visível na tela.
**Diferente de** exercícios que param nas classes vazias,
**o nosso produto** tem regras de negócio testadas, um diagrama conferido automaticamente contra o código e uma interface que dá vontade de usar.

## 2. O produto É / NÃO É / FAZ / NÃO FAZ

| É | NÃO É |
|---|---|
| Uma modelagem OO do iPhone, executável | Um emulador de iOS |
| Uma vitrine de interfaces, composição e polimorfismo | Um aplicativo de telefonia real |
| Uma simulação com regras explícitas | Um reprodutor de músicas protegidas por direitos autorais |

| FAZ | NÃO FAZ |
|---|---|
| Tocar músicas de domínio público com um sintetizador | Fazer ligações reais (tudo é simulado) |
| Simular ligações feitas e recebidas, com correio de voz visual | Acessar sites reais dentro do aparelho (usa sites fictícios) |
| Navegar com abas, histórico, busca e favoritos | Guardar dados pessoais (estado só na sessão) |
| Mostrar cada chamada de método no painel "Por dentro" | Exigir cadastro ou login |

## 3. Objetivos do produto

1. **Modelo fiel ao desafio:** as interfaces `ReprodutorMusical`, `AparelhoTelefonico` e `NavegadorInternet` com os métodos pedidos, e o `IPhone` implementando as três.
2. **Diagrama que não mente:** o UML fica no repositório e um teste falha se ele divergir do código.
3. **Coordenação entre papéis:** a cena da apresentação de 2007, em que a música pausa quando chega uma ligação e volta quando ela termina.
4. **Fácil de avaliar:** abre com um clique, com histórico de exemplo, e mostra o que acontece por dentro.

## 4. Personas

### Marina, recrutadora técnica (34 anos)
- **Comportamento:** avalia dezenas de portfólios por semana, gasta poucos minutos em cada um.
- **Necessidades:** entender rápido o que o projeto faz e ver sinais de boas práticas sem precisar clonar nada.

### Lucas, desenvolvedor sênior que faz a entrevista técnica (41 anos)
- **Comportamento:** abre o código, procura os testes e as decisões de arquitetura.
- **Necessidades:** ver modelagem limpa, testes significativos e escolhas justificadas.

### Beatriz, estudante de programação (22 anos)
- **Comportamento:** aprende POO e UML e quer exemplos concretos.
- **Necessidades:** ligar o diagrama ao código e o código ao comportamento.

## 5. Jornadas

**Marina avalia o projeto em 2 minutos**
1. Abre o README: vê o GIF do aparelho, o diagrama e o selo de testes passando.
2. Abre o simulador, desbloqueia e toca uma música.
3. Clica em "Mãe está ligando": a música pausa, ela atende, encerra, e a música volta sozinha.
4. No painel "Por dentro", vê cada método chamado, inclusive os que o próprio aparelho fez.

**Lucas investiga o código**
1. Procura o `IPhone` e encontra composição (três especialistas) em vez de uma classe gigante.
2. Encontra o proxy dinâmico que registra as chamadas e o teste que confere o diagrama UML.
3. Roda `mvn verify` e vê os testes passarem, inclusive a máquina de estados da chamada.

## 6. Funcionalidades e revisão técnica

| Funcionalidade | Esforço | Valor de negócio | Valor de UX |
|---|---|---|---|
| Interfaces dos três papéis e `IPhone` que implementa as três | E | $$$ | ♥ |
| Diagrama UML (Mermaid) conferido por teste | EE | $$$ | ♥♥ |
| Reprodutor com fila, aleatório, repetição e posição pelo relógio | EE | $$ | ♥♥♥ |
| Músicas de domínio público sintetizadas no navegador (Web Audio) | EE | $$ | ♥♥♥ |
| Telefone: números brasileiros, chamadas com máquina de estados | EE | $$$ | ♥♥ |
| Correio de voz visual (recados ouvidos em qualquer ordem) | E | $$ | ♥♥♥ |
| Navegador: barra de endereços com busca, abas, abas privadas, histórico, favoritos | EE | $$ | ♥♥ |
| Música pausa na ligação e volta depois (coordenação entre papéis) | E | $$$ | ♥♥♥ |
| Painel "Por dentro" com proxy dinâmico | EE | $$$ | ♥♥♥ |
| Tela de bloqueio, ilha dinâmica, tela de início | EE | $ | ♥♥♥ |
| Várias pessoas ao mesmo tempo (um aparelho por sessão) | E | $$ | ♥ |
| Mensagens (SMS) e contatos editáveis | EE | $ | ♥♥ |

## 7. Sequenciamento em ondas

| Onda | Funcionalidades | Situação |
|---|---|---|
| 0 | Entrega do desafio: interfaces, `IPhone`, diagrama e testes ([java-iphone-uml](https://github.com/barbozadevti/java-iphone-uml)) | Entregue |
| 1 | Reprodutor musical com fila, aleatório e repetição | Entregue |
| 2 | Telefone com números brasileiros, máquina de estados da chamada e correio de voz | Entregue |
| 3 | Navegador com abas, histórico, busca e sites fictícios | Entregue |
| 4 | `IPhone` por composição, proxy de rastreio e diagrama UML conferido por teste | Entregue |
| 5 | API REST por papel e frontend do aparelho com o painel "Por dentro" | Entregue |
| 6 | Mensagens (SMS) e contatos editáveis | Próxima |

## 8. MVP

**Hipótese:** um modelo OO que se pode usar, e não só ler, prende a atenção de quem avalia e prova domínio de interfaces, composição e testes.

**Ondas 0 a 5.** Validado quando:
- o diagrama e o código não divergem (garantido por teste);
- a música pausa em toda ligação iniciada ou recebida e volta quando a chamada termina;
- nenhuma transição de chamada inválida é aceita (testada contra uma tabela com todas as combinações);
- o aparelho funciona no celular e no computador, sem instalar nada além do Java.
