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
package ch.qos.logback.core.joran.util;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.core.joran.spi.DefaultClass;
import ch.qos.logback.core.spi.FilterReply;
import ch.qos.logback.core.util.Duration;
import ch.qos.logback.core.util.FileSize;

public class House {
  Door mainDoor;
  int count;
  Double temperature;
  boolean open;
  String name;
  String camelCase;
  SwimmingPool pool;
  Duration duration;
  FileSize fs;
  HouseColor houseColor;
  FilterReply reply;
  
  
  Charset charset;

  List<String> adjectiveList = new ArrayList<String>();
  List<Window> windowList = new ArrayList<Window>();
  List<SwimmingPool> largePoolList = new ArrayList<SwimmingPool>();

  public String getCamelCase() {
    return camelCase;
  }

  public void setCamelCase(String camelCase) {
    this.camelCase = camelCase;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int c) {
    this.count = c;
  }

  
  public Double getTemperature() {
    return temperature;
  }

  public void setTemperature(Double temperature) {
    this.temperature = temperature;
  }

  public Door getDoor() {
    return mainDoor;
  }

  public void setDoor(Door door) {
    this.mainDoor = door;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isOpen() {
    return open;
  }

  public void setOpen(boolean open) {
    this.open = open;
  }

  @DefaultClass(LargeSwimmingPoolImpl.class)
  public void addLargeSwimmingPool(SwimmingPool pool) {
    this.pool = pool;
  }

  @DefaultClass(SwimmingPoolImpl.class)
  public void setSwimmingPool(SwimmingPool pool) {
    this.pool = pool;
  }

  public SwimmingPool getSwimmingPool() {
    return pool;
  }

  public void addWindow(Window w) {
    windowList.add(w);
  }

  public void addAdjective(String s) {
    adjectiveList.add(s);
  }

  public Duration getDuration() {
    return duration;
  }

  public void setDuration(Duration duration) {
    this.duration = duration;
  }

  public FileSize getFs() {
    return fs;
  }

  public void setFs(FileSize fs) {
    this.fs = fs;
  }

  public void setHouseColor(HouseColor color) {
    this.houseColor = color;
  }

  public HouseColor getHouseColor() {
    return houseColor;
  }

  public void setFilterReply(FilterReply reply) {
    this.reply = reply;
  }

  public FilterReply getFilterReply() {
    return reply;
  }

  public Charset getCharset() {
    return charset;
  }

  public void setCharset(Charset charset) {
    this.charset = charset;
  }
}

class Door {
  int handle;
}

interface SwimmingPool {
}

class SwimmingPoolImpl implements SwimmingPool {
  int length;
  int width;
  int depth;
}

class LargeSwimmingPoolImpl implements SwimmingPool {
  int length;
  int width;
  int depth;
}

enum HouseColor {
  WHITE, BLUE
}