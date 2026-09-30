// App Música: biblioteca, mini player e a tela "Tocando agora".
import { loja, tela, agir, posicaoMusica } from "./estado.js";
import { esc, icone, mmss } from "./ui.js";

function capa(f, classe) {
  return `<div class="capa ${classe}" data-c1="${esc(f.cor)}" data-c2="${esc(f.corSecundaria)}">${icone("nota")}</div>`;
}

function equalizador(tocando) {
  return `<span class="eq ${tocando ? "" : "parado"}" aria-hidden="true"><i></i><i></i><i></i></span>`;
}

export function renderMusica(e) {
  const m = e.musica;
  if (tela.abaMusica === "tocando" && m.faixa) return renderTocando(m);
  const linhas = loja.biblioteca.map((f) => {
    const atual = m.faixa?.id === f.id;
    return `<li><button class="linha-ios faixa-linha ${atual ? "tocando" : ""}" data-acao="tocar-faixa" data-id="${esc(f.id)}">
      ${capa(f, "pequena")}
      <span class="texto"><strong>${esc(f.titulo)}</strong><small>${esc(f.autor)} · ${esc(f.ano)}</small></span>
      <span class="direita">${atual ? equalizador(m.estado === "TOCANDO") : mmss(f.duracao)}</span>
    </button></li>`;
  }).join("");
  return `<section class="app-tela">
    <div class="rolagem" data-rolagem="musica">
      <h1 class="titulo-grande">Música</h1>
      <div class="i-segmentos" role="tablist">
        <button class="ativa" role="tab" aria-selected="true">Biblioteca</button>
        <button role="tab" data-acao="abrir-tocando" ${m.faixa ? "" : "disabled"}>Tocando agora</button>
      </div>
      <p class="subtitulo-secao">Domínio público · ${loja.biblioteca.length} músicas</p>
      <ul class="lista-ios">${linhas}</ul>
      <p class="subtitulo-secao">As músicas são tocadas por um sintetizador no navegador, a partir da partitura que a API envia.</p>
    </div>
    ${m.faixa ? miniPlayer(m) : ""}
  </section>`;
}

function miniPlayer(m) {
  const f = m.faixa;
  return `<div class="mini-player">
    <button class="linha-ios" data-acao="abrir-tocando" aria-label="Abrir Tocando agora">
      ${capa(f, "pequena")}
      <span class="texto"><strong>${esc(f.titulo)}</strong><small>${esc(f.autor)}</small></span>
    </button>
    <button data-acao="alternar-play" aria-label="${m.estado === "TOCANDO" ? "Pausar" : "Tocar"}">${icone(m.estado === "TOCANDO" ? "pausar" : "tocar")}</button>
  </div>`;
}

function renderTocando(m) {
  const f = m.faixa;
  const tocando = m.estado === "TOCANDO";
  const repeticao = { DESLIGADA: "Repetição desligada", TODAS: "Repetir todas", UMA: "Repetir esta" }[m.repeticao];
  return `<section class="tocando-agora" data-c1="${esc(f.cor)}" data-c2="${esc(f.corSecundaria)}">
    <button class="puxar" data-acao="fechar-tocando" aria-label="Voltar para a biblioteca"></button>
    <div class="capa grande ${tocando ? "" : "pausada"}" data-c1="${esc(f.cor)}" data-c2="${esc(f.corSecundaria)}">
      ${icone("nota")}<canvas id="visualizador" width="300" height="120" aria-hidden="true"></canvas>
    </div>
    <div class="info-faixa"><strong>${esc(f.titulo)}</strong><span>${esc(f.autor)} · ${esc(f.ano)}</span></div>
    <div class="progresso">
      <input type="range" min="0" max="${f.duracao}" step="0.1" data-relogio="musica-barra" aria-label="Posição na música">
      <div class="tempos"><span data-relogio="musica-pos">0:00</span><span data-relogio="musica-resta">-0:00</span></div>
    </div>
    <div class="controles">
      <button data-acao="anterior" aria-label="Anterior">${icone("anterior")}</button>
      <button class="principal" data-acao="alternar-play" aria-label="${tocando ? "Pausar" : "Tocar"}">${icone(tocando ? "pausar" : "tocar")}</button>
      <button data-acao="proxima" aria-label="Próxima">${icone("proxima")}</button>
    </div>
    ${m.interrompida ? `<div class="aviso-interrompida">Pausada pela ligação · volta sozinha quando ela terminar</div>` : ""}
    <div class="alternancias">
      <button class="${m.aleatorio ? "ligado" : ""}" data-acao="aleatorio" aria-pressed="${m.aleatorio}" aria-label="Aleatório">${icone("aleatorio")}</button>
      <button class="${m.repeticao !== "DESLIGADA" ? "ligado" : ""}" data-acao="repeticao" aria-label="${repeticao}" title="${repeticao}">
        ${icone("repetir")}${m.repeticao === "UMA" ? `<span class="um">1</span>` : ""}
      </button>
    </div>
  </section>`;
}

export const acoesMusica = {
  "tocar-faixa": (el) => agir("POST", `/api/musica/faixas/${encodeURIComponent(el.dataset.id)}/tocar`).then((ok) => {
    if (ok) tela.abaMusica = "tocando";
  }),
  "abrir-tocando": () => { tela.abaMusica = "tocando"; },
  "fechar-tocando": () => { tela.abaMusica = "biblioteca"; },
  "alternar-play": () => agir("POST", loja.estado.musica.estado === "TOCANDO" ? "/api/musica/pausar" : "/api/musica/tocar"),
  anterior: () => agir("POST", "/api/musica/anterior"),
  proxima: () => agir("POST", "/api/musica/proxima"),
  aleatorio: () => agir("POST", "/api/musica/aleatorio"),
  repeticao: () => agir("POST", "/api/musica/repeticao"),
};

/** Barra de progresso: enquanto o dedo arrasta, o relógio não mexe nela; ao soltar, a posição vai para a API. */
export let arrastandoBarra = false;
export function prepararBarra(raiz) {
  const barra = raiz.querySelector('[data-relogio="musica-barra"]');
  if (!barra || barra.dataset.pronta) return;
  barra.dataset.pronta = "1";
  barra.addEventListener("input", () => { arrastandoBarra = true; atualizarTempos(raiz, Number(barra.value)); });
  barra.addEventListener("change", () => {
    arrastandoBarra = false;
    agir("POST", "/api/musica/posicao", { segundos: Number(barra.value) });
  });
}

export function atualizarTempos(raiz, posicao = posicaoMusica()) {
  const f = loja.estado?.musica?.faixa;
  if (!f) return;
  const barra = raiz.querySelector('[data-relogio="musica-barra"]');
  if (barra && !arrastandoBarra) barra.value = posicao;
  const pos = raiz.querySelector('[data-relogio="musica-pos"]');
  const resta = raiz.querySelector('[data-relogio="musica-resta"]');
  if (pos) pos.textContent = mmss(posicao);
  if (resta) resta.textContent = "-" + mmss(f.duracao - posicao);
}
