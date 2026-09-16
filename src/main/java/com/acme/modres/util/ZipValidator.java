package com.acme.modres.util;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ZipValidator extends ZipFile {

  private File file;

  public ZipValidator(File file) throws ZipException, IOException {
    super(file);
    this.file = file;
  }

  public boolean isValid() throws Throwable {
    if (file.exists()) {
      ZipValidator zipFile = null;
      try {
        zipFile = new ZipValidator(file);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        if (!entries.hasMoreElements()) {
          return true;
        }
      } finally {
        if (zipFile != null) {
          zipFile.close();
        }
      }
    }
    return false;
  }

}