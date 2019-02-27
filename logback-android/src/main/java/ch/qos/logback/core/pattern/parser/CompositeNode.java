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
package ch.qos.logback.core.pattern.parser;

public class CompositeNode extends SimpleKeywordNode {
  Node childNode;

  CompositeNode(String keyword) {
    super(Node.COMPOSITE_KEYWORD, keyword);

  }

  public Node getChildNode() {
    return childNode;
  }

  public void setChildNode(Node childNode) {
    this.childNode = childNode;
  }

  public boolean equals(Object o) {
    if(!super.equals(o)) {
      return false;
    }
    if (!(o instanceof CompositeNode)) {
      return false;
    }
    CompositeNode r = (CompositeNode) o;

    return (childNode != null) ? childNode.equals(r.childNode)
            : (r.childNode == null);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    if(childNode != null) {
     buf.append("CompositeNode("+childNode+")");
    } else {
      buf.append("CompositeNode(no child)");
    }
    buf.append(printNext());
    return buf.toString();
  }
}