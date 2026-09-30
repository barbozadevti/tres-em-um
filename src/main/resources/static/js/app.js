// Monta o aparelho: bloqueio, início, apps, ilha dinâmica, chamada e os painéis do palco.
import { loja, tela, assinar, agir, buscarEstado, carregarCatalogos, iniciarConsultas, posicaoMusica, duracaoChamada } from "./estado.js";
import { esc, icone, iconeBussola, iconesStatus, horaMinuto, dataPorExtenso, mmss, aplicarCores, avisar } from "./ui.js";
import * as som from "./som.js";
import { renderMusica, acoesMusica, prepararBarra, atualizarTempos } from "./musica.js";
import { renderTelefone, renderChamada, renderFimDeChamada, acoesTelefone, mascarar } from "./telefone.js";
import { renderNavegador, acoesNavegador, enviarEndereco } from "./navegador.js";
import { renderUml, renderAjustes, acoesExtras } from "./extras.js";

const $ = (id) => document.getElementById(id);
const el = {
  apresentacao: $("apresentacao"), porDentro: $("por-dentro"), escala: $("moldura-escala"), aparelho: $("aparelho"),
  status: $("barra-status"), ilha: $("ilha"), conteudo: $("conteudo"), sobreposicao: $("sobreposicao"), indicador: $("indicador-inicio"),
};

/** Troca o HTML de uma região só se mudou, preservando a rolagem. */
function regiao(alvo, html) {
  if (alvo._html === html) return false;
  const rolagens = {};
  alvo.querySelectorAll("[data-rolagem]").forEach((r) => { rolagens[r.dataset.rolagem] = r.scrollTop; });
  alvo.innerHTML = html;
  alvo._html = html;
  alvo.querySelectorAll("[data-rolagem]").forEach((r) => {
    if (rolagens[r.dataset.rolagem] !== undefined) r.scrollTop = rolagens[r.dataset.rolagem];
  });
  aplicarCores(alvo);
  return true;
}

// ----- Telas do sistema -----

function papelDeParede() {
  return `<div class="papel-de-parede" aria-hidden="true"></div>`;
}

function bloqueio(e) {
  const t = e.telefone;
  const m = e.musica;
  const perdidas = t.recentes.filter((c) => c.desfecho === "Perdida").slice(0, t.chamadasPerdidas);
  const notificacoes = [];
  if (m.faixa) {
    notificacoes.push(`<button class="notificacao" data-acao="abrir-app" data-app="musica">
      <span class="app-mini capa" data-c1="${esc(m.faixa.cor)}" data-c2="${esc(m.faixa.corSecundaria)}">${icone("nota")}</span>
      <span><strong>${esc(m.faixa.titulo)}</strong><small>${esc(m.faixa.autor)} · ${m.estado === "TOCANDO" ? "tocando" : "pausada"}</small></span>
      <span class="quando">Música</span></button>`);
  }
  if (perdidas.length) {
    notificacoes.push(`<button class="notificacao" data-acao="abrir-app" data-app="telefone" data-aba="recentes">
      <span class="app-mini i-telefone">${icone("telefone")}</span>
      <span><strong>${perdidas.length === 1 ? "Chamada perdida" : `${perdidas.length} chamadas perdidas`}</strong>
      <small>${esc([...new Set(perdidas.map((c) => c.nome))].join(", "))}</small></span><span class="quando">Telefone</span></button>`);
  }
  if (t.recadosNaoOuvidos) {
    notificacoes.push(`<button class="notificacao" data-acao="abrir-app" data-app="telefone" data-aba="correio">
      <span class="app-mini i-recados">${icone("correio")}</span>
      <span><strong>Correio de voz</strong><small>${t.recadosNaoOuvidos} ${t.recadosNaoOuvidos === 1 ? "recado novo" : "recados novos"}</small></span>
      <span class="quando">Telefone</span></button>`);
  }
  return `${papelDeParede()}
  <div class="bloqueio" id="bloqueio">
    <div class="data" data-relogio="data">${esc(dataPorExtenso())}</div>
    <div class="hora-grande" data-relogio="hora">${horaMinuto()}</div>
    <div class="notificacoes">${notificacoes.join("")}</div>
    <div class="deslizar" id="deslizar">
      <span class="rotulo">deslize para desbloquear</span>
      <button class="puxador" id="puxador" aria-label="Desbloquear">${icone("seta")}</button>
    </div>
  </div>`;
}

