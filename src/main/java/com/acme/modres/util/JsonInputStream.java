package com.acme.modres.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.Gson;

public class JsonInputStream extends InputStream {
  private final InputStream delegate;

  public JsonInputStream(InputStream delegate) {
    this.delegate = delegate;
  }

  public Object parseJsonAs(Class<?> cls) {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(delegate))) {
      Gson gson = new Gson();
      return gson.fromJson(reader, cls);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to parse JSON stream", e);
    }
  }

  @Override
  public int read() throws IOException {
    return delegate.read();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }
}
