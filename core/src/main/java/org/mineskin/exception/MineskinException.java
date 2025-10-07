package org.mineskin.exception;

import javax.annotation.Nullable;

public class MineskinException extends RuntimeException implements IBreadcrumbException {

    private String breadcrumb;

    public MineskinException() {
    }

    public MineskinException(String message) {
        super(message);
    }

    public MineskinException(String message, Throwable cause) {
        super(message, cause);
    }

    public MineskinException(Throwable cause) {
        super(cause);
    }

    public MineskinException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public MineskinException withBreadcrumb(String breadcrumb) {
        this.breadcrumb = breadcrumb;
        return this;
    }

    @Nullable
    @Override
    public String getBreadcrumb() {
        return breadcrumb;
    }

}