function inicio(e) {
  const m = e.musica;
  const t = e.telefone;
  const tocando = m.estado === "TOCANDO";
  const widget = m.faixa
    ? `<div class="widget-musica">
        <button class="capa" data-acao="abrir-app" data-app="musica" data-c1="${esc(m.faixa.cor)}" data-c2="${esc(m.faixa.corSecundaria)}" aria-label="Abrir Música">${icone("nota")}</button>
        <div><small>${tocando ? "Tocando agora" : "Pausada"}</small><strong>${esc(m.faixa.titulo)}</strong><span>${esc(m.faixa.autor)}</span>
          <div class="controles-widget">
            <button data-acao="anterior" aria-label="Anterior">${icone("anterior")}</button>
            <button data-acao="alternar-play" aria-label="${tocando ? "Pausar" : "Tocar"}">${icone(tocando ? "pausar" : "tocar")}</button>
            <button data-acao="proxima" aria-label="Próxima">${icone("proxima")}</button>
          </div></div>
      </div>`
    : `<button class="widget-musica" data-acao="abrir-app" data-app="musica">
        <span class="capa" data-c1="#f43f5e" data-c2="#8b5cf6">${icone("nota")}</span>
        <span><small>Música</small><strong>Toque uma música</strong><span>6 clássicos de domínio público</span></span>
      </button>`;
  const app = (id, rotulo, classe, conteudoIcone, selo = 0, extra = "") =>
    `<button class="app" data-acao="abrir-app" data-app="${id}" ${extra} aria-label="${rotulo}${selo ? `, ${selo} novidades` : ""}">
      <span class="icone-app ${classe}">${conteudoIcone}</span><span class="rotulo-app">${rotulo}</span>${selo ? `<span class="selo">${selo}</span>` : ""}</button>`;
  return `${papelDeParede()}
  <div class="inicio">
    ${widget}
    <div class="grade-apps">
      ${app("telefone", "Recados", "i-recados", icone("correio"), t.recadosNaoOuvidos, 'data-aba="correio"')}
      ${app("navegador", "Clima", "i-clima", '<b class="texto-icone">27°</b>', 0, 'data-url="clima.exemplo"')}
      ${app("navegador", "Keynote", "i-keynote", '<b class="texto-icone">2007</b>', 0, 'data-url="keynote.exemplo"')}
      ${app("ajustes", "Ajustes", "i-ajustes", icone("ajustes"))}
    </div>
    <div class="pontos-pagina" aria-hidden="true"><i></i><i></i></div>
    <div class="dock">
      ${app("telefone", "Telefone", "i-telefone", icone("telefone"), t.chamadasPerdidas + t.recadosNaoOuvidos)}
      ${app("navegador", "Navegador", "i-navegador", iconeBussola())}
      ${app("musica", "Música", "i-musica", icone("nota"))}
      ${app("uml", "Diagrama", "i-uml", icone("diagrama"))}
    </div>
  </div>`;
}

function appAtual(e) {
  switch (tela.app) {
    case "musica": return renderMusica(e);
    case "telefone": return renderTelefone(e);
    case "navegador": return renderNavegador(e);
    case "uml": return renderUml();
    case "ajustes": return renderAjustes();
    default: return inicio(e);
  }
}

function telaClara(e) {
  if (tela.modo !== "app") return false;
  if (e.telefone.chamada && !tela.chamadaMinimizada) return false;
  if (tela.app === "musica" && tela.abaMusica === "tocando" && e.musica.faixa) return false;
  if (tela.app === "navegador") {
    const aba = e.navegador.abas.find((a) => a.id === e.navegador.abaAtual);
    return !aba?.privada;
  }
  return true;
}

