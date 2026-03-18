package com.dimenna.truck.ui;

public interface Refreshable {
    /** Se llama cada vez que la vista vuelve a mostrarse. */
    default void onShow() {}

    /** Se llama cuando cambió el catálogo (ABM o reorden). */
    default void onCatalogChanged() {}
}
