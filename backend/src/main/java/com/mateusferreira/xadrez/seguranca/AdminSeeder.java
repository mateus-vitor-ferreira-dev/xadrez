package com.mateusferreira.xadrez.seguranca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Promove a {@link Role#ADMIN} as contas listadas em {@code app.admin-usuarios}
 * (apelidos separados por vírgula) na subida da aplicação. É o "seed" da conta
 * administradora: a pessoa se cadastra normalmente e, ao definir
 * {@code APP_ADMIN_USUARIOS=seuApelido} e reiniciar, a conta ganha acesso total
 * (todas as skins liberadas). Só promove — nunca rebaixa — e ignora contas ainda
 * inexistentes (elas são promovidas na próxima subida, depois de criadas).
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UsuarioRepository repository;
    private final String adminUsuarios;

    public AdminSeeder(UsuarioRepository repository,
                       @Value("${app.admin-usuarios:}") String adminUsuarios) {
        this.repository = repository;
        this.adminUsuarios = adminUsuarios;
    }

    @Override
    public void run(String... args) {
        List<String> apelidos = Arrays.stream(adminUsuarios.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (apelidos.isEmpty()) {
            return;
        }
        int promovidos = 0;
        for (String apelido : apelidos) {
            var opt = repository.findByUsuario(apelido);
            if (opt.isEmpty()) {
                log.warn("app.admin-usuarios: conta '{}' não existe ainda — será promovida na próxima subida.", apelido);
                continue;
            }
            Usuario u = opt.get();
            if (u.getRole() != Role.ADMIN) {
                u.setRole(Role.ADMIN);
                repository.save(u);
                promovidos++;
            }
        }
        if (promovidos > 0) {
            log.info("AdminSeeder: {} conta(s) promovida(s) a ADMIN.", promovidos);
        }
    }
}