function ilha(e) {
  const c = e.telefone.chamada;
  const m = e.musica;
  el.ilha.classList.remove("expandida", "chamada-entrando");
  if (c) {
    el.ilha.classList.add("expandida");
    return `<span class="icone-fone">${icone("telefone")}</span>
      <span class="tempo-ilha" data-relogio="ilha-chamada">${c.estado === "EM_ANDAMENTO" ? "0:00" : c.estado === "TOCANDO" ? "tocando" : "chamando"}</span>`;
  }
  if (m.estado === "TOCANDO" && m.faixa) {
    el.ilha.classList.add("expandida");
    return `<span class="capa-mini" data-c1="${esc(m.faixa.cor)}" data-c2="${esc(m.faixa.corSecundaria)}"></span>
      <span class="onda" data-c1="${esc(m.faixa.cor)}"><i></i><i></i><i></i><i></i></span>`;
  }
  return "";
}

// ----- Painéis do palco -----

function montarApresentacao() {
  el.apresentacao.innerHTML = `
    <div class="marca"><img src="img/icone.svg" alt=""><div><strong>Três em Um</strong><span>iPhone modelado em UML · Java 21 + Spring Boot</span></div></div>
    <h1 class="chamada-keynote"><span class="p1">Um iPod.</span> <span class="p2">Um telefone.</span> <span class="p3">Um navegador.</span> <span class="p4">Um só aparelho.</span></h1>
    <p class="texto-apoio">Na apresentação de 2007, o iPhone foi anunciado como três aparelhos em um. Aqui, cada papel é uma
      <code>interface</code> Java, e o aparelho ao lado roda o domínio de verdade, no servidor.</p>
    <div class="cartao-painel">
      <h2>Os três papéis</h2>
      <div class="papeis">
        <div class="papel p-musica" data-papel="ReprodutorMusical"><span class="ponto"></span><code>ReprodutorMusical</code><small>tocar · pausar</small></div>
        <div class="papel p-telefone" data-papel="AparelhoTelefonico"><span class="ponto"></span><code>AparelhoTelefonico</code><small>ligar · atender</small></div>
        <div class="papel p-navegador" data-papel="NavegadorInternet"><span class="ponto"></span><code>NavegadorInternet</code><small>exibirPagina</small></div>
      </div>
    </div>
    <div class="cartao-painel">
      <h2>Experimente</h2>
      <ol class="roteiro">
        <li><span><b>Desbloqueie</b> o aparelho e toque uma música.</span></li>
        <li><span>Com a música tocando, <b>simule uma ligação</b>: ela pausa e volta sozinha quando a chamada termina.</span></li>
        <li><span>No <b>Navegador</b>, digite <code>keynote.exemplo</code> ou faça uma busca.</span></li>
      </ol>
      <div class="botoes-simulador">
        <button class="botao-palco verde" data-acao="simular-palco" data-numero="(27) 99901-2233">${icone("chegando")}Mãe está ligando</button>
        <button class="botao-palco" data-acao="simular-palco" data-numero="(21) 3555-0199">${icone("chegando")}Número desconhecido</button>
      </div>
    </div>
    <div class="links-palco">
      <a href="https://github.com/barbozadevti/tres-em-um" target="_blank" rel="noopener">${icone("github")}Código no GitHub</a>
      <a href="/swagger-ui.html" target="_blank" rel="noopener">${icone("codigo")}API (Swagger)</a>
      <a href="#" data-acao="abrir-app" data-app="uml">${icone("diagrama")}Diagrama UML</a>
    </div>`;
  el.porDentro.innerHTML = `
    <div class="cartao-painel por-dentro">
      <div class="cabeca"><h2>Por dentro</h2><span class="ao-vivo">ao vivo</span>
        <button class="botao-palco botao-som" data-acao="alternar-som" id="botao-som"></button></div>
      <p class="explicacao-rastro">Cada toque vira uma chamada de método em um dos papéis. Um proxy dinâmico
        (<code>java.lang.reflect.Proxy</code>) registra tudo no servidor.</p>
      <ol class="rastro" id="rastro"></ol>
      <p class="legenda-proxy">Em lilás, o motivo quando é o próprio aparelho que age, como pausar a música porque chegou uma ligação.</p>
    </div>`;
  atualizarBotaoSom();
}

function atualizarBotaoSom() {
  const mudo = som.estaMudo();
  $("botao-som").innerHTML = `${icone(mudo ? "semsom" : "som")}${mudo ? "Som desligado" : "Som ligado"}`;
}

