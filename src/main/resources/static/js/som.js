// Som do aparelho, todo sintetizado com Web Audio: as músicas (a partir da partitura que vem da API),
// os tons do teclado (DTMF), o tom de chamando (425 Hz, padrão brasileiro) e o toque de chamada.

let ctx = null;
let mestre = null;
let mudo = false;
try { mudo = localStorage.getItem("tres-em-um:mudo") === "1"; } catch { /* sem armazenamento */ }

export function ativar() {
  if (!ctx) {
    const Contexto = window.AudioContext || window.webkitAudioContext;
    if (!Contexto) return;
    ctx = new Contexto();
    mestre = ctx.createGain();
    mestre.gain.value = mudo ? 0 : 0.9;
    mestre.connect(ctx.destination);
  }
  if (ctx.state === "suspended") ctx.resume();
}

export function estaMudo() { return mudo; }

export function alternarMudo() {
  mudo = !mudo;
  try { localStorage.setItem("tres-em-um:mudo", mudo ? "1" : "0"); } catch { /* ignora */ }
  if (mestre) mestre.gain.setTargetAtTime(mudo ? 0 : 0.9, ctx.currentTime, 0.05);
  if (mudo) window.speechSynthesis?.cancel();
  return mudo;
}

// ----- Música -----

const NOTAS = { C: -9, "C#": -8, D: -7, "D#": -6, E: -5, F: -4, "F#": -3, G: -2, "G#": -1, A: 0, "A#": 1, B: 2 };

