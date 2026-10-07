package br.com.cti.insights.model;

/**
 * Tipo de usuario. Todo cadastro nasce USER; ADMIN e definido direto no banco. Ainda nao ha rota
 * restrita por perfil: o campo vai no token para a regra poder ser ligada depois.
 */
public enum Perfil {
	USER, ADMIN
}
