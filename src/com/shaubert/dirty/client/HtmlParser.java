package com.shaubert.dirty.client;

import org.ccil.cowan.tagsoup.HTMLSchema;
import org.ccil.cowan.tagsoup.Parser;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HtmlParser implements ContentHandler {

    private XMLReader reader;
    
    private StringBuilder textBuilder; 
    private List<HtmlTagFinder> tagFinders = new ArrayList<HtmlTagFinder>();
    
    public void parse(InputStream stream) throws IOException {
        reader = new Parser();
        try {
            reader.setProperty(Parser.schemaProperty, new HTMLSchema());
        } catch (org.xml.sax.SAXNotRecognizedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        } catch (org.xml.sax.SAXNotSupportedException e) {
            // Should not happen.
            throw new RuntimeException(e);
        }
        
        reader.setContentHandler(this);
        try {
            reader.parse(new InputSource(stream));
        } catch (SAXException e) {
            // TagSoup doesn't throw parse exceptions.
            throw new RuntimeException(e);
        }
    }
    
    public void addTagFinder(HtmlTagFinder finder) {
        tagFinders.add(finder);
    }
    
    protected void handleStartTag(String tag, Attributes attributes) {
        boolean hasActive = false;
        for (HtmlTagFinder finder : tagFinders) {
            hasActive |= finder.handleStartTag(tag, attributes);
        }
        if (hasActive && textBuilder == null) {
            textBuilder = new StringBuilder();
        }
    }

    protected  void handleEndTag(String tag) {
        boolean hasActive = false;
        for (HtmlTagFinder finder : tagFinders) {
            hasActive |= finder.handleEndTag(tag);
        }
        if (!hasActive) {
            textBuilder = null;
        }
    }
        
    @Override
    public void setDocumentLocator(Locator locator) {
    }

    @Override
    public void startDocument() throws SAXException {
    }

    @Override
    public void endDocument() throws SAXException {
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        handleStartTag(localName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        handleEndTag(localName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (textBuilder == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();

        /*
         * Ignore whitespace that immediately follows other whitespace;
         * newlines count as spaces.
         */

        for (int i = 0; i < length; i++) {
            char c = ch[i + start];

            if (c == ' ' || c == '\n' || c == '\t') {
                char pred;
                int len = sb.length();

                if (len == 0) {
                    len = textBuilder.length();

                    if (len == 0) {
                        pred = '\n';
                    } else {
                        pred = textBuilder.charAt(len - 1);
                    }
                } else {
                    pred = sb.charAt(len - 1);
                }

                if (pred != ' ' && pred != '\n' && pred != '\t') {
                    sb.append(' ');
                }
            } else {
                sb.append(c);
            }
        }

        if (sb.length() > 0) {
            String text = sb.toString();
            for (HtmlTagFinder tagFinder : tagFinders) {
                if (tagFinder.isActive()) {
                    tagFinder.handleText(text);
                }
            }
            
            textBuilder.append(text);
        }
    }
    
    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
    }
    
}