# AprovIA

App Android nativo (Kotlin) focado em chat com IA.

## O que o app faz
Basicamente é um chat onde você conversa com uma IA. O projeto usa Firebase para quase tudo no backend.

### Funcionalidades
- **Login/Cadastro**: Autenticação completa (Firebase Auth). Tem validação de campos e a opção "Esqueci minha senha" que checa se o e-mail existe antes de enviar o reset.
- **Chat**: Troca de mensagens em tempo real salva no Realtime Database.
- **Histórico**: As conversas ficam salvas e podem ser acessadas pelo menu lateral (Navigation Drawer).
- **Perfil**: Dá para escolher um avatar predefinido e ver as infos da conta no menu.
- **UI**: Interface limpa usando Material Design, com modo noturno/escuro onde aplicável (como na tela de login).

## Como rodar
1. Clone o repositório.
2. Abra no Android Studio.
3. **Importante**: Você precisa colocar o seu arquivo `google-services.json` na pasta `app/`, senão o Firebase não vai conectar e o app vai quebrar no login.

## Stack
- Kotlin
- Firebase (Auth e Realtime Database)
- Glide (para carregar as imagens de perfil)
- Android Views (XML)
