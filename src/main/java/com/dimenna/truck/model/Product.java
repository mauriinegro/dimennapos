package com.dimenna.truck.model;

/** Modelo inmutable para un producto del catálogo. */
public record Product(long id, String nombre, double precio, int orden) {}
