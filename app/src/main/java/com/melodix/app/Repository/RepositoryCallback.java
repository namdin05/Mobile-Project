package com.melodix.app.Repository;

public interface RepositoryCallback<T> {
    void onSuccess(T data);
    void onError(String message);
}