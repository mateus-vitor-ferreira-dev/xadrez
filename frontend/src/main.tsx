import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import App from './App.tsx'

// O QueryClient é o "cérebro" do TanStack Query: guarda o cache das respostas,
// controla refetch, etc. Criamos um e o disponibilizamos via Context (Provider)
// para que qualquer componente possa usar useQuery/useMutation.
const queryClient = new QueryClient()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </StrictMode>,
)
