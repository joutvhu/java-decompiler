package com.joutvhu.javadec.engine.util;

import com.joutvhu.javadec.engine.util.collections.SetFactory;
import com.joutvhu.javadec.engine.util.output.Dumpable;
import com.joutvhu.javadec.engine.util.output.Dumper;

import java.util.Collection;
import java.util.Set;

public class DecompilerComments implements Dumpable {
    private Set<DecompilerComment> comments = SetFactory.newOrderedSet();

    public DecompilerComments() {
    }

    public void addComment(String comment) {
        DecompilerComment decompilerComment = new DecompilerComment(comment);
        comments.add(decompilerComment);
    }

    public void addComment(DecompilerComment comment) {
        comments.add(comment);
    }

    public void addComments(Collection<DecompilerComment> comments) {
        this.comments.addAll(comments);
    }

    @Override
    public Dumper dump(Dumper d) {
        if (comments.isEmpty()) return d;
        d.beginBlockComment(false);
        for (DecompilerComment comment : comments) {
            d.dump(comment);
        }
        d.endBlockComment();
        return d;
    }

    public boolean contains(DecompilerComment comment) {
        return comments.contains(comment);
    }

    public Collection<DecompilerComment> getCommentCollection() {
        return comments;
    }

}
