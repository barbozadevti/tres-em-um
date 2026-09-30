// Apps "Diagrama" (o UML do próprio aparelho) e "Ajustes".
import { tela } from "./estado.js";
import { icone } from "./ui.js";

export function renderUml() {
  return `<section class="app-tela uml-tela">
    <div class="rolagem" data-rolagem="uml">
      <h1 class="titulo-grande">Diagrama</h1>
      <p class="uml-legenda">Como este aparelho foi modelado. Cada papel é uma interface; o <code>IPhone</code> implementa as três
        e delega a um especialista. Um teste automatizado lê este diagrama e confere, por reflexão, se ele continua igual ao código.</p>
      <div class="chips-uml"><span>ReprodutorMusical</span><span>AparelhoTelefonico</span><span>NavegadorInternet</span></div>
      <div class="uml-zoom">
        <button data-acao="zoom-menos" aria-label="Diminuir">−</button>
        <button data-acao="zoom-mais" aria-label="Aumentar">+</button>
        <button data-acao="zoom-ajustar">Ajustar</button>
      </div>
      <div class="uml-caixa"><img src="img/uml.svg" width="${tela.zoomUml}" alt="Diagrama de classes UML do Três em Um"></div>
    </div>
  </section>`;
}

export function renderAjustes() {
  const linha = (cor, nomeIcone, rotulo, valor, acao = "", extra = "") => `<li>
    <${acao ? "button" : "div"} class="linha-ios" ${acao ? `data-acao="${acao}"` : ""} ${extra}>
      <span class="ajuste-icone" data-fundo="${cor}">${icone(nomeIcone)}</span>
      <span class="texto"><strong>${rotulo}</strong></span>
      <span class="valor-ajuste">${valor}</span>
    </${acao ? "button" : "div"}></li>`;
  return `<section class="app-tela">
    <div class="rolagem" data-rolagem="ajustes">
      <h1 class="titulo-grande">Ajustes</h1>
      <div class="perfil-aparelho"><img src="img/icone.svg" alt=""><strong>Três em Um</strong><small>iPhone modelado em UML · simulação</small></div>
      <p class="subtitulo-secao">Sobre</p>
      <ul class="lista-ios">
        ${linha("#8b5cf6", "diagrama", "Papéis", "3 interfaces")}
        ${linha("#f97316", "codigo", "Domínio", "Java 21")}
        ${linha("#22c55e", "ajustes", "Servidor", "Spring Boot 4")}
        ${linha("#0ea5e9", "info", "Estado", "sessão, sem banco")}
      </ul>
      <p class="subtitulo-secao">Links</p>
      <ul class="lista-ios">
        ${linha("#111827", "github", "Código-fonte", "GitHub ↗", "abrir-externo", 'data-url="https://github.com/barbozadevti/tres-em-um"')}
        ${linha("#16a34a", "codigo", "API (Swagger)", "Abrir ↗", "abrir-externo", 'data-url="/swagger-ui.html"')}
      </ul>
      <p class="subtitulo-secao">Aparelho</p>
      <ul class="lista-ios">
        ${linha("#ef4444", "recarregar", "Reiniciar aparelho", "", "reiniciar")}
      </ul>
      <p class="subtitulo-secao">Reiniciar apaga as músicas, ligações e abas desta sessão e volta ao aparelho de exemplo.</p>
    </div>
  </section>`;
}

export const acoesExtras = {
  "zoom-mais": () => { tela.zoomUml = Math.min(1600, tela.zoomUml + 150); },
  "zoom-menos": () => { tela.zoomUml = Math.max(340, tela.zoomUml - 150); },
  "zoom-ajustar": () => { tela.zoomUml = 350; },
  "abrir-externo": (el) => { window.open(el.dataset.url, "_blank", "noopener"); },
};
