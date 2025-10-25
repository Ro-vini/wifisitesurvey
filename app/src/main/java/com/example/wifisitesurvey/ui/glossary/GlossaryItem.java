package com.example.wifisitesurvey.ui.glossary;

public class GlossaryItem {
    private final String term;
    private final String definition;

    public GlossaryItem(String term, String definition) {
        this.term = term;
        this.definition = definition;
    }

    public String getTerm() {
        return term;
    }

    public String getDefinition() {
        return definition;
    }
}
