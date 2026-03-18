package com.dimenna.truck.ui;

public interface Refreshable {
    
    default void onShow() {}

    
    default void onCatalogChanged() {}
}
