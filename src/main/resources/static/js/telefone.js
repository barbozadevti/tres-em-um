// App Telefone: favoritos, recentes, contatos, teclado, correio de voz visual e a tela de chamada.
import { loja, tela, agir, contato } from "./estado.js";
import { esc, icone, mmss, quandoFoi, iniciais } from "./ui.js";
import * as som from "./som.js";

const LETRAS = { 2: "ABC", 3: "DEF", 4: "GHI", 5: "JKL", 6: "MNO", 7: "PQRS", 8: "TUV", 9: "WXYZ", 0: "+" };
const TECLAS = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"];

function avatar(nome, cor, classe = "") {
  const letras = iniciais(nome);
  return `<span class="avatar ${classe}" ${cor ? `data-fundo="${esc(cor)}"` : ""}>${letras ? esc(letras) : icone("pessoa")}</span>`;
}

/** Máscara enquanto digita: 190, 3223-4567, (27) 99876-5432, 0800 555 0101. */
export function mascarar(d) {
  if (d.startsWith("0800")) return [d.slice(0, 4), d.slice(4, 7), d.slice(7)].filter(Boolean).join(" ");
  if (/[*#]/.test(d) || d.length <= 4) return d;
  if (d.length <= 9) return d.slice(0, d.length - 4) + "-" + d.slice(-4);
  if (d.length <= 11) return `(${d.slice(0, 2)}) ${d.slice(2, d.length - 4)}-${d.slice(-4)}`;
  return d;
}

function contatoPorDigitos(d) {
  if (d.length < 8) return null;
  const alvo = d.length <= 9 ? "27" + d : d;
  return loja.contatos.find((c) => c.numero.replace(/\D/g, "") === alvo) || null;
}

export function renderTelefone(e) {
  const t = e.telefone;
  let corpo;
  if (tela.contatoAberto) corpo = cartaoContato(contato(tela.contatoAberto));
  else if (tela.abaTelefone === "favoritos") corpo = favoritos();
  else if (tela.abaTelefone === "recentes") corpo = recentes(t);
  else if (tela.abaTelefone === "contatos") corpo = contatos();
  else if (tela.abaTelefone === "correio") corpo = correio(t);
  else corpo = teclado();
  const aba = (id, rotulo, nomeIcone, selo = 0) => `<button class="${tela.abaTelefone === id ? "ativa" : ""}" data-acao="aba-telefone" data-aba="${id}">
      ${icone(nomeIcone)}${rotulo}${selo ? `<span class="selo">${selo}</span>` : ""}</button>`;
  return `<section class="app-tela">
    ${corpo}
    <nav class="abas-inferiores" aria-label="Seções do telefone">
      ${aba("favoritos", "Favoritos", "estrela")}
      ${aba("recentes", "Recentes", "relogio", t.chamadasPerdidas)}
      ${aba("contatos", "Contatos", "pessoa")}
      ${aba("teclado", "Teclado", "teclado")}
      ${aba("correio", "Correio", "correio", t.recadosNaoOuvidos)}
    </nav>
  </section>`;
}

function favoritos() {
  const linhas = loja.contatos.filter((c) => c.favorito).map((c) => `<li>
    <button class="linha-ios" data-acao="ligar" data-numero="${esc(c.numero)}">
      ${avatar(c.nome, c.cor)}
      <span class="texto"><strong>${esc(c.nome)}</strong><small>${esc(c.rotulo)}</small></span>
      <span class="direita">${icone("telefone")}</span>
    </button></li>`).join("");
  return `<div class="rolagem" data-rolagem="tel-favoritos"><h1 class="titulo-grande">Favoritos</h1><ul class="lista-ios">${linhas}</ul></div>`;
}

function recentes(t) {
  if (!t.recentes.length) return `<div class="rolagem"><h1 class="titulo-grande">Recentes</h1><p class="vazio-ios">Nenhuma chamada.</p></div>`;
  const linhas = t.recentes.map((c) => {
    const perdida = c.desfecho === "Perdida";
    const detalhe = c.duracao > 0 ? `${c.desfecho} · ${mmss(c.duracao)}` : c.desfecho;
    return `<li class="${perdida ? "perdida" : ""}"><button class="linha-ios" data-acao="ligar" data-numero="${esc(c.numero)}">
      ${icone(c.direcao === "RECEBIDA" ? "chegando" : "saindo", "seta-direcao")}
      <span class="texto"><strong>${esc(c.nome)}</strong><small>${esc(detalhe)}</small></span>
      <span class="direita">${esc(quandoFoi(c.inicio))}</span>
    </button></li>`;
  }).join("");
  return `<div class="rolagem" data-rolagem="tel-recentes"><h1 class="titulo-grande">Recentes</h1><ul class="lista-ios">${linhas}</ul></div>`;
}

function contatos() {
  const linhas = loja.contatos.map((c) => `<li><button class="linha-ios" data-acao="abrir-contato" data-id="${esc(c.id)}">
      ${avatar(c.nome, c.cor)}
      <span class="texto"><strong>${esc(c.nome)}</strong><small>${esc(c.numero)}</small></span>
    </button></li>`).join("");
  return `<div class="rolagem" data-rolagem="tel-contatos"><h1 class="titulo-grande">Contatos</h1><ul class="lista-ios">${linhas}</ul>
    <p class="subtitulo-secao">Contatos fictícios. Nenhuma ligação real é feita.</p></div>`;
}

function cartaoContato(c) {
  return `<div class="rolagem">
    <button class="voltar-ios" data-acao="fechar-contato">${icone("voltar")}Contatos</button>
    <div class="cartao-contato">
      ${avatar(c.nome, c.cor, "grande")}
      <h3>${esc(c.nome)}</h3>
      <span class="numero">${esc(c.rotulo)} · ${esc(c.numero)}</span>
      <div class="acoes-contato">
        <button class="acao-contato" data-acao="ligar" data-numero="${esc(c.numero)}">${icone("telefone")}Ligar</button>
        <button class="acao-contato" data-acao="simular-chamada" data-numero="${esc(c.numero)}">${icone("chegando")}Pedir para ${esc(c.nome.split(" ")[0])} ligar</button>
      </div>
    </div>
  </div>`;
}

function teclado() {
  const c = contatoPorDigitos(tela.digitos);
  const teclas = TECLAS.map((k) => `<button class="tecla" data-acao="tecla" data-tecla="${k}" aria-label="${k}">
      <b>${k}</b><small>${LETRAS[k] || ""}</small></button>`).join("");
  return `<div class="teclado-tela">
    <div class="visor"><span class="digitos" aria-live="polite">${esc(mascarar(tela.digitos))}</span><small>${c ? esc(c.nome) : ""}</small></div>
    <div class="teclas">${teclas}</div>
    <div class="linha-ligar">
      <span></span>
      <button class="botao-ligar" data-acao="ligar-digitos" aria-label="Ligar">${icone("telefone")}</button>
      ${tela.digitos ? `<button class="botao-apagar" data-acao="apagar-digito" aria-label="Apagar">${icone("apagar")}</button>` : "<span></span>"}
    </div>
  </div>`;
}

function correio(t) {
  const linhas = t.recados.map((r) => {
    const aberto = tela.recadoAberto === r.id;
    return `<li class="recado">
      <button class="linha-ios" data-acao="abrir-recado" data-id="${r.id}" aria-expanded="${aberto}">
        <span class="${r.ouvido ? "ponto-lido" : "ponto-novo"}" aria-label="${r.ouvido ? "" : "Novo"}"></span>
        <span class="texto"><strong>${esc(r.nome)}</strong><small>${esc(quandoFoi(r.recebidoEm))}</small></span>
        <span class="direita">${mmss(r.duracao)}</span>
      </button>
      ${aberto ? `<div class="detalhe">
        <p class="transcricao ${tela.recadoFalando === r.id ? "falando" : ""}">${esc(r.transcricao)}</p>
        <div class="acoes-recado">
          <button data-acao="ouvir-recado" data-id="${r.id}">${icone("tocar")}Ouvir</button>
          <button data-acao="ligar" data-numero="${esc(r.numero)}">${icone("telefone")}Retornar</button>
          <button class="apagar" data-acao="apagar-recado" data-id="${r.id}">${icone("lixeira")}Apagar</button>
        </div></div>` : ""}
    </li>`;
  }).join("");
  return `<div class="rolagem" data-rolagem="tel-correio">
    <h1 class="titulo-grande">Correio de voz</h1>
    <button class="botao-ios destaque" data-acao="correio-iniciar" ${t.recadosNaoOuvidos ? "" : "disabled"}>
      ${icone("tocar")}${t.recadosNaoOuvidos ? `Ouvir recados novos (${t.recadosNaoOuvidos})` : "Nenhum recado novo"}
    </button>
    <p class="subtitulo-secao">Correio de voz visual: ouça em qualquer ordem</p>
    ${t.recados.length ? `<ul class="lista-ios">${linhas}</ul>` : `<p class="vazio-ios">Sem recados.</p>`}
  </div>`;
}

// ----- Tela de chamada (sobreposição) -----

export function renderChamada(e) {
  const c = e.telefone.chamada;
  if (!c || tela.chamadaMinimizada) return "";
  const cor = c.cor || "#6366f1";
  const topo = `${avatar(c.nome, null, `grande ${c.estado === "TOCANDO" ? "tocando" : ""}`)}
    <h3>${esc(c.nome)}</h3>`;
  const emergencia = c.emergencia ? `<span class="etiqueta-emergencia">Número de emergência · simulação, nenhuma ligação real é feita</span>` : "";
  if (c.estado === "TOCANDO") {
    return `<section class="tela-chamada" data-cor="${esc(cor)}" aria-label="Chamada recebida">
      ${topo}<span class="situacao">${c.contato ? "celular" : esc(c.numero)} · chamada recebida</span>${emergencia}
      <div class="entrando">
        <button class="encerrar-chamada" data-acao="recusar"><span class="circulo">${icone("desligar")}</span>Recusar</button>
        <button class="atender" data-acao="atender"><span class="circulo">${icone("telefone")}</span>Atender</button>
      </div>
    </section>`;
  }
  const emAndamento = c.estado === "EM_ANDAMENTO";
  const situacao = emAndamento ? `<span class="situacao" data-relogio="chamada">0:00</span>` : `<span class="situacao">chamando…</span>`;
  const botao = (acao, nomeIcone, rotulo, ligado, habilitado = true) =>
    `<button class="botao-chamada ${ligado ? "ligado" : ""}" data-acao="${acao}" ${habilitado ? "" : "disabled"} aria-pressed="${!!ligado}">
      <span class="circulo">${icone(nomeIcone)}</span>${rotulo}</button>`;
  const meio = tela.tecladoNaChamada && emAndamento
    ? `<div class="tons-enviados" aria-live="polite">${esc(c.tons)}</div>
       <div class="teclado-chamada">${TECLAS.map((k) => `<button class="tecla" data-acao="tom-chamada" data-tecla="${k}"><b>${k}</b><small>${LETRAS[k] || ""}</small></button>`).join("")}</div>`
    : `<div class="botoes-chamada">
        ${botao("mudo", "mudo", "mudo", c.mudo, emAndamento)}
        ${botao("teclado-chamada", "teclado", "teclado", false, emAndamento)}
        ${botao("viva-voz", "altofalante", "viva-voz", c.vivaVoz)}
      </div>`;
  return `<section class="tela-chamada" data-cor="${esc(cor)}" aria-label="Chamada em curso">
    ${topo}${situacao}${emergencia}
    ${meio}
    ${tela.tecladoNaChamada && emAndamento ? `<button class="botao-palco" data-acao="teclado-chamada">Ocultar teclado</button>` : ""}
    <button class="encerrar-chamada" data-acao="encerrar" aria-label="Encerrar"><span class="circulo">${icone("desligar")}</span></button>
  </section>`;
}

export function renderFimDeChamada() {
  const f = tela.fimDeChamada;
  if (!f) return "";
  return `<div class="fim-chamada"><div>Chamada encerrada<small>${esc(f.nome)} · ${esc(f.detalhe)}</small></div></div>`;
}

// ----- Ações -----

function ligar(numero) {
  tela.chamadaMinimizada = false;
  tela.tecladoNaChamada = false;
  return agir("POST", "/api/telefone/ligacoes", { numero }).then((ok) => { if (ok) tela.digitos = ""; });
}

export function falarRecado(id) {
  const r = loja.estado.telefone.recados.find((x) => x.id === id);
  if (!r) return;
  tela.recadoAberto = id;
  tela.recadoFalando = id;
  som.falar(`Recado de ${r.nome}. ${r.transcricao}`, () => {
    if (tela.recadoFalando === id) tela.recadoFalando = null;
    document.dispatchEvent(new Event("redesenhar"));
  });
}

export const acoesTelefone = {
  "aba-telefone": (el) => {
    tela.abaTelefone = el.dataset.aba;
    tela.contatoAberto = null;
    if (el.dataset.aba === "recentes" && loja.estado.telefone.chamadasPerdidas) agir("POST", "/api/telefone/recentes/visto");
  },
  tecla: (el) => {
    som.tom(el.dataset.tecla);
    if (tela.digitos.length < 15) tela.digitos += el.dataset.tecla;
  },
  "apagar-digito": () => { tela.digitos = tela.digitos.slice(0, -1); },
  "ligar-digitos": () => {
    if (!tela.digitos) {
      // Como no iPhone: com o visor vazio, o botão traz o último número discado.
      const ultima = loja.estado.telefone.recentes.find((c) => c.direcao === "EFETUADA");
      if (ultima) tela.digitos = ultima.numero.replace(/\D/g, "");
      return;
    }
    return ligar(tela.digitos);
  },
  ligar: (el) => ligar(el.dataset.numero),
  "abrir-contato": (el) => { tela.contatoAberto = el.dataset.id; },
  "fechar-contato": () => { tela.contatoAberto = null; },
  "simular-chamada": (el) => {
    tela.chamadaMinimizada = false;
    return agir("POST", "/api/simulador/chamada-recebida", { numero: el.dataset.numero });
  },
  atender: () => { tela.chamadaMinimizada = false; return agir("POST", "/api/telefone/atender"); },
  recusar: () => agir("POST", "/api/telefone/recusar"),
  encerrar: () => { tela.tecladoNaChamada = false; return agir("POST", "/api/telefone/encerrar"); },
  mudo: () => agir("POST", "/api/telefone/mudo"),
  "viva-voz": () => agir("POST", "/api/telefone/viva-voz"),
  "teclado-chamada": () => { tela.tecladoNaChamada = !tela.tecladoNaChamada; },
  "tom-chamada": (el) => { som.tom(el.dataset.tecla); return agir("POST", "/api/telefone/tons", { tecla: el.dataset.tecla }); },
  "abrir-recado": (el) => {
    const id = Number(el.dataset.id);
    tela.recadoAberto = tela.recadoAberto === id ? null : id;
    if (tela.recadoAberto === null) { som.calar(); tela.recadoFalando = null; }
  },
  "ouvir-recado": (el) => {
    const id = Number(el.dataset.id);
    return agir("POST", `/api/telefone/recados/${id}/ouvir`).then((ok) => { if (ok) falarRecado(id); });
  },
  "apagar-recado": (el) => {
    som.calar();
    tela.recadoFalando = null;
    tela.recadoAberto = null;
    return agir("DELETE", `/api/telefone/recados/${el.dataset.id}`);
  },
  "correio-iniciar": () => agir("POST", "/api/telefone/correio-de-voz").then((ok) => {
    const id = loja.estado.telefone.recadoEmReproducao;
    if (ok && id) falarRecado(id);
  }),
};
