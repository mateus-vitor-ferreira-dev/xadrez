#!/usr/bin/env bash
# Para TUDO do ambiente local do xadrez: mata o que estiver ocupando as portas do
# backend (8080) e do frontend/Vite (5173).
#
# Por que existe: processos Spring/Vite às vezes ficam ÓRFÃOS entre execuções e
# continuam "segurando" a porta. Aí um novo backend não sobe (a porta parece
# ocupada) e você acaba testando contra CÓDIGO ANTIGO sem perceber.
#
# Uso:
#   ./scripts/parar.sh            # portas padrão: 8080 e 5173
#   ./scripts/parar.sh 8080 5174  # portas específicas
#
# Mata POR PORTA (não por nome de processo) de propósito: assim o próprio script
# nunca se mata por acidente ao "casar" o padrão na própria linha de comando —
# armadilha clássica do `pkill -f`.

set -u

portas=("$@")
if [ ${#portas[@]} -eq 0 ]; then
  portas=(8080 5173)
fi

# PIDs que estão ESCUTANDO na porta (via ss). Vazio = ninguém.
pids_da_porta() {
  ss -ltnpH "sport = :$1" 2>/dev/null | grep -oE 'pid=[0-9]+' | cut -d= -f2 | sort -u
}

matar_porta() {
  local porta="$1"
  local pids
  pids=$(pids_da_porta "$porta")
  if [ -z "$pids" ]; then
    echo "porta $porta: livre"
    return
  fi
  echo "porta $porta: encerrando PID(s): $(echo "$pids" | tr '\n' ' ')"
  # SIGTERM primeiro (encerramento limpo); SIGKILL só no que teimar em ficar vivo.
  kill $pids 2>/dev/null
  sleep 2
  for pid in $pids; do
    if kill -0 "$pid" 2>/dev/null; then
      echo "  PID $pid não saiu no TERM; forçando KILL"
      kill -9 "$pid" 2>/dev/null
    fi
  done
}

for porta in "${portas[@]}"; do
  matar_porta "$porta"
done

echo "pronto."