function frequencia(nota) {
  const m = /^([A-G]#?)(\d)$/.exec(nota);
  const semitons = NOTAS[m[1]] + (Number(m[2]) - 4) * 12;
  return 440 * Math.pow(2, semitons / 12);
}

/** "E4:1 R:0.5" -> [{ inicio, duracao, freq }] em segundos. */
export function eventos(partitura, bpm, oitava = 0) {
  const batida = 60 / bpm;
  let t = 0;
  const lista = [];
  for (const token of partitura.trim().split(/\s+/)) {
    const [nota, batidas] = token.split(":");
    const duracao = Number(batidas) * batida;
    if (nota !== "R") lista.push({ inicio: t, duracao, freq: frequencia(nota) * Math.pow(2, oitava) });
    t += duracao;
  }
  return { lista, total: t };
}

const musica = { id: null, saida: null, inicioCtx: 0, deslocamento: 0, analisador: null };

function voz(saida, freq, inicio, duracao, volume, forma = "triangle") {
  const osc = ctx.createOscillator();
  const brilho = ctx.createOscillator();
  const env = ctx.createGain();
  osc.type = forma;
  brilho.type = "sine";
  osc.frequency.value = freq;
  brilho.frequency.value = freq * 2;
  const g2 = ctx.createGain();
  g2.gain.value = 0.18;
  brilho.connect(g2).connect(env);
  osc.connect(env).connect(saida);
  const fim = inicio + Math.max(0.08, duracao * 0.95);
  env.gain.setValueAtTime(0, inicio);
  env.gain.linearRampToValueAtTime(volume, inicio + 0.012);
  env.gain.exponentialRampToValueAtTime(volume * 0.55, inicio + 0.18);
  env.gain.setValueAtTime(volume * 0.55, fim - 0.05);
  env.gain.linearRampToValueAtTime(0.0001, fim + 0.06);
  osc.start(inicio);
  brilho.start(inicio);
  osc.stop(fim + 0.1);
  brilho.stop(fim + 0.1);
}

/** Toca a faixa a partir de uma posição (segundos). Chamado quando a API diz que está tocando. */
export function tocarFaixa(faixa, posicao) {
  if (!ctx) return;
  pararFaixa();
  const saida = ctx.createGain();
  saida.gain.value = 0.32;
  // Um eco leve dá corpo ao timbre simples.
  const eco = ctx.createDelay();
  eco.delayTime.value = 0.26;
  const retorno = ctx.createGain();
  retorno.gain.value = 0.22;
  saida.connect(eco).connect(retorno).connect(eco);
  retorno.connect(mestre);
  const analisador = ctx.createAnalyser();
  analisador.fftSize = 64;
  saida.connect(analisador);
  saida.connect(mestre);

  const agora = ctx.currentTime + 0.05;
  // Melodias escritas em regiões graves sobem uma oitava: soam mais claras no sintetizador.
  const oitavas = (faixa.partitura.match(/\d(?=:)/g) || []).map(Number);
  const melodia = eventos(faixa.partitura, faixa.bpm, Math.max(...oitavas) <= 4 ? 1 : 0);
  for (const e of melodia.lista) {
    if (e.inicio + e.duracao <= posicao) continue;
    const inicio = agora + Math.max(0, e.inicio - posicao);
    const duracao = e.inicio < posicao ? e.duracao - (posicao - e.inicio) : e.duracao;
    voz(saida, e.freq, inicio, duracao, 0.22);
  }
  if (faixa.baixo) {
    const baixo = eventos(faixa.baixo, faixa.bpm, 0);
    for (let volta = 0; volta * baixo.total < melodia.total; volta++) {
      for (const e of baixo.lista) {
        const absoluto = volta * baixo.total + e.inicio;
        if (absoluto >= melodia.total || absoluto + e.duracao <= posicao) continue;
        const inicio = agora + Math.max(0, absoluto - posicao);
        const duracao = Math.min(e.duracao, melodia.total - absoluto) - Math.max(0, posicao - absoluto);
        voz(saida, e.freq, inicio, duracao, 0.12, "sine");
      }
    }
  }
  Object.assign(musica, { id: faixa.id, saida, inicioCtx: agora, deslocamento: posicao, analisador });
}

export function pararFaixa() {
  if (!musica.saida) return;
  const saida = musica.saida;
  saida.gain.setTargetAtTime(0, ctx.currentTime, 0.03);
  setTimeout(() => saida.disconnect(), 300);
  Object.assign(musica, { id: null, saida: null, analisador: null });
}

/** Faixa e posição que estão soando agora (para comparar com o que a API diz). */
export function posicaoAtual() {
  if (!ctx || !musica.id) return null;
  return { id: musica.id, posicao: musica.deslocamento + (ctx.currentTime - musica.inicioCtx) };
}

/** Níveis de 0 a 1 para o visualizador da capa. */
export function niveis(quantidade) {
  if (!musica.analisador) return null;
  const dados = new Uint8Array(musica.analisador.frequencyBinCount);
  musica.analisador.getByteFrequencyData(dados);
  return Array.from({ length: quantidade }, (_, i) => dados[Math.min(dados.length - 1, i + 1)] / 255);
}

// ----- Telefone -----

const DTMF = {
  1: [697, 1209], 2: [697, 1336], 3: [697, 1477],
  4: [770, 1209], 5: [770, 1336], 6: [770, 1477],
  7: [852, 1209], 8: [852, 1336], 9: [852, 1477],
  "*": [941, 1209], 0: [941, 1336], "#": [941, 1477],
};

function bipe(freqs, duracao, volume = 0.12, inicio = ctx.currentTime) {
  const env = ctx.createGain();
  env.gain.setValueAtTime(0, inicio);
  env.gain.linearRampToValueAtTime(volume, inicio + 0.01);
  env.gain.setValueAtTime(volume, inicio + duracao - 0.02);
  env.gain.linearRampToValueAtTime(0, inicio + duracao);
  env.connect(mestre);
  for (const f of freqs) {
    const osc = ctx.createOscillator();
    osc.frequency.value = f;
    osc.connect(env);
    osc.start(inicio);
    osc.stop(inicio + duracao + 0.02);
  }
}

/** Tom de tecla: duas frequências, como nos telefones de verdade. */
export function tom(tecla) {
  if (!ctx || !DTMF[tecla]) return;
  bipe(DTMF[tecla], 0.14, 0.1);
}

let chamando = null;
let tocando = null;

/** Tom de "chamando" do Brasil: 425 Hz, 1 s ligado, 4 s desligado. */
export function iniciarTomDeChamando() {
  if (!ctx || chamando) return;
  const ciclo = () => bipe([425], 1, 0.06);
  ciclo();
  chamando = setInterval(ciclo, 5000);
}

/** Toque de chamada recebida: um arpejo de marimba, criado para o projeto. */
export function iniciarToque() {
  if (!ctx || tocando) return;
  const notas = [76, 83, 88, 83, 81, 88, 91, 88];
  const ciclo = () => {
    const t0 = ctx.currentTime + 0.02;
    notas.forEach((midi, i) => {
      const freq = 440 * Math.pow(2, (midi - 69) / 12);
      const env = ctx.createGain();
      const osc = ctx.createOscillator();
      osc.type = "sine";
      osc.frequency.value = freq;
      const t = t0 + i * 0.13;
      env.gain.setValueAtTime(0, t);
      env.gain.linearRampToValueAtTime(0.16, t + 0.005);
      env.gain.exponentialRampToValueAtTime(0.001, t + 0.35);
      osc.connect(env).connect(mestre);
      osc.start(t);
      osc.stop(t + 0.4);
    });
  };
  ciclo();
  tocando = setInterval(ciclo, 2200);
}

export function pararTonsDeChamada() {
  clearInterval(chamando);
  clearInterval(tocando);
  chamando = null;
  tocando = null;
}

// ----- Correio de voz: o recado é "falado" pela síntese de voz do navegador -----

export function falar(texto, aoTerminar) {
  const voz = window.speechSynthesis;
  if (!voz || mudo) { aoTerminar?.(); return false; }
  voz.cancel();
  const fala = new SpeechSynthesisUtterance(texto);
  fala.lang = "pt-BR";
  fala.rate = 1.05;
  const brasileira = voz.getVoices().find((v) => v.lang === "pt-BR");
  if (brasileira) fala.voice = brasileira;
  fala.onend = () => aoTerminar?.();
  voz.speak(fala);
  return true;
}

export function calar() {
  window.speechSynthesis?.cancel();
}
