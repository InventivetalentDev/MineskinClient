package org.mineskin;

public class MineSkinResponse<T> {

    public final int status;
    public final String message;
    public final String server;
    public final String breadcrumb;
    public final T body;

}
