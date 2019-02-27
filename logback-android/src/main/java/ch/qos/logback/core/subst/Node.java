/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.core.subst;

public class Node {

  enum Type {LITERAL, VARIABLE}

  Type type;
  Object payload;
  Object defaultPart;
  Node next;


  public Node(Type type, Object payload) {
    this.type = type;
    this.payload = payload;
  }


  public Node(Type type, Object payload, Object defaultPart) {
    this.type = type;
    this.payload = payload;
    this.defaultPart = defaultPart;
  }

  void append(Node newNode) {
    if(newNode == null)
      return;
    Node n = this;
    while(true) {
      if(n.next == null) {
        n.next = newNode;
        return;
      }
      n = n.next;
    }
  }

  @Override
  public String toString() {
    switch (type) {
      case LITERAL:
        return "Node{" +
                "type=" + type +
                ", payload='" + payload +
                "'}";
      case VARIABLE:
        StringBuilder payloadBuf = new StringBuilder();
        StringBuilder defaultPartBuf2 = new StringBuilder();
        if (defaultPart != null)
          recursive((Node) defaultPart, defaultPartBuf2);

        recursive((Node) payload, payloadBuf);
        String r = "Node{" +
                "type=" + type +
                ", payload='" + payloadBuf.toString()+"'";
        if (defaultPart != null)
          r += ", defaultPart=" + defaultPartBuf2.toString();
        r += '}';
        return r;
    }
    return null;
  }

  public void dump() {
    System.out.print(this.toString());
    System.out.print(" -> ");
    if(next != null) {
      next.dump();
    }  else {
      System.out.print(" null");
    }
  }

  void recursive(Node n, StringBuilder sb) {
    Node c = n;
    while (c != null) {
      sb.append(c.toString()).append(" --> ");
      c = c.next;
    }
    sb.append("null ");
  }

  public void setNext(Node n) {
    this.next = n;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Node node = (Node) o;

    if (type != node.type) return false;
    if (payload != null ? !payload.equals(node.payload) : node.payload != null) return false;
    if (defaultPart != null ? !defaultPart.equals(node.defaultPart) : node.defaultPart != null) return false;
    if (next != null ? !next.equals(node.next) : node.next != null) return false;


    return true;
  }

  @Override
  public int hashCode() {
    int result = type != null ? type.hashCode() : 0;
    result = 31 * result + (payload != null ? payload.hashCode() : 0);
    result = 31 * result + (defaultPart != null ? defaultPart.hashCode() : 0);
    result = 31 * result + (next != null ? next.hashCode() : 0);
    return result;
  }
}
