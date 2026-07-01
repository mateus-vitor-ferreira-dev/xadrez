// Client ID do Google OAuth. NÃO é segredo (vai no HTML/JS do navegador) — por
// isso pode ficar aqui como padrão. Em produção dá para sobrescrever com a env
// VITE_GOOGLE_CLIENT_ID no build da Vercel.
export const GOOGLE_CLIENT_ID =
  import.meta.env.VITE_GOOGLE_CLIENT_ID ??
  '9171790587-hkqqp9fp5ghhqjtgjli4dpfnl38h3pkr.apps.googleusercontent.com'
