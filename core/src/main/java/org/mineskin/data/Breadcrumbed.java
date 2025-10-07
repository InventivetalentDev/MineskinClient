package org.mineskin.data;

import javax.annotation.Nullable;

public interface Breadcrumbed {
    @Nullable
    String getBreadcrumb();
}
