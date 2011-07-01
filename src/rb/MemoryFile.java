package rb;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: Gong Zeng
 * Date: 5/16/11
 * Time: 12:12 PM
 */
public class MemoryFile {
  String name;
  String content;

  public MemoryFile(String name, String diff) {
    this.name = name;
    this.content = diff;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public InputStream getInputStream() {
    return new ByteArrayInputStream(content.getBytes());
  }
}
