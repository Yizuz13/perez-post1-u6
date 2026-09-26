package com.tienda.pedidos.validacion;

public abstract class ValidadorPedido {
    private ValidadorPedido siguiente;

    public ValidadorPedido enlazar(ValidadorPedido siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    public final void validar(ContextoPedido contexto) {
        validarRegla(contexto);
        if (siguiente != null) {
            siguiente.validar(contexto);
        }
    }

    protected abstract void validarRegla(ContextoPedido contexto);
}