import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // PROXY de desenvolvimento: tudo que o front pedir em "/partidas" é
    // encaminhado para o backend Spring (porta 8080). Assim o navegador "pensa"
    // que fala com o mesmo servidor (localhost:5173) e NÃO há problema de CORS.
    // Em produção isso se resolve de outra forma (mesmo domínio ou CORS no back).
    proxy: {
      '/partidas': 'http://localhost:8080',
    },
  },
})