let ultimoRastro = 0;
let destaque;
function atualizarRastro(e) {
  const lista = $("rastro");
  const topo = e.rastro[0]?.numero ?? 0;
  if (lista._topo === topo && lista.childElementCount) return;
  lista._topo = topo;
  if (!e.rastro.length) {
    lista.innerHTML = `<li class="vazio">Nenhuma chamada ainda. Use o aparelho e acompanhe aqui.</li>`;
    ultimoRastro = 0;
    return;
  }
  lista.innerHTML = e.rastro.map((r) => {
    const ponto = r.assinatura.indexOf(".");
    const hora = new Date(r.quando).toLocaleTimeString("pt-BR");
    return `<li class="r-${esc(r.papel)} ${r.numero > ultimoRastro && ultimoRastro ? "novo" : ""}">
      <div class="linha1"><time>${hora}</time><span><span class="papel-nome">${esc(r.assinatura.slice(0, ponto))}</span>.<span class="metodo">${esc(r.assinatura.slice(ponto + 1))}</span></span></div>
      ${r.motivo ? `<div class="motivo">↳ o aparelho agiu sozinho: ${esc(r.motivo)}</div>` : ""}
      ${r.erro ? `<div class="erro">✕ recusado: ${esc(r.erro)}</div>` : ""}
    </li>`;
  }).join("");
  if (topo > ultimoRastro && ultimoRastro) {
    const papel = e.rastro[0].papel;
    document.querySelectorAll(".papel").forEach((p) => p.classList.toggle("ativo", p.dataset.papel === papel));
    clearTimeout(destaque);
    destaque = setTimeout(() => document.querySelectorAll(".papel").forEach((p) => p.classList.remove("ativo")), 1600);
  }
  ultimoRastro = topo;
}

// ----- Desenho geral -----

function redesenhar() {
  const e = loja.estado;
  if (!e) return;
  const clara = telaClara(e);
  el.status.classList.toggle("escura", clara);
  el.indicador.classList.toggle("escuro", clara);
  el.indicador.hidden = tela.modo === "bloqueio";
  regiao(el.status, `<span data-relogio="hora-status">${tela.modo === "bloqueio" ? "" : horaMinuto()}</span><span class="icones">${iconesStatus()}</span>`);
  regiao(el.ilha, ilha(e));
  if (tela.modo === "bloqueio") {
    if (regiao(el.conteudo, bloqueio(e))) prepararDeslizar();
  } else {
    regiao(el.conteudo, tela.modo === "inicio" ? inicio(e) : appAtual(e));
  }
  regiao(el.sobreposicao, renderChamada(e) + renderFimDeChamada());
  prepararBarra(el.conteudo);
  const campo = el.conteudo.querySelector('[data-formulario="endereco"] input');
  if (campo && document.activeElement !== campo) { campo.focus(); campo.select(); }
  document.querySelectorAll('[data-acao="simular-palco"]').forEach((b) => { b.disabled = !!e.telefone.chamada; });
  atualizarRastro(e);
  tique();
}

/** Valores que andam com o tempo, atualizados sem redesenhar a tela. */
function tique() {
  const agora = new Date();
  document.querySelectorAll('[data-relogio="hora"]').forEach((n) => { n.textContent = horaMinuto(agora); });
  document.querySelectorAll('[data-relogio="hora-status"]').forEach((n) => { if (n.textContent) n.textContent = horaMinuto(agora); });
  atualizarTempos(el.conteudo);
  const chamada = loja.estado?.telefone?.chamada;
  if (chamada?.estado === "EM_ANDAMENTO") {
    const texto = mmss(duracaoChamada());
    document.querySelectorAll('[data-relogio="chamada"], [data-relogio="ilha-chamada"]').forEach((n) => { n.textContent = texto; });
  }
}

function visualizador() {
  const canvas = document.getElementById("visualizador");
  if (canvas) {
    const g = canvas.getContext("2d");
    const niveis = som.niveis(24);
    g.clearRect(0, 0, canvas.width, canvas.height);
    if (niveis) {
      const largura = canvas.width / niveis.length;
      g.fillStyle = "rgba(255,255,255,.55)";
      niveis.forEach((v, i) => {
        if (v < 0.02) return;
        const h = v * canvas.height;
        g.fillRect(i * largura + 2, canvas.height - h, largura - 4, h);
      });
    }
  }
  requestAnimationFrame(visualizador);
}

