package org.mineskin.response;

import org.mineskin.data.CodeAndMessage;

import java.util.List;
import java.util.Optional;

public interface MineSkinResponse<T> {
    boolean isSuccess();

    int getStatus();

    List<CodeAndMessage> getMessages();

    Optional<CodeAndMessage> getFirstMessage();

    List<CodeAndMessage> getErrors();

    boolean hasErrors();

    Optional<CodeAndMessage> getFirstError();

    Optional<CodeAndMessage> getErrorOrMessage();

    List<CodeAndMessage> getWarnings();

    Optional<CodeAndMessage> getFirstWarning();

    String getServer();

    String getBreadcrumb();

    T getBody();


}
