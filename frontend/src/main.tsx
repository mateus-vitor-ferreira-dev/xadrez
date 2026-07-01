import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import './index.css'
import App from './App.tsx'
import PaginaAuth from './PaginaAuth.tsx'
import PaginaApelido from './PaginaApelido.tsx'
import PaginaLobby from './PaginaLobby.tsx'
import { AuthProvider } from './auth.tsx'

// O QueryClient é o "cérebro" do TanStack Query: guarda o cache das respostas,
// controla refetch, etc. Criamos um e o disponibilizamos via Context (Provider)
// para que qualquer componente possa usar useQuery/useMutation.
const queryClient = new QueryClient()

// Aplica o tema salvo antes de renderizar — assim a tela de login (que pode ser
// aberta direto, sem passar pelo App) já nasce no tema certo.
document.documentElement.dataset.tema =
  localStorage.getItem('tema') === 'claro' ? 'claro' : 'escuro'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      {/* AuthProvider por fora do Router: a sessão é única e vale em todas as rotas. */}
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<App />} />
            <Route path="/login" element={<PaginaAuth modo="login" />} />
            <Route path="/registro" element={<PaginaAuth modo="registro" />} />
            <Route path="/apelido" element={<PaginaApelido />} />
            <Route path="/lobby" element={<PaginaLobby />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
)
