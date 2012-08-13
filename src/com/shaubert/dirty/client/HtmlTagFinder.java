package com.shaubert.dirty.client;

import com.shaubert.dirty.client.HtmlTagFinder.AttributeWithValue.Constraint;

import org.xml.sax.Attributes;

import java.util.ArrayList;
import java.util.List;

public class HtmlTagFinder {

    public interface AttributeRule {
        boolean applies(Attributes attributes);
    }
    
    public static class AttributeWithValue implements AttributeRule {
        
        public enum Constraint {
            STARTS_WITH,
            ENDS_WITH,
            CONTAINS,
            EQUALS,
            NOT_EQUALS,
        }
        
        private String value;
        private String name;
        private Constraint constraint;
        
        public AttributeWithValue(String name, String value, Constraint constraint) {
            this.value = value;
            this.name = name;
            this.constraint = constraint;
        }

        @Override
        public boolean applies(Attributes attributes) {
            String val = attributes.getValue("", name);
            if (val != null) {
                switch (constraint) {
                    case STARTS_WITH: return val.startsWith(value);
                    case ENDS_WITH: return val.endsWith(value);
                    case CONTAINS: return val.contains(value);
                    case EQUALS: return val.equalsIgnoreCase(value);
                    case NOT_EQUALS: return !val.equalsIgnoreCase(value);
                }
            }
            return false;
        }
    }
    
    public static class Rule {
        private String tagName;
        private List<AttributeRule> subRules;
        
        public Rule(String tagName) {
            this.tagName = tagName;
            this.subRules = new ArrayList<HtmlTagFinder.AttributeRule>();
        }
        
        public Rule withAttribute(String attribute) {
            subRules.add(new AttributeWithValue(attribute, null, Constraint.NOT_EQUALS));
            return this;
        }
        
        public Rule withAttributeWithValue(String attribute, String value) {
            subRules.add(new AttributeWithValue(attribute, value, Constraint.EQUALS));
            return this;
        }
        
        public Rule withAttributeWithValue(String attribute, String value, Constraint constraint) {
            subRules.add(new AttributeWithValue(attribute, value, constraint));
            return this;
        }
        
        public boolean applies(String tag, Attributes attributes) {
            boolean result = tag.equalsIgnoreCase(tagName);
            if (result) {
                for (AttributeRule subRule : subRules) {
                    if (!subRule.applies(attributes)) {
                        result = false;
                        break;
                    }
                }
            }
            return result;
        }
    }
    
    public static class TagNode {

        private String name;
        private Attributes attributes;
        private String text;
        
        private TagNode parent;
        private List<TagNode> childs = new ArrayList<HtmlTagFinder.TagNode>();
        private List<TagNode> notContentChilds = new ArrayList<HtmlTagFinder.TagNode>();
        
        public TagNode(String name, Attributes attributes) {
            this.name = name;
            this.attributes = attributes;
        }
        
        public TagNode(String text) {
            this.text = text;
        }
        
        public String getName() {
            return name;
        }
        
        public Attributes getAttributes() {
            return attributes;
        }
        
        public String getText() {
            return text;
        }
        
        public boolean isContentNode() {
            return this.name == null;
        }
        
        public TagNode getParent() {
            return parent;
        }
        
        public void addChild(TagNode node) {
            node.parent = this;
            childs.add(node);
            if (!node.isContentNode()) {
                notContentChilds.add(node);
            }
        }
        
        public List<TagNode> getChilds() {
            return childs;
        }
        
        public List<TagNode> getNotContentChilds() {
            return notContentChilds;
        }
        
        public List<TagNode> findAll(Rule rule) {
            List<TagNode> result = new ArrayList<HtmlTagFinder.TagNode>();
            for (TagNode child : notContentChilds) {
                if (rule.applies(child.name, child.attributes)) {
                    result.add(child);
                    result.addAll(child.findAll(rule));
                }
            }
            return result;
        }
        
        public List<TagNode> findPath(String ... pathSegments) {
            return findPath(0, pathSegments);
        }
        
        private List<TagNode> findPath(int level, String ... pathSegments) {
            List<TagNode> result = new ArrayList<HtmlTagFinder.TagNode>();
            for (TagNode child : notContentChilds) {
                if (child.name.equalsIgnoreCase(pathSegments[level])) {
                    if (level == pathSegments.length - 1) {
                        result.add(child);
                    } else {
                        result.addAll(child.findPath(level + 1, pathSegments));
                    }
                }
            }
            return result;
        }
    }
    
    public interface Callback {
        void onTagFounded(HtmlTagFinder finder, TagNode foundTag);
    }
    
    private boolean active;
    private TagNode rootTag;
    private TagNode activeTag;
    private final Rule rule;
    private final Callback callback;
    private int tagCounter;

    public HtmlTagFinder(Rule rule, Callback callback) {
        this.rule = rule;
        this.callback = callback;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public boolean handleStartTag(String tag, Attributes attributes) {
        if (active) {
            tagCounter++;
            TagNode node = new TagNode(tag, attributes);
            activeTag.addChild(node);
            activeTag = node;
        } else {
            active = rule.applies(tag, attributes);
            if (active) {
                tagCounter = 1;
                this.rootTag = new TagNode(tag, attributes);
                this.activeTag = rootTag;
            }
        }
        return active;
    }

    public void handleText(String text) {
        if (active) {
            List<TagNode> childs = activeTag.getChilds();
            TagNode child = childs.isEmpty() ? null : childs.get(childs.size() - 1);
            if (child != null && child.isContentNode()) {
                child.text += text;
            } else {
                activeTag.addChild(new TagNode(text));
            }
        }
    }
    
    public boolean handleEndTag(String tag) {
        if (active) {
            tagCounter--;
            active = tagCounter > 0;
            if (!active) {
                this.callback.onTagFounded(this, rootTag);
            } else {
                activeTag = activeTag.getParent();
            }
        }
        return active;
    }

    public TagNode getTag() {
        return rootTag;
    }
    
    public Rule getRule() {
        return rule;
    }
    
}
