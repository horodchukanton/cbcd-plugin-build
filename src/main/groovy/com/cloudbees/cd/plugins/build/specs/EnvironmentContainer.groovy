package com.cloudbees.cd.plugins.build.specs

class EnvironmentContainer {

    static Map<String, String> container = new HashMap<>()

    static void addVar(String name, String value) {
        container.put(name, value)
    }

    static void replace(String name, String value) {
        container.replace(name, value)
    }

    static void addVars(Map<String, String> vars) {
        container.putAll(vars)
    }

    static Map<String, String> getAll() {
        return container
    }

    static String maskValue(String value) {
        return (value.size() <= 20)
                ? ('*' * value.size())
                : "[ masked ${value.size()} chars ]"
    }

}
