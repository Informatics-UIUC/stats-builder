package edu.illinois.i3.emop.apps.statsbuilder.txt;

import edu.illinois.i3.emop.apps.statsbuilder.OCRToken;

public class TxtToken implements OCRToken {

    private final String _token;

    public TxtToken(String token) {
        _token = token;
    }

    @Override
    public String getText() {
        return _token;
    }

    @Override
    public boolean isLastTokenOnLine() {
        return false;
    }
}