// ----- Som em sintonia com o servidor -----

let tomAtual = null;
function sincronizarSom(e) {
  const m = e.musica;
  if (m.estado === "TOCANDO" && m.faixa) {
    const soando = som.posicaoAtual();
    if (!soando || soando.id !== m.faixa.id || Math.abs(soando.posicao - m.posicao) > 0.4) som.tocarFaixa(m.faixa, m.posicao);
  } else {
    som.pararFaixa();
  }
  const c = e.telefone.chamada;
  const tom = c?.estado === "CHAMANDO" ? "chamando" : c?.estado === "TOCANDO" ? "tocando" : null;
  if (tom !== tomAtual) {
    som.pararTonsDeChamada();
    if (tom === "chamando") som.iniciarTomDeChamando();
    if (tom === "tocando") som.iniciarToque();
    tomAtual = tom;
  }
}

let chamadaAnterior = null;
let fimTemporizador;
function observarChamada(e) {
  const c = e.telefone.chamada;
  if (c && c.id !== chamadaAnterior?.id) {
    tela.chamadaMinimizada = false;
    tela.tecladoNaChamada = false;
    som.calar();
  }
  if (!c && chamadaAnterior) {
    const registro = e.telefone.recentes.find((r) => r.id === chamadaAnterior.id);
    if (registro) {
      tela.fimDeChamada = { nome: registro.nome, detalhe: registro.duracao > 0 ? mmss(registro.duracao) : registro.desfecho };
      clearTimeout(fimTemporizador);
      fimTemporizador = setTimeout(() => { tela.fimDeChamada = null; redesenhar(); }, 1600);
    }
  }
  chamadaAnterior = c;
}

// ----- Desbloquear -----

function desbloquear() {
  som.ativar();
  const b = document.getElementById("bloqueio");
  b?.classList.add("saindo");
  setTimeout(() => { tela.modo = tela.app ? "app" : "inicio"; redesenhar(); }, 320);
}

function prepararDeslizar() {
  const trilho = document.getElementById("deslizar");
  const puxador = document.getElementById("puxador");
  if (!trilho || !puxador) return;
  let inicioX = null;
  let deslocamento = 0;
  const limite = () => trilho.clientWidth - puxador.offsetWidth - 12;
  puxador.addEventListener("pointerdown", (ev) => { inicioX = ev.clientX; deslocamento = 0; puxador.setPointerCapture(ev.pointerId); });
  puxador.addEventListener("pointermove", (ev) => {
    if (inicioX === null) return;
    const escala = el.aparelho.getBoundingClientRect().width / el.aparelho.offsetWidth;
    deslocamento = Math.max(0, Math.min(limite(), (ev.clientX - inicioX) / escala));
    puxador.style.transform = `translateX(${deslocamento}px)`;
  });
  puxador.addEventListener("pointerup", () => {
    if (inicioX === null) return;
    inicioX = null;
    // Arrastar até o fim desbloqueia; um clique simples também (acessível por teclado e mouse).
    if (deslocamento > limite() * 0.7 || deslocamento < 4) {
      puxador.style.transform = `translateX(${limite()}px)`;
      desbloquear();
    } else {
      puxador.style.transition = "transform .25s";
      puxador.style.transform = "translateX(0)";
      setTimeout(() => { puxador.style.transition = ""; }, 260);
    }
  });
  puxador.addEventListener("keydown", (ev) => { if (ev.key === "Enter" || ev.key === " ") { ev.preventDefault(); desbloquear(); } });
}

// ----- Ações -----

function abrirApp(elemento) {
  const app = elemento.dataset.app;
  som.ativar();
  if (tela.modo === "bloqueio") desbloquear();
  tela.app = app;
  tela.modo = tela.modo === "bloqueio" ? "bloqueio" : "app";
  if (elemento.dataset.aba) { tela.abaTelefone = elemento.dataset.aba; tela.contatoAberto = null; }
  if (app === "musica") tela.abaMusica = loja.estado.musica.faixa ? "tocando" : "biblioteca";
  if (app === "telefone" && elemento.dataset.aba === "recentes" && loja.estado.telefone.chamadasPerdidas) agir("POST", "/api/telefone/recentes/visto");
  if (elemento.dataset.url) return enviarEndereco(elemento.dataset.url);
}

