// Utilitários de tela: escape de HTML, formatação, ícones e avisos.

export function esc(valor) {
  return String(valor ?? "")
    .replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;").replaceAll("'", "&#39;");
}

/** Segundos em m:ss. */
export function mmss(segundos) {
  const s = Math.max(0, Math.floor(segundos));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
}

export function horaMinuto(data = new Date()) {
  return data.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

export function dataPorExtenso(data = new Date()) {
  const texto = data.toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" });
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

/** "agora", "há 5 min", "14:30", "ontem", "segunda-feira"... como na lista de recentes. */
export function quandoFoi(iso) {
  const data = new Date(iso);
  const minutos = Math.round((Date.now() - data.getTime()) / 60000);
  if (minutos < 1) return "agora";
  if (minutos < 60) return `há ${minutos} min`;
  const hoje = new Date();
  if (data.toDateString() === hoje.toDateString()) return horaMinuto(data);
  const ontem = new Date(hoje.getTime() - 86400000);
  if (data.toDateString() === ontem.toDateString()) return "ontem";
  return data.toLocaleDateString("pt-BR", { weekday: "long" });
}

export function iniciais(nome) {
  const partes = String(nome).trim().split(/\s+/);
  if (/^[\d(+]/.test(partes[0])) return "";
  return (partes[0][0] + (partes.length > 1 ? partes.at(-1)[0] : "")).toUpperCase();
}

/** Aplica cores vindas dos dados via CSSOM (sem atributo style no HTML). */
export function aplicarCores(raiz) {
  raiz.querySelectorAll("[data-c1]").forEach((el) => {
    el.style.setProperty("--c1", el.dataset.c1);
    if (el.dataset.c2) el.style.setProperty("--c2", el.dataset.c2);
  });
  raiz.querySelectorAll("[data-cor]").forEach((el) => el.style.setProperty("--cor", el.dataset.cor));
  raiz.querySelectorAll("[data-cor-site]").forEach((el) => el.style.setProperty("--cor-site", el.dataset.corSite));
  raiz.querySelectorAll("[data-fundo]").forEach((el) => { el.style.background = el.dataset.fundo; });
}

let temporizadorAviso;
export function avisar(texto, tipo = "") {
  const caixa = document.getElementById("avisos");
  caixa.innerHTML = `<div class="aviso ${tipo}">${esc(texto)}</div>`;
  clearTimeout(temporizadorAviso);
  temporizadorAviso = setTimeout(() => { caixa.innerHTML = ""; }, 3200);
}

// Ícones desenhados para o projeto (viewBox 24x24, preenchimento pela cor do texto).
const caminhos = {
  telefone: "M6.6 10.8a15.2 15.2 0 0 0 6.6 6.6l2.2-2.2c.3-.3.7-.4 1-.2 1.1.4 2.3.6 3.6.6.6 0 1 .4 1 1V20c0 .6-.4 1-1 1A17 17 0 0 1 3 4c0-.6.4-1 1-1h3.5c.6 0 1 .4 1 1 0 1.3.2 2.5.6 3.6.1.3 0 .7-.2 1z",
  desligar: "M12 9c-1.6 0-3.2.3-4.6.7v3.1c0 .4-.2.7-.6.9-1 .5-1.9 1.1-2.6 1.8-.2.2-.4.3-.7.3s-.5-.1-.7-.3L.3 13.1a1 1 0 0 1 0-1.4C3.3 8.8 7.4 7 12 7s8.7 1.8 11.7 4.7a1 1 0 0 1 0 1.4l-2.5 2.4c-.2.2-.4.3-.7.3s-.5-.1-.7-.3c-.8-.7-1.7-1.3-2.6-1.8-.4-.2-.6-.5-.6-.9V9.7C15.2 9.3 13.6 9 12 9z",
  nota: "M9 18.5a3 3 0 1 1-2-2.83V5.5l12-2.5v12.5a3 3 0 1 1-2-2.83V7.1l-8 1.7z",
  tocar: "M8 5.14v13.72c0 .8.87 1.3 1.56.88l11-6.86a1 1 0 0 0 0-1.76l-11-6.86A1.02 1.02 0 0 0 8 5.14z",
  pausar: "M7 4h3.5a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1zm6.5 0H17a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1h-3.5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
  proxima: "M3 6.2v11.6c0 .8.9 1.3 1.6.8l8.4-5.8a1 1 0 0 0 0-1.6L4.6 5.4C3.9 4.9 3 5.4 3 6.2zm10 0v11.6c0 .8.9 1.3 1.6.8l8.4-5.8a1 1 0 0 0 0-1.6l-8.4-5.8c-.7-.5-1.6 0-1.6.8z",
  anterior: "M21 6.2v11.6c0 .8-.9 1.3-1.6.8L11 12.8a1 1 0 0 1 0-1.6l8.4-5.8c.7-.5 1.6 0 1.6.8zm-10 0v11.6c0 .8-.9 1.3-1.6.8L1 12.8a1 1 0 0 1 0-1.6l8.4-5.8c.7-.5 1.6 0 1.6.8z",
  aleatorio: "M17 3l4 4-4 4V8h-2.3c-1 0-1.9.5-2.4 1.3l-4.9 7.4A4.8 4.8 0 0 1 3.4 19H2v-2h1.4c1 0 1.9-.5 2.4-1.3l4.9-7.4A4.8 4.8 0 0 1 14.7 6H17V3zm0 10l4 4-4 4v-3h-2.3a4.8 4.8 0 0 1-3.7-1.8l1.2-1.8c.5.9 1.5 1.6 2.5 1.6H17v-3zM2 6h1.4c1.6 0 3 .7 3.9 2l-1.2 1.8C5.6 8.8 4.6 8 3.4 8H2V6z",
  repetir: "M17 2l4 4-4 4V7H7a2 2 0 0 0-2 2v3H3V9a4 4 0 0 1 4-4h10V2zM7 22l-4-4 4-4v3h10a2 2 0 0 0 2-2v-3h2v3a4 4 0 0 1-4 4H7v3z",
  estrela: "M12 2.5l2.9 6 6.6.9-4.8 4.6 1.2 6.5L12 17.4l-5.9 3.1 1.2-6.5-4.8-4.6 6.6-.9z",
  relogio: "M12 2a10 10 0 1 1 0 20 10 10 0 0 1 0-20zm0 2a8 8 0 1 0 0 16 8 8 0 0 0 0-16zm1 3v4.6l3.2 1.9-1 1.7L11 12.7V7h2z",
  pessoa: "M12 12a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9zm0 2c-4.4 0-8 2.2-8 5v2h16v-2c0-2.8-3.6-5-8-5z",
  teclado: "M6 3.5a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm6 0a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm6 0a2 2 0 1 1 0 4 2 2 0 0 1 0-4zM6 10a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm6 0a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm6 0a2 2 0 1 1 0 4 2 2 0 0 1 0-4zM6 16.5a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm6 0a2 2 0 1 1 0 4 2 2 0 0 1 0-4zm6 0a2 2 0 1 1 0 4 2 2 0 0 1 0-4z",
  correio: "M6.5 7a5 5 0 0 1 4 8h3a5 5 0 1 1 4 2h-11a5 5 0 1 1 0-10zm0 2a3 3 0 1 0 0 6 3 3 0 0 0 0-6zm11 0a3 3 0 1 0 0 6 3 3 0 0 0 0-6z",
  mudo: "M12 2a3 3 0 0 1 3 3v5.2l-6-6A3 3 0 0 1 12 2zM3.3 3.3l17.4 17.4-1.4 1.4-4-4a7 7 0 0 1-2.3.8V21h-2v-2.1A7 7 0 0 1 5 12h2a5 5 0 0 0 6.8 4.7l-1.5-1.5L12 15a3 3 0 0 1-3-3v-1.2L1.9 4.7l1.4-1.4zM19 12a7 7 0 0 1-.8 3.3l-1.5-1.5c.2-.6.3-1.2.3-1.8h2z",
  altofalante: "M3 9h4l5-4.5v15L7 15H3V9zm13.5 3a4.5 4.5 0 0 0-2.5-4v8a4.5 4.5 0 0 0 2.5-4zM14 3.2v2.1a7 7 0 0 1 0 13.4v2.1a9 9 0 0 0 0-17.6z",
  apagar: "M21 5H8.5L2 12l6.5 7H21a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1zm-3.3 10.3-1.4 1.4-2.3-2.3-2.3 2.3-1.4-1.4 2.3-2.3-2.3-2.3 1.4-1.4 2.3 2.3 2.3-2.3 1.4 1.4-2.3 2.3z",
  lixeira: "M9 3h6l1 2h4v2H4V5h4zM6 9h12l-1 12H7z",
  chegando: "M20 5.4 18.6 4 7 15.6V9H5v10h10v-2H8.4z",
  saindo: "M5 18.6 6.4 20 18 8.4V15h2V5H10v2h6.6z",
  voltar: "M15.5 4.5 8 12l7.5 7.5-1.5 1.5-9-9 9-9z",
  avancar: "M8.5 4.5 16 12l-7.5 7.5L10 21l9-9-9-9z",
  recarregar: "M17.7 6.3A8 8 0 1 0 20 12h-2a6 6 0 1 1-1.8-4.2L13 11h7V4z",
  abas: "M8 3h11a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2h-1v-2h1V5H8v1H6V5a2 2 0 0 1 2-2zM5 7h11a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zm0 2v10h11V9z",
  mais: "M11 4h2v7h7v2h-7v7h-2v-7H4v-2h7z",
  fechar: "M5.6 4.2 12 10.6l6.4-6.4 1.4 1.4-6.4 6.4 6.4 6.4-1.4 1.4-6.4-6.4-6.4 6.4-1.4-1.4 6.4-6.4-6.4-6.4z",
  cadeado: "M7 10V8a5 5 0 0 1 10 0v2h1a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V11a1 1 0 0 1 1-1h1zm2 0h6V8a3 3 0 0 0-6 0v2z",
  lupa: "M10 3a7 7 0 0 1 5.6 11.2l5.1 5.1-1.4 1.4-5.1-5.1A7 7 0 1 1 10 3zm0 2a5 5 0 1 0 0 10 5 5 0 0 0 0-10z",
  seta: "M13.2 5.2 20 12l-6.8 6.8-1.4-1.4 4.4-4.4H4v-2h12.2l-4.4-4.4z",
  diagrama: "M3 3h8v6H3zm10 0h8v6h-8zM8 15h8v6H8zM6 9h2v3h8V9h2v5h-5v1h-2v-1H6z",
  ajustes: "M13.9 2.5l.5 2.5c.6.2 1.2.5 1.7.9l2.4-.8 1.9 3.3-1.9 1.7a7 7 0 0 1 0 1.9l1.9 1.7-1.9 3.3-2.4-.8c-.5.4-1.1.7-1.7.9l-.5 2.5h-3.8l-.5-2.5a7 7 0 0 1-1.7-.9l-2.4.8-1.9-3.3 1.9-1.7a7 7 0 0 1 0-1.9L3.6 8.4l1.9-3.3 2.4.8c.5-.4 1.1-.7 1.7-.9l.5-2.5zM12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z",
  info: "M12 2a10 10 0 1 1 0 20 10 10 0 0 1 0-20zm-1 8v7h2v-7zm0-3v2h2V7z",
  codigo: "M8.6 16.6 4 12l4.6-4.6L7.2 6 1.2 12l6 6zm6.8 0L20 12l-4.6-4.6L16.8 6l6 6-6 6z",
  github: "M12 2a10 10 0 0 0-3.2 19.5c.5.1.7-.2.7-.5v-1.7c-2.8.6-3.4-1.3-3.4-1.3-.4-1.2-1.1-1.5-1.1-1.5-.9-.6.1-.6.1-.6 1 .1 1.5 1 1.5 1 .9 1.5 2.4 1.1 2.9.8.1-.7.4-1.1.6-1.3-2.2-.3-4.6-1.1-4.6-5 0-1.1.4-2 1-2.7-.1-.3-.4-1.3.1-2.7 0 0 .8-.3 2.8 1a9.6 9.6 0 0 1 5 0c1.9-1.3 2.8-1 2.8-1 .5 1.4.2 2.4.1 2.7.6.7 1 1.6 1 2.7 0 3.9-2.3 4.7-4.6 5 .4.3.7.9.7 1.9V21c0 .3.2.6.7.5A10 10 0 0 0 12 2z",
  som: "M3 9h4l5-4.5v15L7 15H3V9zm13.5 3a4.5 4.5 0 0 0-2.5-4v8a4.5 4.5 0 0 0 2.5-4z",
  semsom: "M3 9h4l5-4.5v15L7 15H3V9zm13.6-1.4L19 10l2.4-2.4 1.4 1.4-2.4 2.4 2.4 2.4-1.4 1.4-2.4-2.4-2.4 2.4-1.4-1.4 2.4-2.4-2.4-2.4z",
  sinal: "M2 16h3v4H2zm5-4h3v8H7zm5-4h3v12h-3zm5-4h3v16h-3z",
  onda: "M3 12h2l2-6 4 12 3-9 2 3h5v2h-6l-1-1.5-3 9-4-12-1.3 3.5H3z",
};

export function icone(nome, classe = "") {
  return `<svg viewBox="0 0 24 24" class="${classe}" aria-hidden="true"><path d="${caminhos[nome]}"/></svg>`;
}

/** Bússola do navegador (colorida, desenhada à parte). */
export function iconeBussola() {
  return `<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10" fill="none" stroke="#1a8cff" stroke-width="1.6"/>
    <path d="M12 12l5-5-3 7z" fill="#ff3b30"/><path d="M12 12l-5 5 3-7z" fill="#1a8cff"/></svg>`;
}

export function iconesStatus() {
  return `<svg viewBox="0 0 20 12" aria-hidden="true"><rect x="0" y="8" width="3" height="4" rx="1"/><rect x="5" y="5.5" width="3" height="6.5" rx="1"/><rect x="10" y="3" width="3" height="9" rx="1"/><rect x="15" y="0" width="3" height="12" rx="1"/></svg>
  <svg viewBox="0 0 17 12" aria-hidden="true"><path d="M8.5 2.3c2.3 0 4.4.9 6 2.4l1.3-1.4A10.4 10.4 0 0 0 8.5.4 10.4 10.4 0 0 0 1.2 3.3l1.3 1.4a8.6 8.6 0 0 1 6-2.4zm0 3.7c1.3 0 2.5.5 3.4 1.3l1.3-1.3a6.7 6.7 0 0 0-9.4 0l1.3 1.3c.9-.8 2.1-1.3 3.4-1.3zm0 3.6-2 2 2 2 2-2z"/></svg>
  <svg viewBox="0 0 27 12" aria-hidden="true"><rect x=".5" y=".5" width="23" height="11" rx="3.5" fill="none" stroke="currentColor" opacity=".45"/><rect x="2" y="2" width="17" height="8" rx="2"/><path d="M25 4v4c.8-.3 1.3-1.1 1.3-2S25.8 4.3 25 4z" opacity=".45"/></svg>`;
}
