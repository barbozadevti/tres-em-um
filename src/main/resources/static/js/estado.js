// Estado do aparelho vindo da API, e o estado só de tela (qual app está aberto, o que se digitou...).
import { avisar } from "./ui.js";

export const loja = {
  estado: null,
  recebidoEm: 0,
  biblioteca: [],
  contatos: [],
  ouvintes: new Set(),
};

/** Estado de tela: não vai para o servidor. */
export const tela = {
  modo: "bloqueio",          // bloqueio | inicio | app
  app: null,                 // musica | telefone | navegador | uml | ajustes
  abaMusica: "biblioteca",   // biblioteca | tocando
  abaTelefone: "teclado",    // favoritos | recentes | contatos | teclado | correio
  digitos: "",
  contatoAberto: null,
  recadoAberto: null,
  recadoFalando: null,
  chamadaMinimizada: false,
  tecladoNaChamada: false,
  editandoEndereco: false,
  visaoAbas: false,
  carregarPagina: 0,         // muda a cada navegação: dispara a barrinha de carregamento
  zoomUml: 900,
  fimDeChamada: null,
};

export function assinar(fn) {
  loja.ouvintes.add(fn);
}

export function avisarOuvintes() {
  for (const fn of loja.ouvintes) fn(loja.estado);
}

function receber(estado) {
  loja.estado = estado;
  loja.recebidoEm = performance.now();
  avisarOuvintes();
}

/** Segundos desde a última resposta: para o tempo andar na tela entre uma consulta e outra. */
export function decorrido() {
  return (performance.now() - loja.recebidoEm) / 1000;
}

export async function buscarEstado() {
  const resposta = await fetch("/api/aparelho", { headers: { Accept: "application/json" } });
  if (resposta.ok) receber(await resposta.json());
}

/** Executa uma ação da API. Em caso de erro, mostra a mensagem do problem+json e recarrega o estado. */
export async function agir(metodo, rota, corpo) {
  const opcoes = { method: metodo, headers: { Accept: "application/json" } };
  if (corpo !== undefined) {
    opcoes.headers["Content-Type"] = "application/json";
    opcoes.body = JSON.stringify(corpo);
  }
  try {
    const resposta = await fetch(rota, opcoes);
    if (resposta.status === 204) {
      await buscarEstado();
      return true;
    }
    const dados = await resposta.json().catch(() => ({}));
    if (!resposta.ok) {
      avisar(dados.detail || "Não foi possível concluir.", "erro");
      await buscarEstado();
      return false;
    }
    receber(dados);
    return true;
  } catch {
    avisar("Sem conexão com o servidor.", "erro");
    return false;
  }
}

export async function carregarCatalogos() {
  const [biblioteca, contatos] = await Promise.all([
    fetch("/api/musica/biblioteca").then((r) => r.json()),
    fetch("/api/telefone/contatos").then((r) => r.json()),
  ]);
  loja.biblioteca = biblioteca;
  loja.contatos = contatos;
}

/** Consulta o servidor a cada segundo: é assim que a tela fica sabendo que o outro lado atendeu, que a música acabou... */
export function iniciarConsultas() {
  setInterval(() => {
    if (!document.hidden) buscarEstado().catch(() => {});
  }, 1000);
}

export function faixa(id) {
  return loja.biblioteca.find((f) => f.id === id);
}

export function contato(id) {
  return loja.contatos.find((c) => c.id === id);
}

/** Posição da música agora, contando o tempo desde a última resposta. */
export function posicaoMusica() {
  const m = loja.estado?.musica;
  if (!m?.faixa) return 0;
  const pos = m.estado === "TOCANDO" ? m.posicao + decorrido() : m.posicao;
  return Math.min(pos, m.faixa.duracao);
}

export function duracaoChamada() {
  const c = loja.estado?.telefone?.chamada;
  if (!c || c.estado !== "EM_ANDAMENTO") return 0;
  return c.duracao + decorrido();
}
