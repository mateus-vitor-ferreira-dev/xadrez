# Deploy — Xadrez (backend na Railway, frontend na Vercel)

Arquitetura no ar:

```
Navegador → Vercel (frontend React estático) → Railway (Spring Boot + PostgreSQL)
```

O que já está preparado no código:
- **Porta dinâmica:** `server.port=${PORT:8080}` (a Railway injeta `PORT`).
- **Perfil de produção:** `application-prod.properties` (PostgreSQL via variáveis de ambiente).
- **Driver PostgreSQL** no `pom.xml` (escopo `runtime`).
- **CORS** configurável (`CorsConfig` + variável `APP_CORS_ALLOWED_ORIGINS`).
- **Dockerfile** multi-stage (build com Maven, runtime com JRE 21).
- **Frontend** usa `VITE_API_URL` para achar o backend em produção.

---

## Passo 0 — Subir o código para o GitHub

```bash
cd ~/Projetos/xadrez
git init
git checkout -b develop          # padrão do fluxo (main = release; develop = trabalho)
git add .
git commit -m "feat: jogo de xadrez full-stack (backend Spring + frontend React)"
# crie um repo vazio no GitHub e:
git remote add origin git@github.com:<seu-usuario>/xadrez.git
git push -u origin develop
git checkout -b main && git push -u origin main
```

## Passo 1 — Backend na Railway

1. Em railway.app: **New Project → Deploy from GitHub repo** → escolha o repo `xadrez`.
   - Em **Settings → Root Directory**, defina `backend` (o **Dockerfile** e o
     `pom.xml` moram nessa pasta). A Railway então detecta o Dockerfile e constrói o backend.
2. Adicione o banco: **New → Database → Add PostgreSQL** (no mesmo projeto).
3. No serviço do backend, aba **Variables**, adicione:
   ```
   SPRING_PROFILES_ACTIVE = prod
   SPRING_DATASOURCE_URL = jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
   SPRING_DATASOURCE_USERNAME = ${{Postgres.PGUSER}}
   SPRING_DATASOURCE_PASSWORD = ${{Postgres.PGPASSWORD}}
   ```
   (A sintaxe `${{Postgres.XXX}}` referencia as variáveis do banco PostgreSQL criado no passo 2.)
4. Aba **Settings → Networking → Generate Domain**. Anote a URL (ex.: `https://xadrez-production.up.railway.app`).
5. Teste: `curl -X POST https://SUA-URL-RAILWAY/partidas` deve retornar um JSON com `id`.

## Passo 2 — Frontend na Vercel

1. Em vercel.com: **Add New → Project** → importe o mesmo repo `xadrez`.
2. **Root Directory:** selecione `frontend` (o app React está numa subpasta).
   - Framework: a Vercel detecta **Vite** sozinha (build `npm run build`, saída `dist`).
3. **Environment Variables**, adicione:
   ```
   VITE_API_URL = https://SUA-URL-RAILWAY     (sem barra no final)
   ```
4. **Deploy.** Anote a URL final (ex.: `https://xadrez.vercel.app`).

## Passo 3 — Fechar o cadeado do CORS

Agora que você tem a URL da Vercel, volte à **Railway → Variables** do backend e adicione:
```
APP_CORS_ALLOWED_ORIGINS = https://xadrez.vercel.app
```
> Dica: para aceitar também os *previews* da Vercel, use `https://*.vercel.app`
> (o `CorsConfig` usa `allowedOriginPatterns`, que aceita curinga).

A Railway reinicia o backend sozinho. Pronto: abra a URL da Vercel e jogue. 🎉

---

## Notas

- **Ordem importa:** backend primeiro (para ter a URL da Railway → vai no `VITE_API_URL`);
  depois frontend (para ter a URL da Vercel → vai no `APP_CORS_ALLOWED_ORIGINS`).
- **Persistência:** com PostgreSQL, as partidas sobrevivem a reinícios/redeploys.
- **Por que foi fácil trocar H2 → PostgreSQL?** O domínio nunca dependeu do banco; só
  mudaram dependência + configuração. Esse é o valor da separação em camadas.
- **Local continua igual:** sem as variáveis de ambiente, roda no perfil padrão (H2) e
  no proxy do Vite — nada muda no seu fluxo de desenvolvimento.
