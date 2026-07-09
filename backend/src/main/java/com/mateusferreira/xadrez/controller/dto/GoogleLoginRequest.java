package com.mateusferreira.xadrez.controller.dto;

/** Corpo do login com Google: o "ID token" (credential) devolvido pelo GIS. */
public record GoogleLoginRequest(String credential) {
}
