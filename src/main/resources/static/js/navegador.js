// App Navegador: páginas da "internet" do simulador, barra de endereços, abas e favoritos.
import { loja, tela, agir } from "./estado.js";
import { esc, icone } from "./ui.js";

const SITES_DO_SIMULADOR = /^https?:\/\/([a-z0-9-]+\.)*(exemplo|barboza\.dev)(:\d+)?\//;

function blocoHtml(b) {
  switch (b.tipo) {
    case "cabecalho":
      return `<h1>${esc(b.titulo)}</h1>${b.subtitulo ? `<p class="sub">${esc(b.subtitulo)}</p>` : ""}`;
    case "paragrafo":
      return `<p>${esc(b.texto)}</p>`;
    case "lista": {
      const tag = b.numerada ? "ol" : "ul";
      return `${b.titulo ? `<h2>${esc(b.titulo)}</h2>` : ""}<${tag}>${b.itens.map((i) => `<li>${esc(i)}</li>`).join("")}</${tag}>`;
    }
    case "destaque":
      return `<div class="destaque-site"><b>${esc(b.valor)}</b><span>${esc(b.legenda)}</span></div>`;
    case "cartoes":
      return `${b.titulo ? `<h2>${esc(b.titulo)}</h2>` : ""}<div class="cartoes-site">${b.cartoes.map((c) => {
        const externo = !SITES_DO_SIMULADOR.test(c.url);
        return `<button class="cartao-site" data-acao="abrir-link" data-url="${esc(c.url)}">
          <strong>${esc(c.titulo)}</strong>${c.texto ? `<span>${esc(c.texto)}</span>` : ""}
          ${externo ? `<span class="externo">Abre ${esc(new URL(c.url).host)} em outra aba ↗</span>` : ""}
        </button>`;
      }).join("")}</div>`;
    default:
      return "";
  }
}

function corDoSite(dominio) {
  const cores = { "noticias.exemplo": "#b91c1c", "clima.exemplo": "#0284c7", "receitas.exemplo": "#c2410c",
    "keynote.exemplo": "#111827", "barboza.dev": "#4f46e5", "busca.exemplo": "#16a34a" };
  return cores[dominio] || "#64748b";
}

function paginaInicial(n, privada) {
  const favoritos = n.favoritos.map((f) => `<button class="favorito" data-acao="abrir-link" data-url="${esc(f.url)}">
      <span class="letra" data-cor-site="${corDoSite(f.dominio)}">${esc(f.titulo[0])}</span>${esc(f.titulo)}</button>`).join("");
  const visitadas = n.visitadas.map((v) => `<li><button class="linha-ios" data-acao="abrir-link" data-url="${esc(v.url)}">
      <span class="texto"><strong>${esc(v.titulo)}</strong><small>${esc(v.dominio)}</small></span></button></li>`).join("");
  return `<div class="pagina-inicial">
    ${privada ? `<div class="aviso-privada">${icone("cadeado")}<span>Navegação privada: as páginas desta aba não entram no histórico.</span></div>` : ""}
    <h2>Favoritos</h2>
    <div class="favoritos-grade">${favoritos}</div>
    ${visitadas && !privada ? `<h2>Visitados recentemente</h2><ul class="lista-ios">${visitadas}</ul>` : ""}
  </div>`;
}

function paginaHtml(n, privada) {
  const p = n.pagina;
  if (p.tipo === "INICIO") return paginaInicial(n, privada);
  if (p.tipo === "ERRO") {
    return `<div class="erro-pagina">${icone("info")}<h1>${esc(p.titulo)}</h1>${p.blocos.map(blocoHtml).join("")}</div>`;
  }
  return `<div class="faixa-site" data-cor-site="${esc(p.cor)}"><small>${esc(p.dominio)}</small></div>
    <article class="corpo-site" data-cor-site="${esc(p.cor)}">${p.blocos.map(blocoHtml).join("")}</article>`;
}

