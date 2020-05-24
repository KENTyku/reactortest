package ru.kentyku.reactortest;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Cat {
    private String name;
    private Integer age;
    Cat(String name, Integer age){
        this.name=name;
        this.age=age;
    }
}
