package com.github.readutf.test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor @Getter @Setter
public class TestObject {

    String string;
    List<String> list;
    int integer;

    public TestObject() {
        this.string = "TestString";
        this.list = Arrays.asList("test", "test1", "test12");
        this.integer = 23;
    }
}