export function renderNavegador(e) {
  const n = e.navegador;
  const aba = n.abas.find((a) => a.id === n.abaAtual);
  const privada = aba?.privada;
  if (tela.visaoAbas) return visaoAbas(n);
  const endereco = tela.editandoEndereco
    ? `<form class="campo-endereco" data-formulario="endereco" role="search">
        ${icone("lupa")}
        <input name="url" value="${esc(n.pagina.tipo === "INICIO" ? "" : n.pagina.url)}" placeholder="Buscar ou digitar endereço"
          autocomplete="off" autocapitalize="off" spellcheck="false" enterkeyhint="go" aria-label="Endereço">
        <button type="button" class="recarregar" data-acao="cancelar-endereco" aria-label="Cancelar">${icone("fechar")}</button>
      </form>`
    : `<div class="campo-endereco">
        ${n.pagina.tipo === "INICIO" ? icone("lupa") : icone("cadeado")}
        <button class="dominio" data-acao="editar-endereco">${esc(n.pagina.tipo === "INICIO" ? "Buscar ou digitar endereço" : n.pagina.dominio)}</button>
        ${n.pagina.tipo === "INICIO" ? "" : `<button class="recarregar" data-acao="atualizar-pagina" aria-label="Atualizar">${icone("recarregar")}</button>`}
      </div>`;
  return `<section class="app-tela navegador-tela ${privada ? "privada" : ""}">
    <div class="carregando" data-carga="${tela.carregarPagina}"></div>
    <div class="rolagem" data-rolagem="nav-${n.abaAtual}-${esc(n.pagina.url)}">
      <div class="pagina">${paginaHtml(n, privada)}
        ${n.pagina.tipo === "SITE" ? `<p class="nota-rodape">Carregada às ${new Date(n.carregadaEm).toLocaleTimeString("pt-BR")}${n.atualizacoes ? ` · atualizada ${n.atualizacoes}×` : ""}</p>` : ""}
      </div>
    </div>
    <div class="barra-navegador">
      ${endereco}
      <div class="ferramentas">
        <button data-acao="voltar-pagina" ${n.podeVoltar ? "" : "disabled"} aria-label="Voltar">${icone("voltar")}</button>
        <button data-acao="avancar-pagina" ${n.podeAvancar ? "" : "disabled"} aria-label="Avançar">${icone("avancar")}</button>
        <button data-acao="favoritar" ${n.pagina.tipo === "INICIO" ? "disabled" : ""} aria-pressed="${n.favorita}" aria-label="${n.favorita ? "Tirar dos favoritos" : "Favoritar"}">
          ${n.favorita ? icone("estrela") : `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2.5l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.1 1.2-6.5-4.8-4.6 6.6-.9zm0 4.5-1.6 3.3-3.6.5 2.6 2.5-.6 3.6 3.2-1.7 3.2 1.7-.6-3.6 2.6-2.5-3.6-.5z"/></svg>`}
        </button>
        <button class="contagem" data-acao="ver-abas" aria-label="${n.abas.length} abas abertas">${icone("abas")}<b>${n.abas.length}</b></button>
      </div>
    </div>
  </section>`;
}

function visaoAbas(n) {
  const cartoes = n.abas.map((a) => `<div class="cartao-aba ${a.id === n.abaAtual ? "atual" : ""} ${a.privada ? "privada" : ""}">
      <button class="topo-aba" data-cor-site="${corDoSite(a.dominio)}" data-acao="selecionar-aba" data-id="${a.id}">${esc(a.titulo)}</button>
      <button class="corpo-aba" data-acao="selecionar-aba" data-id="${a.id}">
        <strong>${a.privada ? "Aba privada" : esc(a.dominio || "Página inicial")}</strong>${esc(a.url === "about:blank" ? "" : a.url)}
      </button>
      <button class="fechar-aba" data-acao="fechar-aba" data-id="${a.id}" aria-label="Fechar aba">${icone("fechar")}</button>
    </div>`).join("");
  const atualPrivada = n.abas.find((a) => a.id === n.abaAtual)?.privada;
  return `<section class="app-tela visao-abas ${atualPrivada ? "privada" : ""}">
    <div class="grade-abas">${cartoes}</div>
    <div class="barra-abas">
      <button data-acao="nova-aba">${icone("mais")}Nova</button>
      <button data-acao="nova-aba-privada">${icone("cadeado")}Privada</button>
      <button data-acao="fechar-visao-abas">OK</button>
    </div>
  </section>`;
}

function navegou(ok) {
  if (ok) {
    tela.carregarPagina++;
    tela.editandoEndereco = false;
    tela.visaoAbas = false;
  }
}

export function enviarEndereco(url) {
  return agir("POST", "/api/navegador/pagina", { url }).then(navegou);
}

export const acoesNavegador = {
  "abrir-link": (el) => {
    const url = el.dataset.url;
    if (!SITES_DO_SIMULADOR.test(url)) {
      window.open(url, "_blank", "noopener");
      return;
    }
    return enviarEndereco(url);
  },
  "editar-endereco": () => { tela.editandoEndereco = true; },
  "cancelar-endereco": () => { tela.editandoEndereco = false; },
  "atualizar-pagina": () => agir("POST", "/api/navegador/atualizar").then(navegou),
  "voltar-pagina": () => agir("POST", "/api/navegador/voltar").then(navegou),
  "avancar-pagina": () => agir("POST", "/api/navegador/avancar").then(navegou),
  favoritar: () => agir("POST", "/api/navegador/favorito"),
  "ver-abas": () => { tela.visaoAbas = true; },
  "fechar-visao-abas": () => { tela.visaoAbas = false; },
  "selecionar-aba": (el) => agir("POST", `/api/navegador/abas/${el.dataset.id}/selecao`).then(navegou),
  "fechar-aba": (el) => agir("DELETE", `/api/navegador/abas/${el.dataset.id}`),
  "nova-aba": () => agir("POST", "/api/navegador/abas", { privada: false }).then(navegou),
  "nova-aba-privada": () => agir("POST", "/api/navegador/abas", { privada: true }).then(navegou),
};

export function temPaginaInicial() {
  return loja.estado?.navegador?.pagina?.tipo === "INICIO";
}
