package com.shaubert.dirty.client;

import com.shaubert.blogadapter.client.Parser;
import com.shaubert.blogadapter.client.ParserFactory;

public class DirtyParserFactory implements ParserFactory {

    @Override
    public Parser createPostParser() {
        return new DirtyPostParser();
    }

    @Override
    public Parser createCommentsParser() {
        return new DirtyCommentParser();
    }

}
