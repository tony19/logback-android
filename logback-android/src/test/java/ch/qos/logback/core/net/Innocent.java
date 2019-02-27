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
package ch.qos.logback.core.net;

public class Innocent implements java.io.Serializable {

    private static final long serialVersionUID = -1227008349289885025L;

    int anInt;
    Integer anInteger;
    String aString;

    public int getAnInt() {
        return anInt;
    }

    public void setAnInt(int anInt) {
        this.anInt = anInt;
    }

    public Integer getAnInteger() {
        return anInteger;
    }

    public void setAnInteger(Integer anInteger) {
        this.anInteger = anInteger;
    }

    public String getaString() {
        return aString;
    }

    public void setaString(String aString) {
        this.aString = aString;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((aString == null) ? 0 : aString.hashCode());
        result = prime * result + anInt;
        result = prime * result + ((anInteger == null) ? 0 : anInteger.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Innocent other = (Innocent) obj;
        if (aString == null) {
            if (other.aString != null)
                return false;
        } else if (!aString.equals(other.aString))
            return false;
        if (anInt != other.anInt)
            return false;
        if (anInteger == null) {
            if (other.anInteger != null)
                return false;
        } else if (!anInteger.equals(other.anInteger))
            return false;
        return true;
    }

}