const acoes = {
  ...acoesMusica, ...acoesTelefone, ...acoesNavegador, ...acoesExtras,
  "abrir-app": abrirApp,
  "simular-palco": (b) => {
    som.ativar();
    return agir("POST", "/api/simulador/chamada-recebida", { numero: b.dataset.numero });
  },
  "alternar-som": () => { som.ativar(); som.alternarMudo(); atualizarBotaoSom(); },
  reiniciar: async () => {
    som.pararFaixa();
    som.calar();
    await agir("POST", "/api/aparelho/reinicio");
    Object.assign(tela, { modo: "bloqueio", app: null, abaTelefone: "teclado", abaMusica: "biblioteca", digitos: "", visaoAbas: false, editandoEndereco: false });
    avisar("Aparelho reiniciado.");
  },
};

document.addEventListener("click", async (ev) => {
  const alvo = ev.target.closest("[data-acao]");
  if (!alvo || alvo.disabled) return;
  const acao = acoes[alvo.dataset.acao];
  if (!acao) return;
  ev.preventDefault();
  som.ativar();
  const resultado = acao(alvo, ev);
  redesenhar();
  if (resultado?.then) { await resultado; redesenhar(); }
});

document.addEventListener("submit", async (ev) => {
  const form = ev.target.closest('[data-formulario="endereco"]');
  if (!form) return;
  ev.preventDefault();
  const url = form.url.value.trim();
  if (url) await enviarEndereco(url);
  redesenhar();
});

document.addEventListener("keydown", (ev) => {
  if (ev.target.closest("input")) {
    if (ev.key === "Escape") { tela.editandoEndereco = false; redesenhar(); }
    return;
  }
  if (tela.modo === "app" && tela.app === "telefone" && tela.abaTelefone === "teclado" && !loja.estado?.telefone?.chamada) {
    if (/^[0-9*#]$/.test(ev.key)) {
      som.tom(ev.key);
      if (tela.digitos.length < 15) tela.digitos += ev.key;
      redesenhar();
      return;
    }
    if (ev.key === "Backspace") { tela.digitos = tela.digitos.slice(0, -1); redesenhar(); return; }
    if (ev.key === "Enter" && tela.digitos) { acoes["ligar-digitos"](); return; }
  }
  if (ev.key === "Escape" && tela.modo === "app") { irParaInicio(); }
});

function irParaInicio() {
  const c = loja.estado?.telefone?.chamada;
  if (c && !tela.chamadaMinimizada) {
    tela.chamadaMinimizada = true;
  } else if (tela.modo === "app") {
    if (tela.visaoAbas || tela.editandoEndereco) { tela.visaoAbas = false; tela.editandoEndereco = false; }
    tela.modo = "inicio";
    tela.app = null;
  }
  redesenhar();
}

el.indicador.addEventListener("click", irParaInicio);
el.ilha.addEventListener("click", () => {
  const e = loja.estado;
  if (e.telefone.chamada) { tela.chamadaMinimizada = false; }
  else if (e.musica.estado === "TOCANDO" && tela.modo !== "bloqueio") { tela.modo = "app"; tela.app = "musica"; tela.abaMusica = "tocando"; }
  redesenhar();
});
document.addEventListener("redesenhar", redesenhar);

// ----- Tamanho do aparelho: cabe na altura da janela -----

function ajustarEscala() {
  const largura = 402;
  const altura = 860;
  const disponivelAltura = window.innerHeight - 56;
  const disponivelLargura = Math.min(window.innerWidth - 32, el.escala.parentElement.clientWidth || largura);
  const escala = Math.min(1, disponivelAltura / altura, disponivelLargura / largura);
  el.aparelho.style.transform = `scale(${escala})`;
  el.escala.style.width = `${largura * escala}px`;
  el.escala.style.height = `${altura * escala}px`;
}
window.addEventListener("resize", ajustarEscala);

// ----- Início -----

assinar((e) => {
  observarChamada(e);
  sincronizarSom(e);
  redesenhar();
});

montarApresentacao();
ajustarEscala();
await carregarCatalogos();
await buscarEstado();
iniciarConsultas();
setInterval(tique, 250);
requestAnimationFrame(visualizador);
window.tresEmUm = { tela, loja, mascarar };
