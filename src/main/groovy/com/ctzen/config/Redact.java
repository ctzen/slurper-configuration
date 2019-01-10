package com.ctzen.config;

/**
 * Hide value when dumping config settings in log,
 * use for passwords, for example.
 *
 * @author cchang
 */
public class Redact<E> {

    public Redact(E value) {
        this.value = value;
    }

    private final E value;

    public E getValue() {
        return value;
    }

}
