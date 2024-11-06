package com.beans;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ServerOutput implements java.io.Serializable {
    String matchedCount;
    String matchedLines;
}