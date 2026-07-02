// Sons sintetizados (Web Audio API) — não precisam de arquivos de áudio.
let ctx: AudioContext | null = null

function contexto(): AudioContext {
  if (!ctx) ctx = new AudioContext()
  if (ctx.state === 'suspended') void ctx.resume()
  return ctx
}

/** Toca um "toque" curto: grave para captura, mais agudo para movimento simples. */
export function tocarSom(tipo: 'mover' | 'capturar') {
  try {
    const c = contexto()
    const osc = c.createOscillator()
    const gain = c.createGain()
    osc.connect(gain)
    gain.connect(c.destination)
    osc.type = 'triangle'
    osc.frequency.value = tipo === 'capturar' ? 150 : 300
    const t = c.currentTime
    gain.gain.setValueAtTime(0.0001, t)
    gain.gain.exponentialRampToValueAtTime(tipo === 'capturar' ? 0.3 : 0.2, t + 0.008)
    gain.gain.exponentialRampToValueAtTime(0.0001, t + 0.15)
    osc.start(t)
    osc.stop(t + 0.16)
  } catch {
    // navegador sem Web Audio / bloqueio de autoplay — ignora silenciosamente
  }
}
