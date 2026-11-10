package br.com.cti.insights.model;

/**
 * Tipo de usuario. Todo cadastro nasce USER. ADMIN aprova contas e administra usuarios (rotas
 * /api/admin/** e /api/usuarios/{id}); vem de ADMIN_EMAIL ou de outro ADMIN.
 */
public enum Perfil {
	USER, ADMIN
}
